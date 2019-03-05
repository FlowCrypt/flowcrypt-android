/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.ExtraActionInfo;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.api.email.model.ServiceInfo;
import com.flowcrypt.email.database.dao.source.AccountAliasesDao;
import com.flowcrypt.email.database.dao.source.AccountAliasesDaoSource;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.UiJsManager;
import com.flowcrypt.email.js.core.Js;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.flowcrypt.email.model.MessageType;
import com.flowcrypt.email.model.UpdateInfoAboutPgpContactsResult;
import com.flowcrypt.email.model.messages.MessagePart;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.CreateMessageActivity;
import com.flowcrypt.email.ui.activity.ImportPublicKeyActivity;
import com.flowcrypt.email.ui.activity.SelectContactsActivity;
import com.flowcrypt.email.ui.activity.fragment.dialog.NoPgpFoundDialogFragment;
import com.flowcrypt.email.ui.activity.listeners.OnChangeMessageEncryptionTypeListener;
import com.flowcrypt.email.ui.adapter.FromAddressesAdapter;
import com.flowcrypt.email.ui.adapter.PgpContactAdapter;
import com.flowcrypt.email.ui.loader.LoadGmailAliasesLoader;
import com.flowcrypt.email.ui.loader.UpdateInfoAboutPgpContactsAsyncTaskLoader;
import com.flowcrypt.email.ui.widget.CustomChipSpanChipCreator;
import com.flowcrypt.email.ui.widget.PGPContactChipSpan;
import com.flowcrypt.email.ui.widget.PgpContactsNachoTextView;
import com.flowcrypt.email.ui.widget.SingleCharacterSpanChipTokenizer;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.android.gms.common.util.CollectionUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.hootsuite.nachos.NachoTextView;
import com.hootsuite.nachos.chip.Chip;
import com.hootsuite.nachos.terminator.ChipTerminatorHandler;
import com.hootsuite.nachos.tokenizer.ChipTokenizer;
import com.hootsuite.nachos.validator.ChipifyingNachoValidator;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

/**
 * This fragment describe a logic of sent an encrypted or standard message.
 *
 * @author DenBond7
 * Date: 10.05.2017
 * Time: 11:27
 * E-mail: DenBond7@gmail.com
 */

public class CreateMessageFragment extends BaseSyncFragment implements View.OnFocusChangeListener,
    AdapterView.OnItemSelectedListener, View.OnClickListener, PgpContactsNachoTextView.OnChipLongClickListener {
  private static final int REQUEST_CODE_NO_PGP_FOUND_DIALOG = 100;
  private static final int REQUEST_CODE_IMPORT_PUBLIC_KEY = 101;
  private static final int REQUEST_CODE_GET_CONTENT_FOR_SENDING = 102;
  private static final int REQUEST_CODE_COPY_PUBLIC_KEY_FROM_OTHER_CONTACT = 103;
  private static final int REQUEST_CODE_REQUEST_READ_EXTERNAL_STORAGE = 104;
  private static final int REQUEST_CODE_REQUEST_READ_EXTERNAL_STORAGE_FOR_EXTRA_INFO = 105;
  private static final String TAG = CreateMessageFragment.class.getSimpleName();

  private Js js;
  private OnMessageSendListener onMsgSendListener;
  private OnChangeMessageEncryptionTypeListener listener;
  private List<PgpContact> pgpContactsTo;
  private List<PgpContact> pgpContactsCc;
  private List<PgpContact> pgpContactsBcc;
  private ArrayList<AttachmentInfo> atts;
  private ContactsDaoSource contactsDaoSource;
  private FoldersManager.FolderType folderType;
  private IncomingMessageInfo msgInfo;
  private ServiceInfo serviceInfo;
  private AccountDao account;
  private FromAddressesAdapter<String> fromAddrs;
  private PgpContact pgpContactWithNoPublicKey;
  private ExtraActionInfo extraActionInfo;
  private MessageType messageType = MessageType.NEW;
  private File draftCacheDir;

  private ViewGroup layoutAtts;
  private EditText editTextFrom;
  private Spinner spinnerFrom;
  private PgpContactsNachoTextView recipientsTo;
  private PgpContactsNachoTextView recipientsCc;
  private PgpContactsNachoTextView recipientsBcc;
  private EditText editTextEmailSubject;
  private EditText editTextEmailMsg;
  private TextInputLayout textInputLayoutMsg;
  private ScrollView layoutContent;
  private View progressBarTo;
  private View progressBarCc;
  private View progressBarBcc;
  private View layoutCc;
  private View layoutBcc;
  private LinearLayout progressBarAndButtonLayout;
  private ImageButton imageButtonAliases;
  private View imageButtonAdditionalRecipientsVisibility;

  private boolean isContactsUpdateEnabled = true;
  private boolean isUpdateToCompleted = true;
  private boolean isUpdateCcCompleted = true;
  private boolean isUpdateBccCompleted = true;
  private boolean isIncomingMsgInfoUsed;
  private boolean isMsgSentToQueue;
  private int originalColor;

  public CreateMessageFragment() {
    pgpContactsTo = new ArrayList<>();
    pgpContactsCc = new ArrayList<>();
    pgpContactsBcc = new ArrayList<>();
    atts = new ArrayList<>();
    contactsDaoSource = new ContactsDaoSource();
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (context instanceof OnMessageSendListener) {
      this.onMsgSendListener = (OnMessageSendListener) context;
    } else throw new IllegalArgumentException(context.toString() + " must implement " +
        OnMessageSendListener.class.getSimpleName());

    if (context instanceof OnChangeMessageEncryptionTypeListener) {
      this.listener = (OnChangeMessageEncryptionTypeListener) context;
    } else throw new IllegalArgumentException(context.toString() + " must implement " +
        OnChangeMessageEncryptionTypeListener.class.getSimpleName());
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);

    initDraftCacheDir();

    account = new AccountDaoSource().getActiveAccountInformation(getContext());
    fromAddrs = new FromAddressesAdapter<>(getContext(), android.R.layout.simple_list_item_1, android.R.id.text1,
        new ArrayList<String>());
    fromAddrs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    fromAddrs.setUseKeysInfo(listener.getMsgEncryptionType()
        == MessageEncryptionType.ENCRYPTED);
    if (account != null) {
      fromAddrs.add(account.getEmail());
      fromAddrs.updateKeyAvailability(account.getEmail(), !CollectionUtils.isEmpty(
          new UserIdEmailsKeysDaoSource().getLongIdsByEmail(getContext(), account.getEmail())));
    }

    js = UiJsManager.getInstance(getContext()).getJs();
    initExtras(getActivity().getIntent());
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_create_message, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initViews(view);

    if ((msgInfo != null || extraActionInfo != null) && !isIncomingMsgInfoUsed) {
      this.isIncomingMsgInfoUsed = true;
      updateViews();
    }

    showAtts();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    if (account != null && AccountDao.ACCOUNT_TYPE_GOOGLE.equalsIgnoreCase(account.getAccountType())) {
      LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_email_aliases, null, this);
    }

    boolean isEncryptedMode = listener.getMsgEncryptionType() ==
        MessageEncryptionType.ENCRYPTED;
    if (msgInfo != null && GeneralUtil.isConnected(getContext()) && isEncryptedMode) {
      updateRecipients();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (!isMsgSentToQueue) {
      for (AttachmentInfo attInfo : atts) {
        if (attInfo.getUri() != null) {
          if (Constants.FILE_PROVIDER_AUTHORITY.equalsIgnoreCase(attInfo.getUri().getAuthority())) {
            getContext().getContentResolver().delete(attInfo.getUri(), null, null);
          }
        }
      }
    }
  }

  @Override
  public View getContentView() {
    return layoutContent;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_CODE_NO_PGP_FOUND_DIALOG:
        switch (resultCode) {
          case NoPgpFoundDialogFragment.RESULT_CODE_SWITCH_TO_STANDARD_EMAIL:
            listener.onMsgEncryptionTypeChanged(MessageEncryptionType.STANDARD);
            break;

          case NoPgpFoundDialogFragment.RESULT_CODE_IMPORT_THEIR_PUBLIC_KEY:
            if (data != null) {
              PgpContact pgpContact = data.getParcelableExtra(NoPgpFoundDialogFragment.EXTRA_KEY_PGP_CONTACT);

              if (pgpContact != null) {
                startActivityForResult(ImportPublicKeyActivity.newIntent(getContext(),
                    getString(R.string.import_public_key), pgpContact), REQUEST_CODE_IMPORT_PUBLIC_KEY);
              }
            }

            break;

          case NoPgpFoundDialogFragment.RESULT_CODE_COPY_FROM_OTHER_CONTACT:
            if (data != null) {
              pgpContactWithNoPublicKey = data.getParcelableExtra(NoPgpFoundDialogFragment.EXTRA_KEY_PGP_CONTACT);

              if (pgpContactWithNoPublicKey != null) {
                startActivityForResult(SelectContactsActivity.newIntent(getContext(),
                    getString(R.string.use_public_key_from), false), REQUEST_CODE_COPY_PUBLIC_KEY_FROM_OTHER_CONTACT);
              }
            }

            break;

          case NoPgpFoundDialogFragment.RESULT_CODE_REMOVE_CONTACT:
            if (data != null) {
              PgpContact pgpContact = data.getParcelableExtra(NoPgpFoundDialogFragment.EXTRA_KEY_PGP_CONTACT);

              if (pgpContact != null) {
                removePgpContact(pgpContact, recipientsTo, pgpContactsTo);
                removePgpContact(pgpContact, recipientsCc, pgpContactsCc);
                removePgpContact(pgpContact, recipientsBcc, pgpContactsBcc);
              }
            }
            break;
        }
        break;

      case REQUEST_CODE_IMPORT_PUBLIC_KEY:
        switch (resultCode) {
          case Activity.RESULT_OK:
            Toast.makeText(getContext(), R.string.the_key_successfully_imported, Toast.LENGTH_SHORT).show();
            updateRecipients();
            break;
        }
        break;

      case REQUEST_CODE_COPY_PUBLIC_KEY_FROM_OTHER_CONTACT:
        switch (resultCode) {
          case Activity.RESULT_OK:
            if (data != null) {
              PgpContact pgpContact = data.getParcelableExtra(SelectContactsActivity.KEY_EXTRA_PGP_CONTACT);

              if (pgpContact != null) {
                pgpContactWithNoPublicKey.setPubkey(pgpContact.getPubkey());
                new ContactsDaoSource().updatePgpContact(getContext(), pgpContactWithNoPublicKey);

                Toast.makeText(getContext(), R.string.key_successfully_copied, Toast.LENGTH_LONG).show();
                updateRecipients();
              }
            }
            break;
        }

        pgpContactWithNoPublicKey = null;
        break;

      case REQUEST_CODE_GET_CONTENT_FOR_SENDING:
        switch (resultCode) {
          case Activity.RESULT_OK:
            if (data != null && data.getData() != null) {
              AttachmentInfo attachmentInfo = EmailUtil.getAttInfoFromUri(getContext(), data.getData());
              if (hasAbilityToAddAtt(attachmentInfo)) {
                atts.add(attachmentInfo);
                showAtts();
              } else {
                showInfoSnackbar(getView(), getString(R.string.template_warning_max_total_attachments_size,
                    FileUtils.byteCountToDisplaySize(Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES)),
                    Snackbar.LENGTH_LONG);
              }
            } else {
              showInfoSnackbar(getView(), getString(R.string.can_not_attach_this_file), Snackbar.LENGTH_LONG);
            }
            break;
        }
        break;

      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.fragment_secure_compose, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menuActionSend:
        if (getSnackBar() != null) {
          getSnackBar().dismiss();
        }

        if (isUpdateToCompleted && isUpdateCcCompleted && isUpdateBccCompleted) {
          UIUtil.hideSoftInput(getContext(), getView());
          if (isDataCorrect()) {
            sendMsg();
            this.isMsgSentToQueue = true;
          }
        } else {
          Toast.makeText(getContext(), R.string.please_wait_while_information_about_contacts_will_be_updated,
              Toast.LENGTH_SHORT).show();
        }
        return true;

      case R.id.menuActionAttachFile:
        attachFile();
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    switch (requestCode) {
      case REQUEST_CODE_REQUEST_READ_EXTERNAL_STORAGE:
        if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          sendMsg();
        } else {
          Toast.makeText(getActivity(), R.string.cannot_send_attachment_without_read_permission,
              Toast.LENGTH_LONG).show();
        }
        break;

      case REQUEST_CODE_REQUEST_READ_EXTERNAL_STORAGE_FOR_EXTRA_INFO:
        if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          addAtts();
          showAtts();
        } else {
          Toast.makeText(getActivity(), R.string.cannot_send_attachment_without_read_permission,
              Toast.LENGTH_LONG).show();
        }
        break;

      default:
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  @NonNull
  @Override
  public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
    switch (id) {
      case R.id.loader_id_update_info_about_pgp_contacts_to:
        pgpContactsTo.clear();
        progressBarTo.setVisibility(View.VISIBLE);
        isUpdateToCompleted = false;
        return new UpdateInfoAboutPgpContactsAsyncTaskLoader(getContext(), recipientsTo.getChipAndTokenValues());

      case R.id.loader_id_update_info_about_pgp_contacts_cc:
        pgpContactsCc.clear();
        progressBarCc.setVisibility(View.VISIBLE);
        isUpdateCcCompleted = false;
        return new UpdateInfoAboutPgpContactsAsyncTaskLoader(getContext(), recipientsCc.getChipAndTokenValues());

      case R.id.loader_id_update_info_about_pgp_contacts_bcc:
        pgpContactsBcc.clear();
        progressBarBcc.setVisibility(View.VISIBLE);
        isUpdateBccCompleted = false;
        return new UpdateInfoAboutPgpContactsAsyncTaskLoader(getContext(), recipientsBcc.getChipAndTokenValues());

      case R.id.loader_id_load_email_aliases:
        return new LoadGmailAliasesLoader(getContext(), account);

      default:
        return super.onCreateLoader(id, args);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void onSuccess(int loaderId, Object result) {
    switch (loaderId) {
      case R.id.loader_id_update_info_about_pgp_contacts_to:
        isUpdateToCompleted = true;
        pgpContactsTo = getInfoAboutPgpContacts((UpdateInfoAboutPgpContactsResult) result, progressBarTo, R.string.to);

        if (pgpContactsTo != null && !pgpContactsTo.isEmpty()) {
          updateChips(recipientsTo, pgpContactsTo);
        }
        break;

      case R.id.loader_id_update_info_about_pgp_contacts_cc:
        isUpdateCcCompleted = true;
        pgpContactsCc = getInfoAboutPgpContacts((UpdateInfoAboutPgpContactsResult) result, progressBarCc, R.string.cc);

        if (pgpContactsCc != null && !pgpContactsCc.isEmpty()) {
          updateChips(recipientsCc, pgpContactsCc);
        }
        break;

      case R.id.loader_id_update_info_about_pgp_contacts_bcc:
        isUpdateBccCompleted = true;
        pgpContactsBcc = getInfoAboutPgpContacts((UpdateInfoAboutPgpContactsResult) result,
            progressBarBcc, R.string.bcc);

        if (pgpContactsBcc != null && !pgpContactsBcc.isEmpty()) {
          updateChips(recipientsBcc, pgpContactsBcc);
        }
        break;

      case R.id.loader_id_load_email_aliases:
        List<AccountAliasesDao> accountAliasesDaoList = (List<AccountAliasesDao>) result;
        List<String> aliases = new ArrayList<>();
        aliases.add(account.getEmail());

        for (AccountAliasesDao accountAliasesDao : accountAliasesDaoList) {
          aliases.add(accountAliasesDao.getSendAsEmail());
        }

        fromAddrs.clear();
        fromAddrs.addAll(aliases);

        for (String email : aliases) {
          fromAddrs.updateKeyAvailability(email, !CollectionUtils.isEmpty(new UserIdEmailsKeysDaoSource()
              .getLongIdsByEmail(getContext(), email)));
        }

        if (msgInfo != null) {
          prepareAliasForReplyIfNeeded(aliases);
        } else if (listener.getMsgEncryptionType() == MessageEncryptionType.ENCRYPTED) {
          showFirstMatchedAliasWithPrvKey(aliases);
        }

        if (fromAddrs.getCount() == 1) {
          if (imageButtonAliases.getVisibility() == View.VISIBLE) {
            imageButtonAliases.setVisibility(View.INVISIBLE);
          }
        } else {
          if (serviceInfo == null || serviceInfo.isFromFieldEditable()) {
            imageButtonAliases.setVisibility(View.VISIBLE);
          } else {
            imageButtonAliases.setVisibility(View.INVISIBLE);
          }
        }

        new AccountAliasesDaoSource().updateAliases(getContext(), account, accountAliasesDaoList);
        break;

      default:
        super.onSuccess(loaderId, result);
    }
  }

  @Override
  public void onError(int loaderId, Exception e) {
    switch (loaderId) {
      case R.id.loader_id_update_info_about_pgp_contacts_to:
        super.onError(loaderId, e);
        isUpdateToCompleted = true;
        progressBarTo.setVisibility(View.INVISIBLE);
        break;

      case R.id.loader_id_update_info_about_pgp_contacts_cc:
        super.onError(loaderId, e);
        isUpdateCcCompleted = true;
        progressBarCc.setVisibility(View.INVISIBLE);
        break;

      case R.id.loader_id_update_info_about_pgp_contacts_bcc:
        super.onError(loaderId, e);
        isUpdateBccCompleted = true;
        progressBarBcc.setVisibility(View.INVISIBLE);
        break;

      case R.id.loader_id_load_email_aliases:
        break;
    }
  }

  @Override
  public void onLoaderReset(Loader<LoaderResult> loader) {
    super.onLoaderReset(loader);
  }

  @Override
  public void onFocusChange(View v, boolean hasFocus) {
    switch (v.getId()) {
      case R.id.editTextRecipientTo:
        pgpContactsTo = runUpdatePgpContactsAction(pgpContactsTo, progressBarTo,
            R.id.loader_id_update_info_about_pgp_contacts_to, hasFocus);
        break;

      case R.id.editTextRecipientCc:
        pgpContactsCc = runUpdatePgpContactsAction(pgpContactsCc, progressBarCc,
            R.id.loader_id_update_info_about_pgp_contacts_cc, hasFocus);
        break;

      case R.id.editTextRecipientBcc:
        pgpContactsBcc = runUpdatePgpContactsAction(pgpContactsBcc, progressBarBcc,
            R.id.loader_id_update_info_about_pgp_contacts_bcc, hasFocus);
        break;

      case R.id.editTextEmailSubject:
      case R.id.editTextEmailMessage:
        if (hasFocus) {
          boolean isExpandButtonNeeded = false;
          if (TextUtils.isEmpty(recipientsCc.getText())) {
            layoutCc.setVisibility(View.GONE);
            isExpandButtonNeeded = true;
          }

          if (TextUtils.isEmpty(recipientsBcc.getText())) {
            layoutBcc.setVisibility(View.GONE);
            isExpandButtonNeeded = true;
          }

          if (isExpandButtonNeeded) {
            imageButtonAdditionalRecipientsVisibility.setVisibility(View.VISIBLE);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.gravity = Gravity.TOP | Gravity.END;
            progressBarAndButtonLayout.setLayoutParams(layoutParams);
          }
        }
        break;
    }
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    switch (parent.getId()) {
      case R.id.spinnerFrom:
        editTextFrom.setText((CharSequence) parent.getAdapter().getItem(position));
        if (listener.getMsgEncryptionType() == MessageEncryptionType.ENCRYPTED) {
          ArrayAdapter adapter = (ArrayAdapter) parent.getAdapter();
          int colorGray = UIUtil.getColor(getContext(), R.color.gray);
          editTextFrom.setTextColor(adapter.isEnabled(position) ? originalColor : colorGray);
        } else {
          editTextFrom.setTextColor(originalColor);
        }
        break;
    }
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {

  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.imageButtonAliases:
        if (fromAddrs.getCount() > 1) {
          spinnerFrom.performClick();
        }
        break;

      case R.id.imageButtonAdditionalRecipientsVisibility:
        layoutCc.setVisibility(View.VISIBLE);
        layoutBcc.setVisibility(View.VISIBLE);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.TOP | Gravity.END;

        progressBarAndButtonLayout.setLayoutParams(layoutParams);
        v.setVisibility(View.GONE);
        recipientsCc.requestFocus();
        break;
    }
  }

  @Override
  public void onChipLongClick(NachoTextView nachoTextView, @NonNull Chip chip, MotionEvent event) {
  }

  public void onMsgEncryptionTypeChange(MessageEncryptionType messageEncryptionType) {
    String emailMassageHint = null;
    if (messageEncryptionType != null) {
      switch (messageEncryptionType) {
        case ENCRYPTED:
          emailMassageHint = getString(R.string.prompt_compose_security_email);
          recipientsTo.getOnFocusChangeListener().onFocusChange(recipientsTo, false);
          recipientsCc.getOnFocusChangeListener().onFocusChange(recipientsCc, false);
          recipientsBcc.getOnFocusChangeListener().onFocusChange(recipientsBcc, false);
          fromAddrs.setUseKeysInfo(true);

          int colorGray = UIUtil.getColor(getContext(), R.color.gray);
          boolean isItemEnabled = fromAddrs.isEnabled(spinnerFrom.getSelectedItemPosition());
          editTextFrom.setTextColor(isItemEnabled ? originalColor : colorGray);

          break;

        case STANDARD:
          emailMassageHint = getString(R.string.prompt_compose_standard_email);
          pgpContactsTo.clear();
          pgpContactsCc.clear();
          pgpContactsBcc.clear();
          isUpdateToCompleted = true;
          isUpdateCcCompleted = true;
          isUpdateBccCompleted = true;
          fromAddrs.setUseKeysInfo(false);
          editTextFrom.setTextColor(originalColor);
          break;
      }
    }
    textInputLayoutMsg.setHint(emailMassageHint);
  }

  private void attachFile() {
    Intent intent = new Intent();
    intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("*/*");
    startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_attachment)),
        REQUEST_CODE_GET_CONTENT_FOR_SENDING);
  }

  private void initExtras(Intent intent) {
    if (intent != null) {
      if (intent.hasExtra(CreateMessageActivity.EXTRA_KEY_MESSAGE_TYPE)) {
        this.messageType = (MessageType) intent.getSerializableExtra(CreateMessageActivity.EXTRA_KEY_MESSAGE_TYPE);
      }

      if (!TextUtils.isEmpty(intent.getAction()) && intent.getAction().startsWith("android.intent.action")) {
        this.extraActionInfo = ExtraActionInfo.parseExtraActionInfo(getContext(), intent);

        if (hasExternalStorageUris(extraActionInfo.getAtts())) {
          boolean isPermissionGranted = ContextCompat.checkSelfPermission(getContext(),
              Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
          if (isPermissionGranted) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_CODE_REQUEST_READ_EXTERNAL_STORAGE_FOR_EXTRA_INFO);
          } else {
            addAtts();
          }
        } else {
          addAtts();
        }
      } else {
        this.serviceInfo = intent.getParcelableExtra(CreateMessageActivity.EXTRA_KEY_SERVICE_INFO);
        this.msgInfo = intent.getParcelableExtra(CreateMessageActivity.EXTRA_KEY_INCOMING_MESSAGE_INFO);

        if (msgInfo != null && msgInfo.getLocalFolder() != null) {
          this.folderType = FoldersManager.getFolderType(msgInfo.getLocalFolder());
        }

        if (this.serviceInfo != null && this.serviceInfo.getAtts() != null) {
          atts.addAll(this.serviceInfo.getAtts());
        }
      }
    }
  }

  private void initDraftCacheDir() {
    draftCacheDir = new File(getContext().getCacheDir(), Constants.DRAFT_CACHE_DIR);

    if (!draftCacheDir.exists()) {
      if (!draftCacheDir.mkdir()) {
        Log.e(TAG, "Create cache directory " + draftCacheDir.getName() + " filed!");
      }
    }
  }

  private void addAtts() {
    String sizeWarningMsg = getString(R.string.template_warning_max_total_attachments_size,
        FileUtils.byteCountToDisplaySize(Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES));

    for (AttachmentInfo attachmentInfo : extraActionInfo.getAtts()) {
      if (hasAbilityToAddAtt(attachmentInfo)) {

        if (TextUtils.isEmpty(attachmentInfo.getName())) {
          String msg = "attachmentInfo.getName() == null, uri = " + attachmentInfo.getUri();
          ExceptionUtil.handleError(new NullPointerException(msg));
          continue;
        }

        File draftAtt = new File(draftCacheDir, attachmentInfo.getName());

        try {
          InputStream inputStream = getContext().getContentResolver().openInputStream(attachmentInfo.getUri());

          if (inputStream != null) {
            FileUtils.copyInputStreamToFile(inputStream, draftAtt);
            Uri uri = FileProvider.getUriForFile(getContext(), Constants.FILE_PROVIDER_AUTHORITY, draftAtt);
            attachmentInfo.setUri(uri);
            atts.add(attachmentInfo);
          }
        } catch (IOException e) {
          e.printStackTrace();
          ExceptionUtil.handleError(e);

          if (!draftAtt.delete()) {
            Log.e(TAG, "Delete " + draftAtt.getName() + " filed!");
          }
        }
      } else {
        Toast.makeText(getContext(), sizeWarningMsg, Toast.LENGTH_SHORT).show();
        break;
      }
    }
  }

  private void updateRecipients() {
    LoaderManager.getInstance(this).restartLoader(R.id.loader_id_update_info_about_pgp_contacts_to, null, this);

    if (layoutCc.getVisibility() == View.VISIBLE) {
      LoaderManager.getInstance(this).restartLoader(R.id.loader_id_update_info_about_pgp_contacts_cc, null, this);
    } else {
      recipientsCc.setText((CharSequence) null);
      pgpContactsCc.clear();
    }

    if (layoutBcc.getVisibility() == View.VISIBLE) {
      LoaderManager.getInstance(this).restartLoader(R.id.loader_id_update_info_about_pgp_contacts_bcc, null, this);
    } else {
      recipientsBcc.setText((CharSequence) null);
      pgpContactsBcc.clear();
    }
  }

  /**
   * Get information about some {@link PgpContact}s.
   *
   * @param result                  An API result (lookup API).
   * @param progressBar             A {@link ProgressBar} which is showing an action progress.
   * @param additionalToastStringId A hint string id.
   */
  private List<PgpContact> getInfoAboutPgpContacts(UpdateInfoAboutPgpContactsResult result,
                                                   View progressBar, int additionalToastStringId) {
    progressBar.setVisibility(View.INVISIBLE);

    List<PgpContact> pgpContacts = null;

    if (result != null && result.getUpdatedPgpContacts() != null) {
      pgpContacts = result.getUpdatedPgpContacts();
    }

    if (result == null || !result.isAllInfoReceived()) {
      Toast.makeText(getContext(), getString(R.string.info_about_some_contacts_not_received,
          getString(additionalToastStringId)), Toast.LENGTH_SHORT).show();
    }

    return pgpContacts;
  }

  /**
   * Run an action to update information about some {@link PgpContact}s.
   *
   * @param pgpContacts Old {@link PgpContact}s
   * @param progressBar A {@link ProgressBar} which is showing an action progress.
   * @param loaderId    A loader id.
   * @param hasFocus    A value which indicates the view focus.
   * @return A modified contacts list.
   */
  private List<PgpContact> runUpdatePgpContactsAction(List<PgpContact> pgpContacts, View progressBar,
                                                      int loaderId, boolean hasFocus) {
    if (listener.getMsgEncryptionType() == MessageEncryptionType.ENCRYPTED) {
      progressBar.setVisibility(hasFocus ? View.INVISIBLE : View.VISIBLE);
      if (hasFocus) {
        pgpContacts.clear();
      } else {
        if (isContactsUpdateEnabled) {
          if (isAdded()) {
            LoaderManager.getInstance(this).restartLoader(loaderId, null, this);
          }
        } else {
          progressBar.setVisibility(View.INVISIBLE);
        }
      }
    }

    return pgpContacts;
  }

  /**
   * Prepare an alias for the reply. Will be used the email address that the email was received. Will be used the
   * first found matched email.
   *
   * @param aliases A list of Gmail aliases.
   */
  private void prepareAliasForReplyIfNeeded(List<String> aliases) {
    MessageEncryptionType messageEncryptionType = listener.getMsgEncryptionType();

    ArrayList<String> toAddresses;
    if (folderType == FoldersManager.FolderType.SENT) {
      toAddresses = msgInfo.getFrom();
    } else {
      toAddresses = msgInfo.getTo();
    }

    if (toAddresses != null) {
      String firstFoundedAlias = null;
      for (String toAddress : toAddresses) {
        if (firstFoundedAlias == null) {
          for (String alias : aliases) {
            if (alias.equalsIgnoreCase(toAddress)) {
              if (messageEncryptionType == MessageEncryptionType.ENCRYPTED && fromAddrs.hasPrvKey(alias)) {
                firstFoundedAlias = alias;
              } else {
                firstFoundedAlias = alias;
              }
              break;
            }
          }
        } else {
          break;
        }
      }

      if (firstFoundedAlias != null) {
        int position = fromAddrs.getPosition(firstFoundedAlias);
        if (position != -1) {
          spinnerFrom.setSelection(position);
        }
      }
    }
  }

  private void showFirstMatchedAliasWithPrvKey(List<String> aliases) {
    String firstFoundedAliasWithPrvKey = null;
    for (String alias : aliases) {
      if (fromAddrs.hasPrvKey(alias)) {
        firstFoundedAliasWithPrvKey = alias;
        break;
      }
    }

    if (firstFoundedAliasWithPrvKey != null) {
      int position = fromAddrs.getPosition(firstFoundedAliasWithPrvKey);
      if (position != -1) {
        spinnerFrom.setSelection(position);
      }
    }
  }

  /**
   * Generate an outgoing message info from entered information by user.
   *
   * @return <tt>OutgoingMessageInfo</tt> Return a created OutgoingMessageInfo object which
   * contains information about an outgoing message.
   */
  private OutgoingMessageInfo getOutgoingMsgInfo() {
    OutgoingMessageInfo messageInfo = new OutgoingMessageInfo();
    /*if (msgInfo != null && !TextUtils.isEmpty(msgInfo.getHtmlMsg())) {
      //todo-denbond7 Need to think how forward HTML
    }*/
    messageInfo.setMsg(editTextEmailMsg.getText().toString());
    messageInfo.setSubject(editTextEmailSubject.getText().toString());

    List<PgpContact> pgpContactsTo = new ArrayList<>();
    List<PgpContact> pgpContactsCc = new ArrayList<>();
    List<PgpContact> pgpContactsBcc = new ArrayList<>();

    if (msgInfo != null) {
      messageInfo.setRawReplyMsg(msgInfo.getOrigRawMsgWithoutAtts());
    }

    if (listener.getMsgEncryptionType() == MessageEncryptionType.ENCRYPTED) {
      pgpContactsTo = contactsDaoSource.getPgpContacts(getContext(), recipientsTo.getChipValues());
      pgpContactsCc = contactsDaoSource.getPgpContacts(getContext(), recipientsCc.getChipValues());
      pgpContactsBcc = contactsDaoSource.getPgpContacts(getContext(), recipientsBcc.getChipValues());
    } else {
      for (String s : recipientsTo.getChipValues()) {
        pgpContactsTo.add(new PgpContact(s, null));
      }

      for (String s : recipientsCc.getChipValues()) {
        pgpContactsCc.add(new PgpContact(s, null));
      }

      for (String s : recipientsBcc.getChipValues()) {
        pgpContactsBcc.add(new PgpContact(s, null));
      }
    }

    messageInfo.setToPgpContacts(pgpContactsTo.toArray(new PgpContact[0]));
    messageInfo.setCcPgpContacts(pgpContactsCc.toArray(new PgpContact[0]));
    messageInfo.setBccPgpContacts(pgpContactsBcc.toArray(new PgpContact[0]));
    messageInfo.setFromPgpContact(new PgpContact(editTextFrom.getText().toString(), null));
    messageInfo.setUid(EmailUtil.genOutboxUID(getContext()));

    return messageInfo;
  }

  /**
   * Check that all recipients have PGP.
   *
   * @return true if all recipients have PGP, other wise false.
   */
  private boolean hasRecipientWithoutPgp(boolean isRemoveActionEnabled, List<PgpContact> pgpContacts) {
    for (PgpContact pgpContact : pgpContacts) {
      if (!pgpContact.getHasPgp()) {
        showNoPgpFoundDialog(pgpContact, isRemoveActionEnabled);
        return true;
      }
    }

    return false;
  }

  /**
   * This method does update chips in the recipients field.
   *
   * @param view        A view which contains input {@link PgpContact}(s).
   * @param pgpContacts The input {@link PgpContact}(s)
   */
  private void updateChips(PgpContactsNachoTextView view, List<PgpContact> pgpContacts) {
    SpannableStringBuilder builder = new SpannableStringBuilder(view.getText());

    PGPContactChipSpan[] pgpContactChipSpans = builder.getSpans(0, view.length(), PGPContactChipSpan.class);

    if (pgpContactChipSpans.length > 0) {
      for (PgpContact pgpContact : pgpContacts) {
        for (PGPContactChipSpan pgpContactChipSpan : pgpContactChipSpans) {
          if (pgpContact.getEmail().equalsIgnoreCase(pgpContactChipSpan.getText().toString())) {
            pgpContactChipSpan.setHasPgp(pgpContact.getHasPgp());
            break;
          }
        }
      }
      view.invalidateChips();
    }
  }

  /**
   * Init an input {@link NachoTextView} using custom settings.
   *
   * @param pgpContactsNachoTextView An input {@link NachoTextView}
   */
  private void initChipsView(PgpContactsNachoTextView pgpContactsNachoTextView) {
    pgpContactsNachoTextView.setNachoValidator(new ChipifyingNachoValidator());
    pgpContactsNachoTextView.setIllegalCharacters(',');
    pgpContactsNachoTextView.addChipTerminator(' ', ChipTerminatorHandler.BEHAVIOR_CHIPIFY_TO_TERMINATOR);
    pgpContactsNachoTextView.setChipTokenizer(new SingleCharacterSpanChipTokenizer(getContext(),
        new CustomChipSpanChipCreator(getContext()), PGPContactChipSpan.class,
        SingleCharacterSpanChipTokenizer.CHIP_SEPARATOR_WHITESPACE));
    pgpContactsNachoTextView.setAdapter(preparePgpContactAdapter());
    pgpContactsNachoTextView.setOnFocusChangeListener(this);
    pgpContactsNachoTextView.setListener(this);
  }

  /**
   * Do a lot of checks to validate an outgoing message info.
   *
   * @return <tt>Boolean</tt> true if all information is correct, false otherwise.
   */
  private boolean isDataCorrect() {
    recipientsTo.chipifyAllUnterminatedTokens();
    recipientsCc.chipifyAllUnterminatedTokens();
    recipientsBcc.chipifyAllUnterminatedTokens();

    if (!fromAddrs.isEnabled(spinnerFrom.getSelectedItemPosition())) {
      showInfoSnackbar(recipientsTo, getString(R.string.no_key_available));
      return false;
    }

    if (TextUtils.isEmpty(recipientsTo.getText().toString())) {
      showInfoSnackbar(recipientsTo, getString(R.string.text_must_not_be_empty,
          getString(R.string.prompt_recipients_to)));
      recipientsTo.requestFocus();
      return false;
    }

    boolean hasNotValidEmail = hasNotValidEmail(recipientsTo) || hasNotValidEmail(recipientsCc)
        || hasNotValidEmail(recipientsBcc);

    if (!hasNotValidEmail) {
      if (listener.getMsgEncryptionType() == MessageEncryptionType.ENCRYPTED) {
        if (!TextUtils.isEmpty(recipientsTo.getText()) && pgpContactsTo.isEmpty()) {
          showUpdateContactsSnackBar(R.id.loader_id_update_info_about_pgp_contacts_to);
          return false;
        }

        if (!TextUtils.isEmpty(recipientsCc.getText()) && pgpContactsCc.isEmpty()) {
          showUpdateContactsSnackBar(R.id.loader_id_update_info_about_pgp_contacts_cc);
          return false;
        }

        if (!TextUtils.isEmpty(recipientsBcc.getText()) && pgpContactsBcc.isEmpty()) {
          showUpdateContactsSnackBar(R.id.loader_id_update_info_about_pgp_contacts_bcc);
          return false;
        }

        if (hasRecipientWithoutPgp(true, pgpContactsTo)
            || hasRecipientWithoutPgp(true, pgpContactsCc)
            || hasRecipientWithoutPgp(true, pgpContactsBcc)) {
          return false;
        }
      }
    } else return false;

    if (TextUtils.isEmpty(editTextEmailSubject.getText().toString())) {
      showInfoSnackbar(editTextEmailSubject, getString(R.string.text_must_not_be_empty,
          getString(R.string.prompt_subject)));
      editTextEmailSubject.requestFocus();
      return false;
    }

    if ((atts != null && atts.isEmpty()) && TextUtils.isEmpty(editTextEmailMsg.getText().toString())) {
      showInfoSnackbar(editTextEmailMsg, getString(R.string.sending_message_must_not_be_empty));
      editTextEmailMsg.requestFocus();
      return false;
    }

    if (atts != null && !atts.isEmpty() && hasExternalStorageUris(this.atts)) {
      boolean isPermissionGranted = ContextCompat.checkSelfPermission(getContext(),
          Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
      if (isPermissionGranted) {
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
            REQUEST_CODE_REQUEST_READ_EXTERNAL_STORAGE);
        return false;
      } else {
        return true;
      }
    } else {
      return true;
    }
  }

  private void showUpdateContactsSnackBar(final int loaderId) {
    showSnackbar(getView(), getString(R.string.please_update_information_about_contacts),
        getString(R.string.update), Snackbar.LENGTH_LONG, new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            if (GeneralUtil.isConnected(getContext())) {
              LoaderManager.getInstance(CreateMessageFragment.this).restartLoader(loaderId, null,
                  CreateMessageFragment.this);
            } else {
              showInfoSnackbar(getView(), getString(R.string.internet_connection_is_not_available));
            }
          }
        });
  }

  private boolean hasExternalStorageUris(List<AttachmentInfo> attachmentInfoList) {
    for (AttachmentInfo attachmentInfo : attachmentInfoList) {
      if (attachmentInfo.getUri() != null
          && ContentResolver.SCHEME_FILE.equalsIgnoreCase(attachmentInfo.getUri().getScheme())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Remove the current {@link PgpContact} from recipients.
   *
   * @param pgpContact               The {@link PgpContact} which will be removed.
   * @param pgpContactsNachoTextView The {@link NachoTextView} which contains the delete candidate.
   * @param pgpContacts              The list which contains the delete candidate.
   */
  private void removePgpContact(PgpContact pgpContact, PgpContactsNachoTextView pgpContactsNachoTextView,
                                List<PgpContact> pgpContacts) {
    ChipTokenizer chipTokenizer = pgpContactsNachoTextView.getChipTokenizer();
    for (Chip chip : pgpContactsNachoTextView.getAllChips()) {
      if (pgpContact.getEmail()
          .equalsIgnoreCase(chip.getText().toString()) && chipTokenizer != null) {
        chipTokenizer.deleteChip(chip, pgpContactsNachoTextView.getText());
      }
    }

    for (Iterator<PgpContact> iterator = pgpContacts.iterator(); iterator.hasNext(); ) {
      PgpContact next = iterator.next();
      if (next.getEmail().equalsIgnoreCase(next.getEmail())) {
        iterator.remove();
      }
    }
  }

  /**
   * Init fragment views
   *
   * @param view The root fragment view.
   */
  private void initViews(View view) {
    layoutContent = view.findViewById(R.id.scrollView);
    layoutAtts = view.findViewById(R.id.layoutAtts);
    layoutCc = view.findViewById(R.id.layoutCc);
    layoutBcc = view.findViewById(R.id.layoutBcc);
    progressBarAndButtonLayout = view.findViewById(R.id.progressBarAndButtonLayout);

    recipientsTo = view.findViewById(R.id.editTextRecipientTo);
    recipientsCc = view.findViewById(R.id.editTextRecipientCc);
    recipientsBcc = view.findViewById(R.id.editTextRecipientBcc);

    initChipsView(recipientsTo);
    initChipsView(recipientsCc);
    initChipsView(recipientsBcc);

    spinnerFrom = view.findViewById(R.id.spinnerFrom);
    spinnerFrom.setOnItemSelectedListener(this);
    spinnerFrom.setAdapter(fromAddrs);

    editTextFrom = view.findViewById(R.id.editTextFrom);
    originalColor = editTextFrom.getCurrentTextColor();

    imageButtonAliases = view.findViewById(R.id.imageButtonAliases);
    if (imageButtonAliases != null) {
      imageButtonAliases.setOnClickListener(this);
    }

    imageButtonAdditionalRecipientsVisibility = view.findViewById(R.id.imageButtonAdditionalRecipientsVisibility);
    if (imageButtonAdditionalRecipientsVisibility != null) {
      imageButtonAdditionalRecipientsVisibility.setOnClickListener(this);
    }

    editTextEmailSubject = view.findViewById(R.id.editTextEmailSubject);
    editTextEmailSubject.setOnFocusChangeListener(this);
    editTextEmailMsg = view.findViewById(R.id.editTextEmailMessage);
    editTextEmailMsg.setOnFocusChangeListener(this);
    textInputLayoutMsg = view.findViewById(R.id.textInputLayoutEmailMessage);

    progressBarTo = view.findViewById(R.id.progressBarTo);
    progressBarCc = view.findViewById(R.id.progressBarCc);
    progressBarBcc = view.findViewById(R.id.progressBarBcc);
  }

  /**
   * Update views on the screen. This method can be called when we need to update the current
   * screen.
   */
  private void updateViews() {
    onMsgEncryptionTypeChange(listener.getMsgEncryptionType());

    if (extraActionInfo != null) {
      updateViewsFromExtraActionInfo();
    } else {
      if (msgInfo != null) {
        updateViewsFromIncomingMsgInfo();
        recipientsTo.chipifyAllUnterminatedTokens();
        recipientsCc.chipifyAllUnterminatedTokens();
        editTextEmailSubject.setText(prepareReplySubject(msgInfo.getSubject()));
      }

      if (serviceInfo != null) {
        updateViewsFromServiceInfo();
      }
    }
  }

  private void updateViewsFromExtraActionInfo() {
    setupPgpFromExtraActionInfo(recipientsTo, extraActionInfo.getToAddresses());
    setupPgpFromExtraActionInfo(recipientsCc, extraActionInfo.getCcAddresses());
    setupPgpFromExtraActionInfo(recipientsBcc, extraActionInfo.getBccAddresses());

    editTextEmailSubject.setText(extraActionInfo.getSubject());
    editTextEmailMsg.setText(extraActionInfo.getBody());

    if (TextUtils.isEmpty(recipientsTo.getText())) {
      recipientsTo.requestFocus();
      return;
    }

    if (TextUtils.isEmpty(editTextEmailSubject.getText())) {
      editTextEmailSubject.requestFocus();
      return;
    }

    editTextEmailMsg.requestFocus();
  }

  private void updateViewsFromServiceInfo() {
    recipientsTo.setFocusable(serviceInfo.isToFieldEditable());
    recipientsTo.setFocusableInTouchMode(serviceInfo.isToFieldEditable());
    //todo-denbond7 Need to add a similar option for recipientsCc and recipientsBcc

    editTextEmailSubject.setFocusable(serviceInfo.isSubjectEditable());
    editTextEmailSubject.setFocusableInTouchMode(serviceInfo.isSubjectEditable());

    editTextEmailMsg.setFocusable(serviceInfo.isMsgEditable());
    editTextEmailMsg.setFocusableInTouchMode(serviceInfo.isMsgEditable());

    if (!TextUtils.isEmpty(serviceInfo.getSystemMsg())) {
      editTextEmailMsg.setText(serviceInfo.getSystemMsg());
    }
  }

  private void updateViewsFromIncomingMsgInfo() {
    switch (messageType) {
      case REPLY:
        updateViewsIfReplyMode();
        break;

      case REPLY_ALL:
        updateViewsIfReplyAllMode();
        break;

      case FORWARD:
        updateViewsIfFwdMode();
        break;
    }
  }

  private void updateViewsIfFwdMode() {
    if (msgInfo.getAtts() != null
        && !msgInfo.getAtts().isEmpty()) {
      for (AttachmentInfo att : msgInfo.getAtts()) {
        if (hasAbilityToAddAtt(att)) {
          atts.add(att);
        } else {
          showInfoSnackbar(getView(), getString(R.string.template_warning_max_total_attachments_size,
              FileUtils.byteCountToDisplaySize(Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES)),
              Snackbar.LENGTH_LONG);
        }
      }
    }

    editTextEmailMsg.setText(getString(R.string.forward_template, msgInfo.getFrom().get(0),
        EmailUtil.genForwardedMsgDate(msgInfo.getReceiveDate()), msgInfo.getSubject(),
        prepareRecipientsLineForForwarding(msgInfo.getTo())));

    if (msgInfo.getCc() != null && !msgInfo.getCc().isEmpty()) {
      editTextEmailMsg.append("Cc: ");
      editTextEmailMsg.append(prepareRecipientsLineForForwarding(msgInfo.getCc()));
      editTextEmailMsg.append("\n\n");
    }

    if (msgInfo.getMsgParts() != null && !msgInfo.getMsgParts().isEmpty()) {
      for (MessagePart msgPart : msgInfo.getMsgParts()) {
        if (msgPart != null) {
          switch (msgPart.getMsgPartType()) {
            case PGP_MESSAGE:
            case TEXT:
              editTextEmailMsg.append("\n\n");
              editTextEmailMsg.append(msgPart.getValue());
              break;

            case PGP_PUBLIC_KEY:
              //TODO-denbond7 add implementation of the public key view
              break;
          }
        }
      }
    } else if (!msgInfo.hasPlainText() && !TextUtils.isEmpty(msgInfo.getHtmlMsg())) {
      Toast.makeText(getContext(), R.string.cannot_forward_html_emails, Toast.LENGTH_LONG).show();
    }
  }

  private void updateViewsIfReplyAllMode() {
    if (folderType == FoldersManager.FolderType.SENT || folderType == FoldersManager.FolderType.OUTBOX) {
      recipientsTo.setText(prepareRecipients(msgInfo.getTo()));

      if (msgInfo.getCc() != null && !msgInfo.getCc().isEmpty()) {
        layoutCc.setVisibility(View.VISIBLE);
        recipientsCc.append(prepareRecipients(msgInfo.getCc()));
      }
    } else {
      recipientsTo.setText(prepareRecipients(msgInfo.getFrom()));

      Set<String> ccSet = new HashSet<>();

      if (msgInfo.getTo() != null && !msgInfo.getTo().isEmpty()) {
        ArrayList<String> toRecipients = new ArrayList<>(msgInfo.getTo());
        toRecipients.remove(account.getEmail());

        if (AccountDao.ACCOUNT_TYPE_GOOGLE.equalsIgnoreCase(account.getAccountType())) {
          List<AccountAliasesDao> accountAliases = new AccountAliasesDaoSource().getAliases(getContext(), account);
          for (AccountAliasesDao accountAliasesDao : accountAliases) {
            toRecipients.remove(accountAliasesDao.getSendAsEmail());
          }
        }

        ccSet.addAll(toRecipients);
      }

      if (msgInfo.getCc() != null) {
        ArrayList<String> ccRecipients = msgInfo.getCc();
        ccRecipients.remove(account.getEmail());
        ccSet.addAll(ccRecipients);
      }

      if (!ccSet.isEmpty()) {
        layoutCc.setVisibility(View.VISIBLE);
        recipientsCc.append(prepareRecipients(new ArrayList<>(ccSet)));
      }
    }

    if (!TextUtils.isEmpty(recipientsTo.getText()) || !TextUtils.isEmpty(recipientsCc.getText())) {
      editTextEmailMsg.requestFocus();
    }
  }

  private void updateViewsIfReplyMode() {
    if (folderType != null) {
      switch (folderType) {
        case SENT:
        case OUTBOX:
          recipientsTo.setText(prepareRecipients(msgInfo.getTo()));
          break;

        default:
          recipientsTo.setText(prepareRecipients(msgInfo.getFrom()));
          break;
      }
    } else {
      recipientsTo.setText(prepareRecipients(msgInfo.getFrom()));
    }

    if (!TextUtils.isEmpty(recipientsTo.getText())) {
      editTextEmailMsg.requestFocus();
    }
  }

  private void setupPgpFromExtraActionInfo(PgpContactsNachoTextView pgpContactsNachoTextView,
                                           ArrayList<String> addresses) {
    if (addresses != null && !addresses.isEmpty()) {
      pgpContactsNachoTextView.setText(prepareRecipients(addresses));
      pgpContactsNachoTextView.chipifyAllUnterminatedTokens();
      pgpContactsNachoTextView.getOnFocusChangeListener().onFocusChange(pgpContactsNachoTextView, false);
    }
  }

  private String prepareRecipientsLineForForwarding(ArrayList<String> recipients) {
    StringBuilder stringBuilder = new StringBuilder();
    if (recipients != null && !recipients.isEmpty()) {
      stringBuilder.append(recipients.get(0));

      if (recipients.size() > 1) {
        for (int i = 1; i < recipients.size(); i++) {
          String recipient = recipients.get(i);
          stringBuilder.append(", ");
          stringBuilder.append(recipient);
        }
      }

      return stringBuilder.toString();
    } else return "";
  }

  @NonNull
  private String prepareReplySubject(String subject) {
    String prefix = null;

    switch (messageType) {
      case REPLY:
      case REPLY_ALL:
        prefix = "Re";
        break;

      case FORWARD:
        prefix = "Fwd";
        break;
    }

    if (!TextUtils.isEmpty(prefix)) {
      if (TextUtils.isEmpty(subject)) {
        return getString(R.string.template_reply_subject, prefix, "");
      }

      Pattern pattern = Pattern.compile("^(" + prefix + ": )", Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(subject);

      if (matcher.find()) {
        return subject;
      }

      return getString(R.string.template_reply_subject, prefix, subject);
    } else {
      return subject;
    }
  }

  private String prepareRecipients(List<String> recipients) {
    StringBuilder stringBuilder = new StringBuilder();
    if (recipients != null) {
      for (String s : recipients) {
        stringBuilder.append(s).append(" ");
      }
    }

    return stringBuilder.toString();
  }

  /**
   * Prepare a {@link PgpContactAdapter} for the {@link NachoTextView} object.
   *
   * @return <tt>{@link PgpContactAdapter}</tt>
   */
  private PgpContactAdapter preparePgpContactAdapter() {
    PgpContactAdapter pgpContactAdapter = new PgpContactAdapter(getContext(), null, true);
    //setup a search contacts logic in the database
    pgpContactAdapter.setFilterQueryProvider(new FilterQueryProvider() {
      @Override
      public Cursor runQuery(CharSequence constraint) {
        Uri uri = new ContactsDaoSource().getBaseContentUri();
        String selection = ContactsDaoSource.COL_EMAIL + " LIKE ?";
        String[] selectionArgs = new String[]{"%" + constraint + "%"};
        String sortOrder = ContactsDaoSource.COL_LAST_USE + " DESC";

        return getContext().getContentResolver().query(uri, null, selection, selectionArgs, sortOrder);
      }
    });

    return pgpContactAdapter;
  }

  /**
   * Check is the given {@link PgpContactsNachoTextView} has a not valid email.
   *
   * @return <tt>boolean</tt> true - if has, otherwise false..
   */
  private boolean hasNotValidEmail(PgpContactsNachoTextView pgpContactsNachoTextView) {
    List<String> emails = pgpContactsNachoTextView.getChipAndTokenValues();
    for (String email : emails) {
      if (!GeneralUtil.isEmailValid(email)) {
        showInfoSnackbar(pgpContactsNachoTextView, getString(R.string.error_some_email_is_not_valid, email));
        pgpContactsNachoTextView.requestFocus();
        return true;
      }
    }
    return false;
  }

  /**
   * Check is attachment can be added to the current message.
   *
   * @param newAttInfo The new attachment which will be maybe added.
   * @return true if the attachment can be added, otherwise false.
   */
  private boolean hasAbilityToAddAtt(AttachmentInfo newAttInfo) {
    int totalSizeOfAtts = 0;

    for (AttachmentInfo attachmentInfo : atts) {
      totalSizeOfAtts += attachmentInfo.getEncodedSize();
    }

    totalSizeOfAtts += newAttInfo.getEncodedSize();

    return totalSizeOfAtts < Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES;
  }


  /**
   * Show a dialog where we can select different actions.
   *
   * @param pgpContact            The {@link PgpContact} which will be used when we select the
   *                              remove action.
   * @param isRemoveActionEnabled true if we want to show the remove action, false otherwise.
   */
  private void showNoPgpFoundDialog(PgpContact pgpContact, boolean isRemoveActionEnabled) {
    NoPgpFoundDialogFragment dialogFragment = NoPgpFoundDialogFragment.newInstance(pgpContact, isRemoveActionEnabled);
    dialogFragment.setTargetFragment(this, REQUEST_CODE_NO_PGP_FOUND_DIALOG);
    dialogFragment.show(getFragmentManager(), NoPgpFoundDialogFragment.class.getSimpleName());
  }

  /**
   * Send a message.
   */
  private void sendMsg() {
    dismissCurrentSnackBar();

    isContactsUpdateEnabled = false;
    OutgoingMessageInfo msgInfo = getOutgoingMsgInfo();

    ArrayList<AttachmentInfo> forwardedAttInfoList = getForwardedAtts();
    ArrayList<AttachmentInfo> attachmentInfoList = new ArrayList<>(this.atts);
    attachmentInfoList.removeAll(forwardedAttInfoList);

    msgInfo.setAtts(attachmentInfoList);
    msgInfo.setForwardedAtts(forwardedAttInfoList);
    msgInfo.setEncryptionType(listener.getMsgEncryptionType());
    msgInfo.setForwarded(messageType == MessageType.FORWARD);

    if (onMsgSendListener != null) {
      onMsgSendListener.sendMsg(msgInfo);
    }
  }

  /**
   * Generate a forwarded attachments list.
   *
   * @return The generated list.
   */
  private ArrayList<AttachmentInfo> getForwardedAtts() {
    ArrayList<AttachmentInfo> atts = new ArrayList<>();

    for (AttachmentInfo att : this.atts) {
      if (att.getId() != null && att.isForwarded()) {
        atts.add(att);
      }
    }

    return atts;
  }

  /**
   * Show attachments which were added.
   */
  private void showAtts() {
    if (!atts.isEmpty()) {
      layoutAtts.removeAllViews();
      LayoutInflater layoutInflater = LayoutInflater.from(getContext());
      for (final AttachmentInfo att : atts) {
        final View rootView = layoutInflater.inflate(R.layout.attachment_item, layoutAtts, false);

        TextView textViewAttName = rootView.findViewById(R.id.textViewAttchmentName);
        textViewAttName.setText(att.getName());

        TextView textViewAttSize = rootView.findViewById(R.id.textViewAttSize);
        if (att.getEncodedSize() > 0) {
          textViewAttSize.setVisibility(View.VISIBLE);
          textViewAttSize.setText(Formatter.formatFileSize(getContext(), att.getEncodedSize()));
        } else {
          textViewAttSize.setVisibility(View.GONE);
        }

        View imageButtonDownloadAtt = rootView.findViewById(R.id.imageButtonDownloadAtt);
        imageButtonDownloadAtt.setVisibility(View.GONE);

        if (!att.isProtected()) {
          View imageButtonClearAtt = rootView.findViewById(R.id.imageButtonClearAtt);
          imageButtonClearAtt.setVisibility(View.VISIBLE);
          imageButtonClearAtt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              atts.remove(att);
              layoutAtts.removeView(rootView);

              //Remove a temp file which was created by our app
              Uri uri = att.getUri();
              if (uri != null && Constants.FILE_PROVIDER_AUTHORITY.equalsIgnoreCase(uri.getAuthority())) {
                getContext().getContentResolver().delete(uri, null, null);
              }
            }
          });
        }
        layoutAtts.addView(rootView);
      }
    } else {
      layoutAtts.removeAllViews();
    }
  }

  /**
   * This interface will be used when we send a message.
   */
  public interface OnMessageSendListener {
    void sendMsg(OutgoingMessageInfo outgoingMsgInfo);
  }
}
