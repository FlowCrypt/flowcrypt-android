/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.net.Uri;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.gmail.GmailApiHelper;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.api.email.protocol.SmtpProtocolUtil;
import com.flowcrypt.email.api.retrofit.ApiHelper;
import com.flowcrypt.email.api.retrofit.ApiService;
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor;
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel;
import com.flowcrypt.email.api.retrofit.request.model.TestWelcomeModel;
import com.flowcrypt.email.api.retrofit.response.attester.InitialLegacySubmitResponse;
import com.flowcrypt.email.api.retrofit.response.attester.TestWelcomeResponse;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.api.retrofit.response.node.GenerateKeyResult;
import com.flowcrypt.email.database.dao.KeysDao;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.ActionQueueDaoSource;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.PgpContact;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.flowcrypt.email.service.actionqueue.actions.BackupPrivateKeyToInboxAction;
import com.flowcrypt.email.service.actionqueue.actions.RegisterUserPublicKeyAction;
import com.flowcrypt.email.service.actionqueue.actions.SendWelcomeTestEmailAction;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListSendAsResponse;
import com.google.api.services.gmail.model.SendAs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;

import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;
import retrofit2.Response;

/**
 * This loader does job of creating a private key and returns the private key long id as result.
 *
 * @author DenBond7
 * Date: 12.01.2018.
 * Time: 12:36.
 * E-mail: DenBond7@gmail.com
 */
public class CreatePrivateKeyAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {
  private final String passphrase;
  private final AccountDao account;
  private boolean isActionStarted;
  private LoaderResult data;

  public CreatePrivateKeyAsyncTaskLoader(Context context, AccountDao account, String passphrase) {
    super(context);
    this.account = account;
    this.passphrase = passphrase;
  }

  @Override
  public void onStartLoading() {
    if (data != null) {
      deliverResult(data);
    } else {
      if (!isActionStarted) {
        forceLoad();
      }
    }
  }

  @Override
  public LoaderResult loadInBackground() {
    String email = account.getEmail();
    isActionStarted = true;
    NodeKeyDetails nodeKeyDetails = null;
    try {
      KeyStoreCryptoManager manager = new KeyStoreCryptoManager(getContext());

      GenerateKeyResult result = NodeCallsExecutor.genKey(passphrase, genContacts());
      nodeKeyDetails = result.getKey();

      KeysDao keysDao = KeysDao.generateKeysDao(manager, KeyDetails.Type.NEW, nodeKeyDetails, passphrase);

      Uri uri = new KeysDaoSource().addRow(getContext(), keysDao);

      if (uri == null) {
        return new LoaderResult(null, new NullPointerException("Cannot save the generated private key"));
      }

      new UserIdEmailsKeysDaoSource().addRow(getContext(), nodeKeyDetails.getLongId(),
          nodeKeyDetails.getPrimaryPgpContact().getEmail());

      ActionQueueDaoSource daoSource = new ActionQueueDaoSource();

      if (!saveCreatedPrivateKeyAsBackupToInbox(nodeKeyDetails)) {
        daoSource.addAction(getContext(), new BackupPrivateKeyToInboxAction(email, nodeKeyDetails.getLongId()));
      }

      if (!registerUserPublicKey(nodeKeyDetails)) {
        daoSource.addAction(getContext(), new RegisterUserPublicKeyAction(email, nodeKeyDetails.getPublicKey()));
      }

      if (!requestingTestMsgWithNewPublicKey(nodeKeyDetails)) {
        daoSource.addAction(getContext(), new SendWelcomeTestEmailAction(email, nodeKeyDetails.getPublicKey()));
      }

      return new LoaderResult(nodeKeyDetails.getLongId(), null);
    } catch (Exception e) {
      e.printStackTrace();
      if (nodeKeyDetails != null) {
        new KeysDaoSource().removeKey(getContext(), nodeKeyDetails.getLongId());
        new UserIdEmailsKeysDaoSource().removeKey(getContext(), nodeKeyDetails.getLongId());
      }
      ExceptionUtil.handleError(e);
      return new LoaderResult(null, e);
    }
  }

  @Override
  public void deliverResult(@Nullable LoaderResult data) {
    this.data = data;
    super.deliverResult(data);
  }

  /**
   * Perform a backup of the armored key in INBOX.
   *
   * @return true if message was send.
   */
  private boolean saveCreatedPrivateKeyAsBackupToInbox(NodeKeyDetails keyDetails) {
    try {
      Session session = OpenStoreHelper.getAccountSess(getContext(), account);
      Transport transport = SmtpProtocolUtil.prepareSmtpTransport(getContext(), session, account);
      Message msg = EmailUtil.genMsgWithPrivateKeys(getContext(), account, session,
          EmailUtil.genBodyPartWithPrivateKey(account, keyDetails.getPrivateKey()));
      transport.sendMessage(msg, msg.getAllRecipients());
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  private List<PgpContact> genContacts() throws Exception {
    PgpContact pgpContactMain = new PgpContact(account.getEmail(), account.getDisplayName());
    List<PgpContact> contacts = new ArrayList<>();

    switch (account.getAccountType()) {
      case AccountDao.ACCOUNT_TYPE_GOOGLE:
        contacts.add(pgpContactMain);
        Gmail gmail = GmailApiHelper.generateGmailApiService(getContext(), account);
        ListSendAsResponse aliases = gmail.users().settings().sendAs().list(GmailApiHelper.DEFAULT_USER_ID).execute();
        for (SendAs alias : aliases.getSendAs()) {
          if (alias.getVerificationStatus() != null) {
            contacts.add(new PgpContact(alias.getSendAsEmail(), alias.getDisplayName()));
          }
        }
        break;

      default:
        contacts.add(pgpContactMain);
        break;
    }

    return contacts;
  }

  /**
   * Registering a key with attester API.
   * Note: this will only be successful if it's the first time submitting a key for this email address, or if the
   * key being submitted has the same fingerprint as the one already recorded. If it's an error due to key
   * conflict, ignore the error.
   *
   * @param keyDetails Details of the created key.
   * @return true if no errors.
   */
  private boolean registerUserPublicKey(NodeKeyDetails keyDetails) {
    try {
      ApiService apiService = ApiHelper.getInstance(getContext()).getRetrofit().create(ApiService.class);
      InitialLegacySubmitModel model = new InitialLegacySubmitModel(account.getEmail(), keyDetails.getPublicKey());
      Response<InitialLegacySubmitResponse> response = apiService.postInitialLegacySubmit(model).execute();
      InitialLegacySubmitResponse body = response.body();
      return body != null && (body.getApiError() == null ||
          !(body.getApiError().getCode() >= 400 && body.getApiError().getCode() < 500));
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Request a test email from FlowCrypt.
   *
   * @param keyDetails Details of the created key.
   * @return true if no errors.
   */
  private boolean requestingTestMsgWithNewPublicKey(NodeKeyDetails keyDetails) {
    try {
      ApiService apiService = ApiHelper.getInstance(getContext()).getRetrofit().create(ApiService.class);
      TestWelcomeModel model = new TestWelcomeModel(account.getEmail(), keyDetails.getPublicKey());
      Response<TestWelcomeResponse> response = apiService.postTestWelcome(model).execute();

      TestWelcomeResponse testWelcomeResponse = response.body();
      return testWelcomeResponse != null && testWelcomeResponse.isSent();
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }
}
