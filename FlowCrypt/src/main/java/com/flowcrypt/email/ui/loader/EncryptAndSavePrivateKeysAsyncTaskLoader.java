/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Pair;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.api.retrofit.response.node.DecryptKeyResult;
import com.flowcrypt.email.database.dao.KeysDao;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.flowcrypt.email.util.exception.KeyAlreadyAddedException;

import java.util.ArrayList;
import java.util.List;

import androidx.loader.content.AsyncTaskLoader;

/**
 * This loader try to encrypt and save encrypted key with entered password by
 * {@link KeyStoreCryptoManager} to the database.
 * <p>
 * Return true if one or more key saved, false otherwise;
 *
 * @author DenBond7
 * Date: 03.05.2017
 * Time: 11:47
 * E-mail: DenBond7@gmail.com
 */

public class EncryptAndSavePrivateKeysAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {
  private List<NodeKeyDetails> details;
  private KeyDetails.Type type;
  private String passphrase;

  private KeysDaoSource keysDaoSource;

  public EncryptAndSavePrivateKeysAsyncTaskLoader(Context context, ArrayList<NodeKeyDetails> details,
                                                  KeyDetails.Type type, String passphrase) {
    super(context);
    this.details = details;
    this.type = type;
    this.passphrase = passphrase;
    this.keysDaoSource = new KeysDaoSource();
    onContentChanged();
  }

  @Override
  public LoaderResult loadInBackground() {
    List<NodeKeyDetails> acceptedKeysList = new ArrayList<>();
    try {
      KeyStoreCryptoManager keyStoreCryptoManager = new KeyStoreCryptoManager(getContext());
      for (NodeKeyDetails keyDetails : details) {
        String tempPassphrase = passphrase;
        if (keyDetails.isPrivate()) {
          String decryptedKey;
          if (keyDetails.isDecrypted()) {
            tempPassphrase = "";
            decryptedKey = keyDetails.getPrivateKey();
          } else {
            DecryptKeyResult decryptKeyResult = NodeCallsExecutor.decryptKey(keyDetails.getPrivateKey(), passphrase);
            decryptedKey = decryptKeyResult.getDecryptedKey();
          }

          if (!TextUtils.isEmpty(decryptedKey)) {
            if (!keysDaoSource.hasKey(getContext(), keyDetails.getLongId())) {
              KeysDao keysDao = KeysDao.generateKeysDao(keyStoreCryptoManager, type, keyDetails, tempPassphrase);
              Uri uri = keysDaoSource.addRow(getContext(), keysDao);

              List<PgpContact> contacts = keyDetails.getPgpContacts();
              List<Pair<String, String>> pairs = new ArrayList<>();
              if (contacts != null) {
                ContactsDaoSource contactsDaoSource = new ContactsDaoSource();
                pairs = genPairs(keyDetails, contacts, contactsDaoSource);
              }

              if (uri != null) {
                acceptedKeysList.add(keyDetails);
                UserIdEmailsKeysDaoSource userIdEmailsKeysDaoSource = new UserIdEmailsKeysDaoSource();

                for (Pair<String, String> pair : pairs) {
                  userIdEmailsKeysDaoSource.addRow(getContext(), pair.first, pair.second);
                }
              }
            } else if (details.size() == 1) {
              return new LoaderResult(null, new KeyAlreadyAddedException(keyDetails,
                  getContext().getString(R.string.the_key_already_added)));
            } else {
              acceptedKeysList.add(keyDetails);
            }
          } else if (details.size() == 1) {
            return new LoaderResult(null, new IllegalArgumentException(getContext().getString(R.string
                .password_is_incorrect)));
          }
        } else if (details.size() == 1) {
          return new LoaderResult(null, new IllegalArgumentException(getContext().getString(R.string.not_private_key)));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
      return new LoaderResult(null, e);
    }

    return new LoaderResult(acceptedKeysList, null);
  }

  @Override
  public void onStartLoading() {
    if (takeContentChanged()) {
      forceLoad();
    }
  }

  @Override
  public void onStopLoading() {
    cancelLoad();
  }

  private List<Pair<String, String>> genPairs(NodeKeyDetails keyDetails, List<PgpContact> contacts,
                                              ContactsDaoSource daoSource) {
    List<Pair<String, String>> pairs = new ArrayList<>();
    for (PgpContact pgpContact : contacts) {
      if (pgpContact != null) {
        pgpContact.setPubkey(keyDetails.getPublicKey());
        PgpContact temp = daoSource.getPgpContact(getContext(), pgpContact.getEmail());
        if (GeneralUtil.isEmailValid(pgpContact.getEmail()) && temp == null) {
          new ContactsDaoSource().addRow(getContext(), pgpContact);
          //todo-DenBond7 Need to resolve a situation with different public keys.
          //For example we can have a situation when we have to different public
          // keys with the same email
        }

        pairs.add(Pair.create(keyDetails.getLongId(), pgpContact.getEmail()));
      }
    }
    return pairs;
  }
}
