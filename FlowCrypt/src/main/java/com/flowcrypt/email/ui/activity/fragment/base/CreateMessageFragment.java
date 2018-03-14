/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.Formatter;
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
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.JsForUiManager;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.flowcrypt.email.model.UpdateInfoAboutPgpContactsResult;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.CreateMessageActivity;
import com.flowcrypt.email.ui.activity.ImportPublicKeyActivity;
import com.flowcrypt.email.ui.activity.SelectContactsActivity;
import com.flowcrypt.email.ui.activity.fragment.dialog.NoPgpFoundDialogFragment;
import com.flowcrypt.email.ui.activity.fragment.dialog.PgpContactDialogFragment;
import com.flowcrypt.email.ui.activity.listeners.OnChangeMessageEncryptedTypeListener;
import com.flowcrypt.email.ui.adapter.PgpContactAdapter;
import com.flowcrypt.email.ui.loader.LoadGmailAliasesLoader;
import com.flowcrypt.email.ui.loader.UpdateInfoAboutPgpContactsAsyncTaskLoader;
import com.flowcrypt.email.ui.widget.CustomChipSpanChipCreator;
import com.flowcrypt.email.ui.widget.PGPContactChipSpan;
import com.flowcrypt.email.ui.widget.PgpContactsNachoTextView;
import com.flowcrypt.email.ui.widget.SingleCharacterSpanChipTokenizer;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.RFC6068Parser;
import com.flowcrypt.email.util.UIUtil;
import com.hootsuite.nachos.NachoTextView;
import com.hootsuite.nachos.chip.Chip;
import com.hootsuite.nachos.terminator.ChipTerminatorHandler;
import com.hootsuite.nachos.tokenizer.ChipTokenizer;
import com.hootsuite.nachos.validator.ChipifyingNachoValidator;

import org.apache.commons.io.FileUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This fragment describe a logic of sent an encrypted or standard message.
 *
 * @author DenBond7
 *         Date: 10.05.2017
 *         Time: 11:27
 *         E-mail: DenBond7@gmail.com
 */

public class CreateMessageFragment extends BaseGmailFragment implements View.OnFocusChangeListener,
        AdapterView.OnItemSelectedListener, View.OnClickListener, PgpContactsNachoTextView.OnChipLongClickListener {
    private static final int REQUEST_CODE_NO_PGP_FOUND_DIALOG = 100;
    private static final int REQUEST_CODE_IMPORT_PUBLIC_KEY = 101;
    private static final int REQUEST_CODE_GET_CONTENT_FOR_SENDING = 102;
    private static final int REQUEST_CODE_COPY_PUBLIC_KEY_FROM_OTHER_CONTACT = 103;
    private static final int REQUEST_CODE_SHOW_PGP_CONTACT_DIALOG = 105;
    private static final int REQUEST_CODE_REQUEST_WRITE_EXTERNAL_STORAGE = 106;

    private Js js;
    private OnMessageSendListener onMessageSendListener;
    private OnChangeMessageEncryptedTypeListener onChangeMessageEncryptedTypeListener;
    private List<PgpContact> pgpContacts;
    private ArrayList<AttachmentInfo> attachmentInfoList;
    private PgpContactsNachoTextView editTextRecipients;
    private ContactsDaoSource contactsDaoSource;
    private FoldersManager.FolderType folderType;
    private IncomingMessageInfo incomingMessageInfo;
    private ServiceInfo serviceInfo;

    private ViewGroup layoutAttachments;
    private EditText editTextFrom;
    private EditText editTextEmailSubject;
    private EditText editTextEmailMessage;
    private TextInputLayout textInputLayoutEmailMessage;
    private View layoutContent;
    private View progressBarCheckContactsDetails;
    private Spinner spinnerFrom;
    private AccountDao accountDao;
    private ArrayAdapter<String> fromAddressesArrayAdapter;

    private boolean isUpdateInfoAboutContactsEnable = true;
    private boolean isUpdatedInfoAboutContactCompleted = true;
    private boolean isMessageSendingNow;
    private boolean isIncomingMessageInfoUsed;
    private PgpContact pgpContactWithNoPublicKey;
    private ExtraActionInfo extraActionInfo;

    public CreateMessageFragment() {
        pgpContacts = new ArrayList<>();
        attachmentInfoList = new ArrayList<>();
        contactsDaoSource = new ContactsDaoSource();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMessageSendListener) {
            this.onMessageSendListener = (OnMessageSendListener) context;
        } else throw new IllegalArgumentException(context.toString() + " must implement " +
                OnMessageSendListener.class.getSimpleName());

        if (context instanceof OnChangeMessageEncryptedTypeListener) {
            this.onChangeMessageEncryptedTypeListener = (OnChangeMessageEncryptedTypeListener) context;
        } else throw new IllegalArgumentException(context.toString() + " must implement " +
                OnChangeMessageEncryptedTypeListener.class.getSimpleName());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        accountDao = new AccountDaoSource().getActiveAccountInformation(getContext());
        fromAddressesArrayAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, android.R.id.text1, new ArrayList<String>());
        fromAddressesArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (accountDao != null) {
            fromAddressesArrayAdapter.add(accountDao.getEmail());
        }

        js = JsForUiManager.getInstance(getContext()).getJs();

        Intent intent = getActivity().getIntent();
        if (intent != null) {
            if (!TextUtils.isEmpty(intent.getAction()) && intent.getAction().startsWith("android.intent.action")) {
                parseExtraActionInfo(intent);
            } else {
                this.serviceInfo = intent.getParcelableExtra(CreateMessageActivity.EXTRA_KEY_SERVICE_INFO);
                this.incomingMessageInfo = intent.getParcelableExtra(
                        CreateMessageActivity.EXTRA_KEY_INCOMING_MESSAGE_INFO);

                if (incomingMessageInfo != null && incomingMessageInfo.getFolder() != null) {
                    this.folderType = FoldersManager.getFolderTypeForImapFolder(incomingMessageInfo.getFolder());
                }

                if (this.serviceInfo != null && this.serviceInfo.getAttachmentInfoList() != null) {
                    attachmentInfoList.addAll(this.serviceInfo.getAttachmentInfoList());
                }
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_message, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        showAttachments();

        if ((incomingMessageInfo != null || extraActionInfo != null) && !isIncomingMessageInfoUsed) {
            this.isIncomingMessageInfoUsed = true;
            updateViews();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (accountDao != null && AccountDao.ACCOUNT_TYPE_GOOGLE.equalsIgnoreCase(accountDao.getAccountType())) {
            getLoaderManager().restartLoader(R.id.loader_id_load_email_aliases, null, this);
        }

        if (incomingMessageInfo != null && GeneralUtil.isInternetConnectionAvailable(getContext())
                && onChangeMessageEncryptedTypeListener.getMessageEncryptionType() == MessageEncryptionType.ENCRYPTED) {
            getLoaderManager().restartLoader(R.id.loader_id_update_info_about_pgp_contacts, null, this);
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
                        onChangeMessageEncryptedTypeListener.onMessageEncryptionTypeChange(MessageEncryptionType
                                .STANDARD);
                        break;

                    case NoPgpFoundDialogFragment.RESULT_CODE_IMPORT_THEIR_PUBLIC_KEY:
                        if (data != null) {
                            PgpContact pgpContact = data.getParcelableExtra(
                                    NoPgpFoundDialogFragment.EXTRA_KEY_PGP_CONTACT);

                            if (pgpContact != null) {
                                startActivityForResult(
                                        ImportPublicKeyActivity.newIntent(getContext(),
                                                getString(R.string.import_public_key), pgpContact),
                                        REQUEST_CODE_IMPORT_PUBLIC_KEY);
                            }
                        }

                        break;

                    case NoPgpFoundDialogFragment.RESULT_CODE_COPY_FROM_OTHER_CONTACT:
                        if (data != null) {
                            pgpContactWithNoPublicKey = data.getParcelableExtra(NoPgpFoundDialogFragment
                                    .EXTRA_KEY_PGP_CONTACT);

                            if (pgpContactWithNoPublicKey != null) {
                                startActivityForResult(
                                        SelectContactsActivity.newIntent(getContext(),
                                                getString(R.string.use_public_key_from), false),
                                        REQUEST_CODE_COPY_PUBLIC_KEY_FROM_OTHER_CONTACT);
                            }
                        }

                        break;

                    case NoPgpFoundDialogFragment.RESULT_CODE_REMOVE_CONTACT:
                        if (data != null) {
                            PgpContact pgpContact = data.getParcelableExtra(NoPgpFoundDialogFragment
                                    .EXTRA_KEY_PGP_CONTACT);

                            if (pgpContact != null) {
                                removePgpContactFromRecipientsField(pgpContact);
                            }
                        }

                        break;
                }
                break;

            case REQUEST_CODE_SHOW_PGP_CONTACT_DIALOG:
                PgpContact receivedPgpContact = data != null ?
                        (PgpContact) data.getParcelableExtra(PgpContactDialogFragment.EXTRA_KEY_PGP_CONTACT) : null;

                switch (resultCode) {
                    case PgpContactDialogFragment.RESULT_CODE_COPY_EMAIL:
                        if (receivedPgpContact != null) {
                            ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(
                                    Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText(null, receivedPgpContact.getEmail());
                            if (clipboardManager != null) {
                                clipboardManager.setPrimaryClip(clip);
                            }
                        }
                        break;

                    case PgpContactDialogFragment.RESULT_CODE_REMOVE_CONTACT:
                        if (receivedPgpContact != null) {
                            removePgpContactFromRecipientsField(receivedPgpContact);
                        }
                        break;
                }
                break;

            case REQUEST_CODE_IMPORT_PUBLIC_KEY:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Toast.makeText(getContext(), R.string.key_successfully_imported, Toast.LENGTH_SHORT).show();

                        getLoaderManager().restartLoader(R.id.loader_id_update_info_about_pgp_contacts, null,
                                CreateMessageFragment.this);
                        break;
                }
                break;

            case REQUEST_CODE_COPY_PUBLIC_KEY_FROM_OTHER_CONTACT:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (data != null) {
                            PgpContact pgpContact = data.getParcelableExtra(SelectContactsActivity
                                    .KEY_EXTRA_PGP_CONTACT);

                            if (pgpContact != null) {
                                pgpContactWithNoPublicKey.setPubkey(pgpContact.getPubkey());
                                new ContactsDaoSource().updatePgpContact(getContext(), pgpContactWithNoPublicKey);

                                Toast.makeText(getContext(), R.string.key_successfully_copied, Toast.LENGTH_LONG)
                                        .show();
                                getLoaderManager().restartLoader(R.id.loader_id_update_info_about_pgp_contacts, null,
                                        CreateMessageFragment.this);
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
                            AttachmentInfo attachmentInfo = EmailUtil.getAttachmentInfoFromUri(getContext(),
                                    data.getData());
                            if (isAttachmentCanBeAdded(attachmentInfo)) {
                                attachmentInfoList.add(attachmentInfo);
                                showAttachments();
                            } else {
                                showInfoSnackbar(getView(),
                                        getString(R.string.template_warning_max_total_attachments_size,
                                                FileUtils.byteCountToDisplaySize(
                                                        Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES)),
                                        Snackbar.LENGTH_LONG);
                            }
                        } else {
                            showInfoSnackbar(getView(), getString(R.string.can_not_attach_this_file),
                                    Snackbar.LENGTH_LONG);
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
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.setGroupVisible(0, !isMessageSendingNow);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuActionSend:
                if (getSnackBar() != null) {
                    getSnackBar().dismiss();
                }

                if (isUpdatedInfoAboutContactCompleted) {
                    UIUtil.hideSoftInput(getContext(), getView());
                    if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
                        if (isAllInformationCorrect()) {
                            sendMessage();
                        }
                    } else {
                        UIUtil.showInfoSnackbar(getView(), getString(R.string
                                .internet_connection_is_not_available));
                    }
                } else {
                    Toast.makeText(getContext(), R.string.please_wait_while_information_about_contacts_will_be_updated,
                            Toast.LENGTH_SHORT).show();
                }
                return true;

            case R.id.menuActionAttachFile:
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_attachment)),
                        REQUEST_CODE_GET_CONTENT_FOR_SENDING);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendMessage();
                } else {
                    Toast.makeText(getActivity(), R.string.cannot_send_attachment_without_read_permission,
                            Toast.LENGTH_LONG).show();
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_update_info_about_pgp_contacts:
                pgpContacts.clear();
                progressBarCheckContactsDetails.setVisibility(View.VISIBLE);
                isUpdatedInfoAboutContactCompleted = false;
                List<String> emails = selectOnlyValidEmails(editTextRecipients.getChipAndTokenValues());
                return new UpdateInfoAboutPgpContactsAsyncTaskLoader(getContext(), emails);

            case R.id.loader_id_load_email_aliases:
                return new LoadGmailAliasesLoader(getContext(), accountDao);

            default:
                return super.onCreateLoader(id, args);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_update_info_about_pgp_contacts:
                UpdateInfoAboutPgpContactsResult updateInfoAboutPgpContactsResult
                        = (UpdateInfoAboutPgpContactsResult) result;

                isUpdatedInfoAboutContactCompleted = true;
                progressBarCheckContactsDetails.setVisibility(View.INVISIBLE);

                if (updateInfoAboutPgpContactsResult != null
                        && updateInfoAboutPgpContactsResult.getUpdatedPgpContacts() != null) {
                    pgpContacts = updateInfoAboutPgpContactsResult.getUpdatedPgpContacts();
                }

                if (updateInfoAboutPgpContactsResult == null || !updateInfoAboutPgpContactsResult.isAllInfoReceived()) {
                    Toast.makeText(getContext(), R.string.info_about_some_contacts_not_received,
                            Toast.LENGTH_SHORT).show();
                }

                if (!pgpContacts.isEmpty()) {
                    updateChips();
                }
                break;

            case R.id.loader_id_load_email_aliases:
                List<AccountAliasesDao> accountAliasesDaoList = (List<AccountAliasesDao>) result;
                List<String> aliases = new ArrayList<>();
                aliases.add(accountDao.getEmail());

                for (AccountAliasesDao accountAliasesDao : accountAliasesDaoList) {
                    aliases.add(accountAliasesDao.getSendAsEmail());
                }

                fromAddressesArrayAdapter.clear();
                fromAddressesArrayAdapter.addAll(aliases);

                prepareAliasForReplyIfNeed(aliases);

                if (fromAddressesArrayAdapter.getCount() > 1) {
                    editTextFrom.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.mipmap.ic_arrow_drop_down_grey, 0);
                } else {
                    editTextFrom.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                }

                new AccountAliasesDaoSource().updateAliases(getContext(), accountDao, accountAliasesDaoList);
                break;

            default:
                super.handleSuccessLoaderResult(loaderId, result);
        }
    }

    @Override
    public void handleFailureLoaderResult(int loaderId, Exception e) {
        super.handleFailureLoaderResult(loaderId, e);
        switch (loaderId) {
            case R.id.loader_id_update_info_about_pgp_contacts:
                isUpdatedInfoAboutContactCompleted = true;
                progressBarCheckContactsDetails.setVisibility(View.INVISIBLE);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult> loader) {
        super.onLoaderReset(loader);
    }

    @Override
    public void onErrorOccurred(int requestCode, int errorType, Exception e) {
        notifyUserAboutErrorWhenSendMessage();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        switch (v.getId()) {
            case R.id.editTextRecipient:
                if (onChangeMessageEncryptedTypeListener.getMessageEncryptionType()
                        == MessageEncryptionType.ENCRYPTED) {
                    progressBarCheckContactsDetails.setVisibility(hasFocus ? View.INVISIBLE : View.VISIBLE);
                    if (hasFocus) {
                        pgpContacts.clear();
                        if (isAdded()) {
                            getLoaderManager().destroyLoader(R.id.loader_id_update_info_about_pgp_contacts);
                        }
                    } else {
                        if (isUpdateInfoAboutContactsEnable) {
                            if (isAdded()) {
                                getLoaderManager().restartLoader(R.id.loader_id_update_info_about_pgp_contacts, null,
                                        this);
                            }
                        } else {
                            progressBarCheckContactsDetails.setVisibility(View.INVISIBLE);
                        }
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
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.editTextFrom:
                if (fromAddressesArrayAdapter.getCount() > 1) {
                    spinnerFrom.performClick();
                }
                break;
        }
    }

    @Override
    public void onChipLongClick(@NonNull Chip chip, MotionEvent event) {
        PgpContactDialogFragment pgpContactDialogFragment = PgpContactDialogFragment.newInstance(
                new PgpContact(chip.getText().toString(), null));

        pgpContactDialogFragment.setTargetFragment(this, REQUEST_CODE_SHOW_PGP_CONTACT_DIALOG);
        pgpContactDialogFragment.show(getFragmentManager(), NoPgpFoundDialogFragment.class.getSimpleName());
    }

    public void onMessageEncryptionTypeChange(MessageEncryptionType messageEncryptionType) {
        String emailMassageHint = null;
        if (messageEncryptionType != null) {
            switch (messageEncryptionType) {
                case ENCRYPTED:
                    emailMassageHint = getString(R.string.prompt_compose_security_email);
                    editTextRecipients.getOnFocusChangeListener().onFocusChange(editTextRecipients, false);
                    break;

                case STANDARD:
                    emailMassageHint = getString(R.string.prompt_compose_standard_email);
                    pgpContacts.clear();
                    getLoaderManager().destroyLoader(R.id.loader_id_update_info_about_pgp_contacts);
                    break;
            }
        }
        textInputLayoutEmailMessage.setHint(emailMassageHint);
    }

    /**
     * Notify the user about an error which occurred when we send a message.
     */
    public void notifyUserAboutErrorWhenSendMessage() {
        isMessageSendingNow = false;
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
        UIUtil.exchangeViewVisibility(getContext(), false, progressView, getContentView());
        showInfoSnackbar(getView(), getString(R.string.error_occurred_while_sending_message));
    }

    /**
     * Check the message sending status
     *
     * @return true if message was sent, false otherwise.
     */
    public boolean isMessageSendingNow() {
        return isMessageSendingNow;
    }

    /**
     * Parse an incoming information from the intent which has next actions:
     * <ul>
     * <li>{@link Intent#ACTION_VIEW}</li>
     * <li>{@link Intent#ACTION_SENDTO}</li>
     * <li>{@link Intent#ACTION_SEND}</li>
     * <li>{@link Intent#ACTION_SEND_MULTIPLE}</li>
     * </ul>
     *
     * @param intent An incoming intent.
     */
    private void parseExtraActionInfo(Intent intent) {
        //parse mailto: URI
        if (Intent.ACTION_VIEW.equals(intent.getAction()) || Intent.ACTION_SENDTO.equals(intent.getAction())) {
            if (intent.getData() != null) {
                Uri uri = intent.getData();
                if (RFC6068Parser.isMailTo(uri)) {
                    extraActionInfo = RFC6068Parser.parse(uri);
                }
            }
        }

        if (extraActionInfo == null) {
            extraActionInfo = new ExtraActionInfo();
        }

        switch (intent.getAction()) {
            case Intent.ACTION_VIEW:
            case Intent.ACTION_SENDTO:
            case Intent.ACTION_SEND:
            case Intent.ACTION_SEND_MULTIPLE:

                CharSequence extraText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
                // Only use EXTRA_TEXT if the body hasn't already been set by the mailto: URI
                if (extraText != null && TextUtils.isEmpty(extraActionInfo.getBody())) {
                    extraActionInfo.setBody(extraText.toString());
                }

                String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                // Only use EXTRA_SUBJECT if the subject hasn't already been set by the mailto: URI
                if (subject != null && TextUtils.isEmpty(extraActionInfo.getSubject())) {
                    extraActionInfo.setSubject(subject);
                }

                List<AttachmentInfo> attachmentInfoList = new ArrayList<>();
                String maxTotalAttachmentSizeWarning = getString(R.string.template_warning_max_total_attachments_size,
                        FileUtils.byteCountToDisplaySize(Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES));
                if (Intent.ACTION_SEND.equals(intent.getAction())) {
                    Uri stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    if (stream != null) {
                        AttachmentInfo attachmentInfo =
                                EmailUtil.getAttachmentInfoFromUri(getContext(), stream);
                        if (isAttachmentCanBeAdded(attachmentInfo)) {
                            attachmentInfoList.add(attachmentInfo);
                            this.attachmentInfoList.add(attachmentInfo);
                        } else {
                            Toast.makeText(getContext(), maxTotalAttachmentSizeWarning, Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    List<Parcelable> list = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                    if (list != null) {
                        for (Parcelable parcelable : list) {
                            Uri stream = (Uri) parcelable;
                            if (stream != null) {
                                AttachmentInfo attachmentInfo =
                                        EmailUtil.getAttachmentInfoFromUri(getContext(), stream);
                                if (isAttachmentCanBeAdded(attachmentInfo)) {
                                    attachmentInfoList.add(attachmentInfo);
                                    this.attachmentInfoList.add(attachmentInfo);
                                } else {
                                    Toast.makeText(getContext(), maxTotalAttachmentSizeWarning,
                                            Toast.LENGTH_SHORT).show();
                                    break;
                                }
                            }
                        }
                    }
                }

                extraActionInfo.setAttachmentInfoList(attachmentInfoList);
                break;
        }
    }

    /**
     * Prepare an alias for the reply. Will be used the email address that the email was received. Will be used the
     * first found matched email.
     *
     * @param aliases A list of Gmail aliases.
     */
    private void prepareAliasForReplyIfNeed(List<String> aliases) {
        if (incomingMessageInfo != null) {
            ArrayList<String> toAddresses = incomingMessageInfo.getTo();
            if (toAddresses != null) {
                String firstFoundedAlias = null;
                for (String toAddress : toAddresses) {
                    if (firstFoundedAlias == null) {
                        for (String alias : aliases) {
                            if (alias.equalsIgnoreCase(toAddress)) {
                                firstFoundedAlias = alias;
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                }

                if (firstFoundedAlias != null) {
                    int position = fromAddressesArrayAdapter.getPosition(firstFoundedAlias);
                    if (position != -1) {
                        spinnerFrom.setSelection(position);
                    }
                }
            }
        }
    }

    /**
     * Remove not valid emails from the recipients list.
     *
     * @param emails The input list of recipients.
     * @return The list of valid emails.
     */
    private List<String> selectOnlyValidEmails(List<String> emails) {
        List<String> validEmails = new ArrayList<>();
        for (String email : emails) {
            if (js.str_is_email_valid(email)) {
                validEmails.add(email);
            }
        }
        return validEmails;
    }

    /**
     * Generate an outgoing message info from entered information by user.
     *
     * @return <tt>OutgoingMessageInfo</tt> Return a created OutgoingMessageInfo object which
     * contains information about an outgoing message.
     */
    private OutgoingMessageInfo getOutgoingMessageInfo() {
        OutgoingMessageInfo outgoingMessageInfo = new OutgoingMessageInfo();
        outgoingMessageInfo.setMessage(editTextEmailMessage.getText().toString());
        outgoingMessageInfo.setSubject(editTextEmailSubject.getText().toString());

        List<PgpContact> pgpContacts = new ArrayList<>();
        if (incomingMessageInfo != null) {
            outgoingMessageInfo.setRawReplyMessage(
                    incomingMessageInfo.getOriginalRawMessageWithoutAttachments());

        }

        List<String> contacts = editTextRecipients.getChipValues();
        if (onChangeMessageEncryptedTypeListener.getMessageEncryptionType() == MessageEncryptionType.ENCRYPTED) {
            pgpContacts = contactsDaoSource.getPgpContactsListFromDatabase(getContext(), contacts);
        } else {
            for (String s : contacts) {
                pgpContacts.add(new PgpContact(s, null));
            }
        }

        outgoingMessageInfo.setToPgpContacts(pgpContacts.toArray(new PgpContact[0]));
        outgoingMessageInfo.setFromPgpContact(new PgpContact(editTextFrom.getText().toString(), null));

        return outgoingMessageInfo;
    }

    /**
     * Check that all recipients have PGP.
     *
     * @return true if all recipients have PGP, other wise false.
     */
    private boolean isAllRecipientsHavePGP(boolean isShowRemoveAction) {
        for (PgpContact pgpContact : pgpContacts) {
            if (!pgpContact.getHasPgp()) {
                showNoPgpFoundDialog(pgpContact, isShowRemoveAction);
                return false;
            }
        }

        return true;
    }

    /**
     * This method does update chips in the recipients field.
     */
    private void updateChips() {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(editTextRecipients.getText());

        PGPContactChipSpan[] pgpContactChipSpans = spannableStringBuilder.getSpans(0, editTextRecipients.length(),
                PGPContactChipSpan.class);

        if (pgpContactChipSpans.length > 0) {
            for (PgpContact pgpContact : pgpContacts) {
                for (PGPContactChipSpan pgpContactChipSpan : pgpContactChipSpans) {
                    if (pgpContact.getEmail().equalsIgnoreCase(pgpContactChipSpan.getText().toString())) {
                        pgpContactChipSpan.setHasPgp(pgpContact.getHasPgp());
                        break;
                    }
                }
            }
            editTextRecipients.invalidateChips();
        }
    }

    private void initChipsView(View view) {
        editTextRecipients = view.findViewById(R.id.editTextRecipient);
        editTextRecipients.setNachoValidator(new ChipifyingNachoValidator());
        editTextRecipients.setIllegalCharacters(',');
        editTextRecipients.addChipTerminator(' ', ChipTerminatorHandler.BEHAVIOR_CHIPIFY_TO_TERMINATOR);
        editTextRecipients.setChipTokenizer(new SingleCharacterSpanChipTokenizer(getContext(),
                new CustomChipSpanChipCreator(getContext()), PGPContactChipSpan.class,
                SingleCharacterSpanChipTokenizer.CHIP_SEPARATOR_WHITESPACE));
        editTextRecipients.setAdapter(preparePgpContactAdapter());
        editTextRecipients.setOnFocusChangeListener(this);
        editTextRecipients.setOnChipLongClickListener(this);
    }

    /**
     * Do a lot of checks to validate an outgoing message info.
     *
     * @return <tt>Boolean</tt> true if all information is correct, false otherwise.
     */
    private boolean isAllInformationCorrect() {
        if (TextUtils.isEmpty(editTextRecipients.getText().toString())) {
            showInfoSnackbar(editTextRecipients, getString(R.string.text_must_not_be_empty,
                    getString(R.string.prompt_recipient)));
            editTextRecipients.requestFocus();
        } else if (isEmailValid()) {
            if (TextUtils.isEmpty(editTextEmailSubject.getText().toString())) {
                showInfoSnackbar(editTextEmailSubject, getString(R.string.text_must_not_be_empty,
                        getString(R.string.prompt_subject)));
                editTextEmailSubject.requestFocus();
            } else if ((attachmentInfoList != null && attachmentInfoList.isEmpty())
                    && TextUtils.isEmpty(editTextEmailMessage.getText().toString())) {
                showInfoSnackbar(editTextEmailMessage, getString(R.string.sending_message_must_not_be_empty));
                editTextEmailMessage.requestFocus();
            } else if (onChangeMessageEncryptedTypeListener.getMessageEncryptionType() ==
                    MessageEncryptionType.ENCRYPTED) {
                if (pgpContacts.isEmpty()) {
                    showSnackbar(getView(),
                            getString(R.string.please_update_information_about_contacts),
                            getString(R.string.update), Snackbar.LENGTH_LONG,
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
                                        getLoaderManager().restartLoader(
                                                R.id.loader_id_update_info_about_pgp_contacts, null,
                                                CreateMessageFragment.this);
                                    } else {
                                        showInfoSnackbar(getView(), getString(R.string
                                                .internet_connection_is_not_available));
                                    }
                                }
                            });
                } else if (isAllRecipientsHavePGP(true)) {
                    return true;
                }
            } else if (!attachmentInfoList.isEmpty() && isMessageHasExternalStorageUriAttachments()) {
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_CODE_REQUEST_WRITE_EXTERNAL_STORAGE);
                    return false;
                } else {
                    return true;
                }
            } else {
                return true;
            }
        }

        return false;
    }

    private boolean isMessageHasExternalStorageUriAttachments() {
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
     * @param deleteCandidatePgpContact The {@link PgpContact} which will be removed.
     */
    private void removePgpContactFromRecipientsField(PgpContact deleteCandidatePgpContact) {
        ChipTokenizer chipTokenizer = editTextRecipients.getChipTokenizer();
        for (Chip chip : editTextRecipients.getAllChips()) {
            if (deleteCandidatePgpContact.getEmail().equalsIgnoreCase(chip.getText().toString())
                    && chipTokenizer != null) {
                chipTokenizer.deleteChip(chip, editTextRecipients.getText());
            }

        }

        for (Iterator<PgpContact> iterator = pgpContacts.iterator(); iterator.hasNext(); ) {
            PgpContact pgpContact = iterator.next();
            if (deleteCandidatePgpContact.getEmail().equalsIgnoreCase(pgpContact.getEmail())) {
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
        layoutAttachments = view.findViewById(R.id.layoutAttachments);
        initChipsView(view);

        spinnerFrom = view.findViewById(R.id.spinnerFrom);
        spinnerFrom.setOnItemSelectedListener(this);
        spinnerFrom.setAdapter(fromAddressesArrayAdapter);

        editTextFrom = view.findViewById(R.id.editTextFrom);
        editTextFrom.setOnClickListener(this);
        editTextEmailSubject = view.findViewById(R.id.editTextEmailSubject);
        editTextEmailMessage = view.findViewById(R.id.editTextEmailMessage);
        textInputLayoutEmailMessage = view.findViewById(R.id.textInputLayoutEmailMessage);

        layoutContent = view.findViewById(R.id.scrollView);
        progressBarCheckContactsDetails = view.findViewById(R.id.progressBarCheckContactsDetails);
    }

    /**
     * Update views on the screen. This method can be called when we need to update the current
     * screen.
     */
    private void updateViews() {
        onMessageEncryptionTypeChange(onChangeMessageEncryptedTypeListener.getMessageEncryptionType());

        if (extraActionInfo != null) {
            editTextRecipients.setText(prepareRecipients(extraActionInfo.getToAddresses()));
            editTextRecipients.chipifyAllUnterminatedTokens();
            editTextRecipients.getOnFocusChangeListener().onFocusChange(editTextRecipients, false);
            editTextEmailSubject.setText(extraActionInfo.getSubject());
            editTextEmailMessage.setText(extraActionInfo.getBody());
            editTextEmailMessage.requestFocus();
        } else {
            if (incomingMessageInfo != null) {
                if (FoldersManager.FolderType.SENT == folderType) {
                    editTextRecipients.setText(prepareRecipients(incomingMessageInfo.getTo()));
                } else {
                    editTextRecipients.setText(prepareRecipients(incomingMessageInfo.getFrom()));
                }
                editTextRecipients.chipifyAllUnterminatedTokens();
                editTextEmailSubject.setText(prepareReplySubject(incomingMessageInfo.getSubject()));
                editTextEmailMessage.requestFocus();
            }

            if (serviceInfo != null) {
                editTextRecipients.setFocusable(serviceInfo.isToFieldEditEnable());
                editTextRecipients.setFocusableInTouchMode(serviceInfo.isToFieldEditEnable());

                editTextFrom.setFocusable(serviceInfo.isFromFieldEditEnable());
                editTextFrom.setFocusableInTouchMode(serviceInfo.isFromFieldEditEnable());
                if (!serviceInfo.isFromFieldEditEnable()) {
                    editTextFrom.setOnClickListener(null);
                }

                editTextEmailSubject.setFocusable(serviceInfo.isSubjectEditEnable());
                editTextEmailSubject.setFocusableInTouchMode(serviceInfo.isSubjectEditEnable());

                editTextEmailMessage.setFocusable(serviceInfo.isMessageEditEnable());
                editTextEmailMessage.setFocusableInTouchMode(serviceInfo.isMessageEditEnable());

                if (!TextUtils.isEmpty(serviceInfo.getSystemMessage())) {
                    editTextEmailMessage.setText(serviceInfo.getSystemMessage());
                }
            }
        }
    }

    @NonNull
    private String prepareReplySubject(String subject) {
        if (TextUtils.isEmpty(subject)) {
            return getString(R.string.template_reply_subject, "");
        }

        Pattern pattern = Pattern.compile("^(Re: )", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(subject);

        if (matcher.find()) {
            return subject;
        }

        return getString(R.string.template_reply_subject, subject);
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
                return getContext().getContentResolver().query(
                        new ContactsDaoSource().getBaseContentUri(),
                        null,
                        ContactsDaoSource.COL_EMAIL + " LIKE ?",
                        new String[]{"%" + constraint + "%"},
                        ContactsDaoSource.COL_EMAIL + " ASC");
            }
        });

        return pgpContactAdapter;
    }

    /**
     * Check is an email valid.
     *
     * @return <tt>boolean</tt> An email validation result.
     */
    private boolean isEmailValid() {
        List<String> emails = editTextRecipients.getChipAndTokenValues();
        for (String email : emails) {
            if (!js.str_is_email_valid(email)) {
                showInfoSnackbar(editTextRecipients, getString(R.string.error_some_email_is_not_valid, email));
                editTextRecipients.requestFocus();
                return false;
            }
        }
        return true;
    }

    /**
     * Check is attachment can be added to the current message.
     *
     * @param newAttachmentInfo The new attachment which will be maybe added.
     * @return true if the attachment can be added, otherwise false.
     */
    private boolean isAttachmentCanBeAdded(AttachmentInfo newAttachmentInfo) {
        int totalSizeOfAttachments = 0;

        for (AttachmentInfo attachmentInfo : attachmentInfoList) {
            totalSizeOfAttachments += attachmentInfo.getEncodedSize();
        }

        totalSizeOfAttachments += newAttachmentInfo.getEncodedSize();

        return totalSizeOfAttachments < Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES;
    }


    /**
     * Show a dialog where we can select different actions.
     *
     * @param pgpContact         The {@link PgpContact} which will be used when we select the
     *                           remove action.
     * @param isShowRemoveAction true if we want to show the remove action, false otherwise.
     */
    private void showNoPgpFoundDialog(PgpContact pgpContact, boolean isShowRemoveAction) {
        NoPgpFoundDialogFragment noPgpFoundDialogFragment =
                NoPgpFoundDialogFragment.newInstance(pgpContact, isShowRemoveAction);

        noPgpFoundDialogFragment.setTargetFragment(this, REQUEST_CODE_NO_PGP_FOUND_DIALOG);
        noPgpFoundDialogFragment.show(getFragmentManager(), NoPgpFoundDialogFragment.class.getSimpleName());
    }

    /**
     * Send a message.
     */
    private void sendMessage() {
        dismissCurrentSnackBar();

        isUpdateInfoAboutContactsEnable = false;
        isMessageSendingNow = true;

        getActivity().invalidateOptionsMenu();

        statusView.setVisibility(View.GONE);
        UIUtil.exchangeViewVisibility(getContext(), true, progressView, getContentView());

        OutgoingMessageInfo outgoingMessageInfo = getOutgoingMessageInfo();
        outgoingMessageInfo.setAttachmentInfoArrayList(attachmentInfoList);
        outgoingMessageInfo.setMessageEncryptionType(onChangeMessageEncryptedTypeListener.getMessageEncryptionType());

        if (onMessageSendListener != null) {
            onMessageSendListener.sendMessage(outgoingMessageInfo);
        }
    }

    /**
     * Show attachments which were added.
     */
    private void showAttachments() {
        if (!attachmentInfoList.isEmpty()) {
            layoutAttachments.removeAllViews();
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            for (final AttachmentInfo attachmentInfo : attachmentInfoList) {
                final View rootView = layoutInflater.inflate(R.layout.attachment_item, layoutAttachments, false);

                TextView textViewAttachmentName = rootView.findViewById(R.id.textViewAttchmentName);
                textViewAttachmentName.setText(attachmentInfo.getName());

                TextView textViewAttachmentSize = rootView.findViewById(R.id.textViewAttachmentSize);
                textViewAttachmentSize.setText(Formatter.formatFileSize(getContext(), attachmentInfo.getEncodedSize()));

                View imageButtonDownloadAttachment = rootView.findViewById(R.id.imageButtonDownloadAttachment);
                imageButtonDownloadAttachment.setVisibility(View.GONE);

                if (attachmentInfo.isCanBeDeleted()) {
                    View imageButtonClearAttachment = rootView.findViewById(R.id.imageButtonClearAttachment);
                    imageButtonClearAttachment.setVisibility(View.VISIBLE);
                    imageButtonClearAttachment.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            attachmentInfoList.remove(attachmentInfo);
                            layoutAttachments.removeView(rootView);
                        }
                    });
                }
                layoutAttachments.addView(rootView);
            }
        } else {
            layoutAttachments.removeAllViews();
        }
    }

    /**
     * This interface will be used when we send a message.
     */
    public interface OnMessageSendListener {
        void sendMessage(OutgoingMessageInfo outgoingMessageInfo);
    }
}
