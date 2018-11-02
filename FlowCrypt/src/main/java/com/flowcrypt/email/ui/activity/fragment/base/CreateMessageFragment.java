/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
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
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.JsForUiManager;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.flowcrypt.email.model.MessageType;
import com.flowcrypt.email.model.UpdateInfoAboutPgpContactsResult;
import com.flowcrypt.email.model.messages.MessagePart;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.CreateMessageActivity;
import com.flowcrypt.email.ui.activity.ImportPublicKeyForPgpContactActivity;
import com.flowcrypt.email.ui.activity.SelectContactsActivity;
import com.flowcrypt.email.ui.activity.fragment.dialog.NoPgpFoundDialogFragment;
import com.flowcrypt.email.ui.activity.listeners.OnChangeMessageEncryptedTypeListener;
import com.flowcrypt.email.ui.adapter.PgpContactAdapter;
import com.flowcrypt.email.ui.loader.LoadGmailAliasesLoader;
import com.flowcrypt.email.ui.loader.UpdateInfoAboutPgpContactsAsyncTaskLoader;
import com.flowcrypt.email.ui.widget.CustomChipSpanChipCreator;
import com.flowcrypt.email.ui.widget.PGPContactChipSpan;
import com.flowcrypt.email.ui.widget.PgpContactsNachoTextView;
import com.flowcrypt.email.ui.widget.SingleCharacterSpanChipTokenizer;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;
import com.flowcrypt.email.util.exception.FlowCryptException;
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

/**
 * This fragment describe a logic of sent an encrypted or standard message.
 *
 * @author DenBond7
 *         Date: 10.05.2017
 *         Time: 11:27
 *         E-mail: DenBond7@gmail.com
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
    private OnMessageSendListener onMessageSendListener;
    private OnChangeMessageEncryptedTypeListener onChangeMessageEncryptedTypeListener;
    private List<PgpContact> pgpContactsTo;
    private List<PgpContact> pgpContactsCc;
    private List<PgpContact> pgpContactsBcc;
    private ArrayList<AttachmentInfo> attachmentInfoList;
    private ContactsDaoSource contactsDaoSource;
    private FoldersManager.FolderType folderType;
    private IncomingMessageInfo incomingMessageInfo;
    private ServiceInfo serviceInfo;
    private AccountDao accountDao;
    private ArrayAdapter<String> fromAddressesArrayAdapter;
    private PgpContact pgpContactWithNoPublicKey;
    private ExtraActionInfo extraActionInfo;
    private MessageType messageType = MessageType.NEW;
    private File draftCacheDir;

    private ViewGroup layoutAttachments;
    private EditText editTextFrom;
    private Spinner spinnerFrom;
    private PgpContactsNachoTextView editTextRecipientsTo;
    private PgpContactsNachoTextView editTextRecipientsCc;
    private PgpContactsNachoTextView editTextRecipientsBcc;
    private EditText editTextEmailSubject;
    private EditText editTextEmailMessage;
    private TextInputLayout textInputLayoutEmailMessage;
    private ScrollView layoutContent;
    private View progressBarTo;
    private View progressBarCc;
    private View progressBarBcc;
    private View layoutCc;
    private View layoutBcc;
    private LinearLayout progressBarAndButtonLayout;
    private ImageButton imageButtonAliases;
    private View imageButtonAdditionalRecipientsVisibility;

    private boolean isUpdateInfoAboutContactsEnable = true;
    private boolean isUpdateInfoAboutToCompleted = true;
    private boolean isUpdateInfoAboutCcCompleted = true;
    private boolean isUpdateInfoAboutBccCompleted = true;
    private boolean isIncomingMessageInfoUsed;
    private boolean isMessageSentToQueue;

    public CreateMessageFragment() {
        pgpContactsTo = new ArrayList<>();
        pgpContactsCc = new ArrayList<>();
        pgpContactsBcc = new ArrayList<>();
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

        initDraftCacheDirectory();

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
            if (intent.hasExtra(CreateMessageActivity.EXTRA_KEY_MESSAGE_TYPE)) {
                this.messageType =
                        (MessageType) intent.getSerializableExtra(CreateMessageActivity.EXTRA_KEY_MESSAGE_TYPE);
            }

            if (!TextUtils.isEmpty(intent.getAction()) && intent.getAction().startsWith("android.intent.action")) {
                this.extraActionInfo = ExtraActionInfo.parseExtraActionInfo(getContext(), intent);

                if (isListHasExternalStorageUriAttachments(extraActionInfo.getAttachmentInfoList())) {
                    if (ContextCompat.checkSelfPermission(getContext(),
                            Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                REQUEST_CODE_REQUEST_READ_EXTERNAL_STORAGE_FOR_EXTRA_INFO);
                    } else {
                        addAttachmentsFromExtraActionInfo();
                    }
                } else {
                    addAttachmentsFromExtraActionInfo();
                }
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

        if ((incomingMessageInfo != null || extraActionInfo != null) && !isIncomingMessageInfoUsed) {
            this.isIncomingMessageInfoUsed = true;
            updateViews();
        }

        showAttachments();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (accountDao != null && AccountDao.ACCOUNT_TYPE_GOOGLE.equalsIgnoreCase(accountDao.getAccountType())) {
            getLoaderManager().restartLoader(R.id.loader_id_load_email_aliases, null, this);
        }

        if (incomingMessageInfo != null && GeneralUtil.isInternetConnectionAvailable(getContext())
                && onChangeMessageEncryptedTypeListener.getMessageEncryptionType() == MessageEncryptionType.ENCRYPTED) {
            updateRecipientsFields();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!isMessageSentToQueue) {
            for (AttachmentInfo attachmentInfo : attachmentInfoList) {
                if (attachmentInfo.getUri() != null && Constants.FILE_PROVIDER_AUTHORITY.equalsIgnoreCase(
                        attachmentInfo.getUri().getAuthority())) {
                    getContext().getContentResolver().delete(attachmentInfo.getUri(), null, null);
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
                        onChangeMessageEncryptedTypeListener.onMessageEncryptionTypeChange(MessageEncryptionType
                                .STANDARD);
                        break;

                    case NoPgpFoundDialogFragment.RESULT_CODE_IMPORT_THEIR_PUBLIC_KEY:
                        if (data != null) {
                            PgpContact pgpContact = data.getParcelableExtra(
                                    NoPgpFoundDialogFragment.EXTRA_KEY_PGP_CONTACT);

                            if (pgpContact != null) {
                                startActivityForResult(ImportPublicKeyForPgpContactActivity.newIntent(getContext(),
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
                                startActivityForResult(SelectContactsActivity.newIntent(getContext(),
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
                                removePgpContactFromRecipientsField(pgpContact, editTextRecipientsTo, pgpContactsTo);
                                removePgpContactFromRecipientsField(pgpContact, editTextRecipientsCc, pgpContactsCc);
                                removePgpContactFromRecipientsField(pgpContact, editTextRecipientsBcc, pgpContactsBcc);
                            }
                        }
                        break;
                }
                break;

            case REQUEST_CODE_IMPORT_PUBLIC_KEY:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Toast.makeText(getContext(), R.string.the_key_successfully_imported, Toast.LENGTH_SHORT).show();
                        updateRecipientsFields();
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

                                Toast.makeText(getContext(), R.string.key_successfully_copied,
                                        Toast.LENGTH_LONG).show();
                                updateRecipientsFields();
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuActionSend:
                if (getSnackBar() != null) {
                    getSnackBar().dismiss();
                }

                if (isUpdateInfoAboutToCompleted && isUpdateInfoAboutCcCompleted && isUpdateInfoAboutBccCompleted) {
                    UIUtil.hideSoftInput(getContext(), getView());
                    if (isAllInformationCorrect()) {
                        sendMessage();
                        this.isMessageSentToQueue = true;
                    }
                } else {
                    Toast.makeText(getContext(), R.string.please_wait_while_information_about_contacts_will_be_updated,
                            Toast.LENGTH_SHORT).show();
                }
                return true;

            case R.id.menuActionAttachFile:
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
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
            case REQUEST_CODE_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendMessage();
                } else {
                    Toast.makeText(getActivity(), R.string.cannot_send_attachment_without_read_permission,
                            Toast.LENGTH_LONG).show();
                }
                break;

            case REQUEST_CODE_REQUEST_READ_EXTERNAL_STORAGE_FOR_EXTRA_INFO:
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    addAttachmentsFromExtraActionInfo();
                    showAttachments();
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
                isUpdateInfoAboutToCompleted = false;
                return new UpdateInfoAboutPgpContactsAsyncTaskLoader(getContext(),
                        selectOnlyValidEmails(editTextRecipientsTo.getChipAndTokenValues()));

            case R.id.loader_id_update_info_about_pgp_contacts_cc:
                pgpContactsCc.clear();
                progressBarCc.setVisibility(View.VISIBLE);
                isUpdateInfoAboutCcCompleted = false;
                return new UpdateInfoAboutPgpContactsAsyncTaskLoader(getContext(),
                        selectOnlyValidEmails(editTextRecipientsCc.getChipAndTokenValues()));

            case R.id.loader_id_update_info_about_pgp_contacts_bcc:
                pgpContactsBcc.clear();
                progressBarBcc.setVisibility(View.VISIBLE);
                isUpdateInfoAboutBccCompleted = false;
                return new UpdateInfoAboutPgpContactsAsyncTaskLoader(getContext(),
                        selectOnlyValidEmails(editTextRecipientsBcc.getChipAndTokenValues()));

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
            case R.id.loader_id_update_info_about_pgp_contacts_to:
                isUpdateInfoAboutToCompleted = true;
                pgpContactsTo = getInfoAboutPgpContacts((UpdateInfoAboutPgpContactsResult) result,
                        progressBarTo, R.string.to);

                if (pgpContactsTo != null && !pgpContactsTo.isEmpty()) {
                    updateChips(editTextRecipientsTo, pgpContactsTo);
                }
                break;

            case R.id.loader_id_update_info_about_pgp_contacts_cc:
                isUpdateInfoAboutCcCompleted = true;
                pgpContactsCc = getInfoAboutPgpContacts((UpdateInfoAboutPgpContactsResult) result,
                        progressBarCc, R.string.cc);

                if (pgpContactsCc != null && !pgpContactsCc.isEmpty()) {
                    updateChips(editTextRecipientsCc, pgpContactsCc);
                }
                break;

            case R.id.loader_id_update_info_about_pgp_contacts_bcc:
                isUpdateInfoAboutBccCompleted = true;
                pgpContactsBcc = getInfoAboutPgpContacts((UpdateInfoAboutPgpContactsResult) result,
                        progressBarBcc, R.string.bcc);

                if (pgpContactsBcc != null && !pgpContactsBcc.isEmpty()) {
                    updateChips(editTextRecipientsBcc, pgpContactsBcc);
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

                if (fromAddressesArrayAdapter.getCount() == 1) {
                    if (imageButtonAliases.getVisibility() == View.VISIBLE) {
                        imageButtonAliases.setVisibility(View.INVISIBLE);
                    }
                } else {
                    if (serviceInfo == null || serviceInfo.isFromFieldEditEnable()) {
                        imageButtonAliases.setVisibility(View.VISIBLE);
                    } else {
                        imageButtonAliases.setVisibility(View.INVISIBLE);
                    }
                }

                new AccountAliasesDaoSource().updateAliases(getContext(), accountDao, accountAliasesDaoList);
                break;

            default:
                super.handleSuccessLoaderResult(loaderId, result);
        }
    }

    @Override
    public void handleFailureLoaderResult(int loaderId, Exception e) {
        switch (loaderId) {
            case R.id.loader_id_update_info_about_pgp_contacts_to:
                super.handleFailureLoaderResult(loaderId, e);
                isUpdateInfoAboutToCompleted = true;
                progressBarTo.setVisibility(View.INVISIBLE);
                break;

            case R.id.loader_id_update_info_about_pgp_contacts_cc:
                super.handleFailureLoaderResult(loaderId, e);
                isUpdateInfoAboutCcCompleted = true;
                progressBarCc.setVisibility(View.INVISIBLE);
                break;

            case R.id.loader_id_update_info_about_pgp_contacts_bcc:
                super.handleFailureLoaderResult(loaderId, e);
                isUpdateInfoAboutBccCompleted = true;
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
                    boolean isNeedToShowExpandButton = false;
                    if (TextUtils.isEmpty(editTextRecipientsCc.getText())) {
                        layoutCc.setVisibility(View.GONE);
                        isNeedToShowExpandButton = true;
                    }

                    if (TextUtils.isEmpty(editTextRecipientsBcc.getText())) {
                        layoutBcc.setVisibility(View.GONE);
                        isNeedToShowExpandButton = true;
                    }

                    if (isNeedToShowExpandButton) {
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
                if (fromAddressesArrayAdapter.getCount() > 1) {
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
                editTextRecipientsCc.requestFocus();
                break;
        }
    }

    @Override
    public void onChipLongClick(NachoTextView nachoTextView, @NonNull Chip chip, MotionEvent event) {
    }

    public void onMessageEncryptionTypeChange(MessageEncryptionType messageEncryptionType) {
        String emailMassageHint = null;
        if (messageEncryptionType != null) {
            switch (messageEncryptionType) {
                case ENCRYPTED:
                    emailMassageHint = getString(R.string.prompt_compose_security_email);
                    editTextRecipientsTo.getOnFocusChangeListener().onFocusChange(editTextRecipientsTo, false);
                    editTextRecipientsCc.getOnFocusChangeListener().onFocusChange(editTextRecipientsCc, false);
                    editTextRecipientsBcc.getOnFocusChangeListener().onFocusChange(editTextRecipientsBcc, false);
                    break;

                case STANDARD:
                    emailMassageHint = getString(R.string.prompt_compose_standard_email);
                    pgpContactsTo.clear();
                    pgpContactsCc.clear();
                    pgpContactsBcc.clear();
                    isUpdateInfoAboutToCompleted = true;
                    isUpdateInfoAboutCcCompleted = true;
                    isUpdateInfoAboutBccCompleted = true;
                    break;
            }
        }
        textInputLayoutEmailMessage.setHint(emailMassageHint);
    }

    /**
     * Notify the user about an error which occurred when we send a message.
     *
     * @param e An occurred error.
     */
    public void notifyUserAboutErrorWhenSendMessage(Exception e) {
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
        UIUtil.exchangeViewVisibility(getContext(), false, progressView, getContentView());

        String errorMessage = getString(R.string.error_occurred_while_sending_message);

        if (e != null && e instanceof FlowCryptException) {
            errorMessage = e.getMessage();
        }

        showInfoSnackbar(getView(), errorMessage);
    }

    private void initDraftCacheDirectory() {
        draftCacheDir = new File(getContext().getCacheDir(), Constants.DRAFT_CACHE_DIR);

        if (!draftCacheDir.exists()) {
            if (!draftCacheDir.mkdir()) {
                Log.e(TAG, "Create cache directory " + draftCacheDir.getName() + " filed!");
            }
        }
    }

    private void addAttachmentsFromExtraActionInfo() {
        String maxTotalAttachmentSizeWarning = getString(R.string.template_warning_max_total_attachments_size,
                FileUtils.byteCountToDisplaySize(Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES));

        for (AttachmentInfo attachmentInfo : extraActionInfo.getAttachmentInfoList()) {
            if (isAttachmentCanBeAdded(attachmentInfo)) {
                File draftAttachment = new File(draftCacheDir, attachmentInfo.getName());

                try {
                    InputStream inputStream = getContext().getContentResolver()
                            .openInputStream(attachmentInfo.getUri());

                    if (inputStream != null) {
                        FileUtils.copyInputStreamToFile(inputStream, draftAttachment);
                        attachmentInfo.setUri(FileProvider.getUriForFile(getContext(),
                                Constants.FILE_PROVIDER_AUTHORITY, draftAttachment));
                        attachmentInfoList.add(attachmentInfo);
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    if (!draftAttachment.delete()) {
                        Log.e(TAG, "Delete " + draftAttachment.getName() + " filed!");
                    }
                }
            } else {
                Toast.makeText(getContext(), maxTotalAttachmentSizeWarning, Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

    private void updateRecipientsFields() {
        getLoaderManager().restartLoader(R.id.loader_id_update_info_about_pgp_contacts_to, null, this);

        if (layoutCc.getVisibility() == View.VISIBLE) {
            getLoaderManager().restartLoader(R.id.loader_id_update_info_about_pgp_contacts_cc, null, this);
        } else {
            editTextRecipientsCc.setText((CharSequence) null);
            pgpContactsCc.clear();
        }

        if (layoutBcc.getVisibility() == View.VISIBLE) {
            getLoaderManager().restartLoader(R.id.loader_id_update_info_about_pgp_contacts_bcc, null, this);
        } else {
            editTextRecipientsBcc.setText((CharSequence) null);
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
        if (onChangeMessageEncryptedTypeListener.getMessageEncryptionType() == MessageEncryptionType.ENCRYPTED) {
            progressBar.setVisibility(hasFocus ? View.INVISIBLE : View.VISIBLE);
            if (hasFocus) {
                pgpContacts.clear();
            } else {
                if (isUpdateInfoAboutContactsEnable) {
                    if (isAdded()) {
                        getLoaderManager().restartLoader(loaderId, null, this);
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
    private void prepareAliasForReplyIfNeed(List<String> aliases) {
        if (incomingMessageInfo != null) {
            ArrayList<String> toAddresses;
            if (folderType == FoldersManager.FolderType.SENT) {
                toAddresses = incomingMessageInfo.getFrom();
            } else {
                toAddresses = incomingMessageInfo.getTo();
            }

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
        if (incomingMessageInfo != null && !TextUtils.isEmpty(incomingMessageInfo.getHtmlMessage())) {
            //todo-denbond7 Need to think how forward HTML
        }
        outgoingMessageInfo.setMessage(editTextEmailMessage.getText().toString());
        outgoingMessageInfo.setSubject(editTextEmailSubject.getText().toString());

        List<PgpContact> pgpContactsTo = new ArrayList<>();
        List<PgpContact> pgpContactsCc = new ArrayList<>();
        List<PgpContact> pgpContactsBcc = new ArrayList<>();

        if (incomingMessageInfo != null) {
            outgoingMessageInfo.setRawReplyMessage(incomingMessageInfo.getOriginalRawMessageWithoutAttachments());
        }

        if (onChangeMessageEncryptedTypeListener.getMessageEncryptionType() == MessageEncryptionType.ENCRYPTED) {
            pgpContactsTo = contactsDaoSource.getPgpContactsListFromDatabase(getContext(),
                    editTextRecipientsTo.getChipValues());
            pgpContactsCc = contactsDaoSource.getPgpContactsListFromDatabase(getContext(),
                    editTextRecipientsCc.getChipValues());
            pgpContactsBcc = contactsDaoSource.getPgpContactsListFromDatabase(getContext(),
                    editTextRecipientsBcc.getChipValues());
        } else {
            for (String s : editTextRecipientsTo.getChipValues()) {
                pgpContactsTo.add(new PgpContact(s, null));
            }

            for (String s : editTextRecipientsCc.getChipValues()) {
                pgpContactsCc.add(new PgpContact(s, null));
            }

            for (String s : editTextRecipientsBcc.getChipValues()) {
                pgpContactsBcc.add(new PgpContact(s, null));
            }
        }

        outgoingMessageInfo.setToPgpContacts(pgpContactsTo.toArray(new PgpContact[0]));
        outgoingMessageInfo.setCcPgpContacts(pgpContactsCc.toArray(new PgpContact[0]));
        outgoingMessageInfo.setBccPgpContacts(pgpContactsBcc.toArray(new PgpContact[0]));
        outgoingMessageInfo.setFromPgpContact(new PgpContact(editTextFrom.getText().toString(), null));
        outgoingMessageInfo.setUid(EmailUtil.generateOutboxUID(getContext()));

        return outgoingMessageInfo;
    }

    /**
     * Check that all recipients have PGP.
     *
     * @return true if all recipients have PGP, other wise false.
     */
    private boolean isAllRecipientsHavePGP(boolean isShowRemoveAction, List<PgpContact> pgpContacts) {
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
     *
     * @param pgpContactsNachoTextView A view which contains input {@link PgpContact}(s).
     * @param pgpContacts              The input {@link PgpContact}(s)
     */
    private void updateChips(PgpContactsNachoTextView pgpContactsNachoTextView, List<PgpContact> pgpContacts) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(pgpContactsNachoTextView.getText());

        PGPContactChipSpan[] pgpContactChipSpans = spannableStringBuilder.getSpans(0, pgpContactsNachoTextView.length(),
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
            pgpContactsNachoTextView.invalidateChips();
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
        pgpContactsNachoTextView.setOnChipLongClickListener(this);
    }

    /**
     * Do a lot of checks to validate an outgoing message info.
     *
     * @return <tt>Boolean</tt> true if all information is correct, false otherwise.
     */
    private boolean isAllInformationCorrect() {
        editTextRecipientsTo.chipifyAllUnterminatedTokens();
        editTextRecipientsCc.chipifyAllUnterminatedTokens();
        editTextRecipientsBcc.chipifyAllUnterminatedTokens();

        if (TextUtils.isEmpty(editTextRecipientsTo.getText().toString())) {
            showInfoSnackbar(editTextRecipientsTo, getString(R.string.text_must_not_be_empty,
                    getString(R.string.prompt_recipients_to)));
            editTextRecipientsTo.requestFocus();
            return false;
        }

        if (isEmailValid(editTextRecipientsTo) && isEmailValid(editTextRecipientsCc)
                && isEmailValid(editTextRecipientsBcc)) {
            if (onChangeMessageEncryptedTypeListener.getMessageEncryptionType() == MessageEncryptionType.ENCRYPTED) {
                if (!TextUtils.isEmpty(editTextRecipientsTo.getText()) && pgpContactsTo.isEmpty()) {
                    showUpdateInfoAboutPgpContactsSnackBar(R.id.loader_id_update_info_about_pgp_contacts_to);
                    return false;
                }

                if (!TextUtils.isEmpty(editTextRecipientsCc.getText()) && pgpContactsCc.isEmpty()) {
                    showUpdateInfoAboutPgpContactsSnackBar(R.id.loader_id_update_info_about_pgp_contacts_cc);
                    return false;
                }

                if (!TextUtils.isEmpty(editTextRecipientsBcc.getText()) && pgpContactsBcc.isEmpty()) {
                    showUpdateInfoAboutPgpContactsSnackBar(R.id.loader_id_update_info_about_pgp_contacts_bcc);
                    return false;
                }

                if (!isAllRecipientsHavePGP(true, pgpContactsTo)
                        || !isAllRecipientsHavePGP(true, pgpContactsCc)
                        || !isAllRecipientsHavePGP(true, pgpContactsBcc)) {
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

        if ((attachmentInfoList != null && attachmentInfoList.isEmpty())
                && TextUtils.isEmpty(editTextEmailMessage.getText().toString())) {
            showInfoSnackbar(editTextEmailMessage, getString(R.string.sending_message_must_not_be_empty));
            editTextEmailMessage.requestFocus();
            return false;
        }

        if (attachmentInfoList != null && !attachmentInfoList.isEmpty()
                && isListHasExternalStorageUriAttachments(this.attachmentInfoList)) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
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

    private void showUpdateInfoAboutPgpContactsSnackBar(final int loaderId) {
        showSnackbar(getView(),
                getString(R.string.please_update_information_about_contacts),
                getString(R.string.update), Snackbar.LENGTH_LONG,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
                            getLoaderManager().restartLoader(loaderId, null, CreateMessageFragment.this);
                        } else {
                            showInfoSnackbar(getView(), getString(R.string.internet_connection_is_not_available));
                        }
                    }
                });
    }

    private boolean isListHasExternalStorageUriAttachments(List<AttachmentInfo> attachmentInfoList) {
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
     * @param pgpContactsNachoTextView  The {@link NachoTextView} which contains the delete candidate.
     * @param pgpContacts               The list which contains the delete candidate.
     */
    private void removePgpContactFromRecipientsField(PgpContact deleteCandidatePgpContact,
                                                     PgpContactsNachoTextView pgpContactsNachoTextView,
                                                     List<PgpContact> pgpContacts) {
        ChipTokenizer chipTokenizer = pgpContactsNachoTextView.getChipTokenizer();
        for (Chip chip : pgpContactsNachoTextView.getAllChips()) {
            if (deleteCandidatePgpContact.getEmail()
                    .equalsIgnoreCase(chip.getText().toString()) && chipTokenizer != null) {
                chipTokenizer.deleteChip(chip, pgpContactsNachoTextView.getText());
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
        layoutContent = view.findViewById(R.id.scrollView);
        layoutAttachments = view.findViewById(R.id.layoutAttachments);
        layoutCc = view.findViewById(R.id.layoutCc);
        layoutBcc = view.findViewById(R.id.layoutBcc);
        progressBarAndButtonLayout = view.findViewById(R.id.progressBarAndButtonLayout);

        editTextRecipientsTo = view.findViewById(R.id.editTextRecipientTo);
        editTextRecipientsCc = view.findViewById(R.id.editTextRecipientCc);
        editTextRecipientsBcc = view.findViewById(R.id.editTextRecipientBcc);

        initChipsView(editTextRecipientsTo);
        initChipsView(editTextRecipientsCc);
        initChipsView(editTextRecipientsBcc);

        spinnerFrom = view.findViewById(R.id.spinnerFrom);
        spinnerFrom.setOnItemSelectedListener(this);
        spinnerFrom.setAdapter(fromAddressesArrayAdapter);

        editTextFrom = view.findViewById(R.id.editTextFrom);

        imageButtonAliases = view.findViewById(R.id.imageButtonAliases);
        if (imageButtonAliases != null) {
            imageButtonAliases.setOnClickListener(this);
        }

        imageButtonAdditionalRecipientsVisibility =
                view.findViewById(R.id.imageButtonAdditionalRecipientsVisibility);
        if (imageButtonAdditionalRecipientsVisibility != null) {
            imageButtonAdditionalRecipientsVisibility.setOnClickListener(this);
        }

        editTextEmailSubject = view.findViewById(R.id.editTextEmailSubject);
        editTextEmailSubject.setOnFocusChangeListener(this);
        editTextEmailMessage = view.findViewById(R.id.editTextEmailMessage);
        editTextEmailMessage.setOnFocusChangeListener(this);
        textInputLayoutEmailMessage = view.findViewById(R.id.textInputLayoutEmailMessage);

        progressBarTo = view.findViewById(R.id.progressBarTo);
        progressBarCc = view.findViewById(R.id.progressBarCc);
        progressBarBcc = view.findViewById(R.id.progressBarBcc);
    }

    /**
     * Update views on the screen. This method can be called when we need to update the current
     * screen.
     */
    private void updateViews() {
        onMessageEncryptionTypeChange(onChangeMessageEncryptedTypeListener.getMessageEncryptionType());

        if (extraActionInfo != null) {
            updateViewsFromExtraActionInfo();
        } else {
            if (incomingMessageInfo != null) {
                updateViewsFromIncomingMessageInfo();
                editTextRecipientsTo.chipifyAllUnterminatedTokens();
                editTextRecipientsCc.chipifyAllUnterminatedTokens();
                editTextEmailSubject.setText(prepareReplySubject(incomingMessageInfo.getSubject()));
            }

            if (serviceInfo != null) {
                updateViewsFromServiceInfo();
            }
        }
    }

    private void updateViewsFromExtraActionInfo() {
        setupPgpFromExtraActionInfo(editTextRecipientsTo, extraActionInfo.getToAddresses());
        setupPgpFromExtraActionInfo(editTextRecipientsCc, extraActionInfo.getCcAddresses());
        setupPgpFromExtraActionInfo(editTextRecipientsBcc, extraActionInfo.getBccAddresses());

        editTextEmailSubject.setText(extraActionInfo.getSubject());
        editTextEmailMessage.setText(extraActionInfo.getBody());

        if (TextUtils.isEmpty(editTextRecipientsTo.getText())) {
            editTextRecipientsTo.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(editTextEmailSubject.getText())) {
            editTextEmailSubject.requestFocus();
            return;
        }

        editTextEmailMessage.requestFocus();
    }

    private void updateViewsFromServiceInfo() {
        editTextRecipientsTo.setFocusable(serviceInfo.isToFieldEditEnable());
        editTextRecipientsTo.setFocusableInTouchMode(serviceInfo.isToFieldEditEnable());
        //todo-denbond7 Need to add a similar option for editTextRecipientsCc and editTextRecipientsBcc

        editTextEmailSubject.setFocusable(serviceInfo.isSubjectEditEnable());
        editTextEmailSubject.setFocusableInTouchMode(serviceInfo.isSubjectEditEnable());

        editTextEmailMessage.setFocusable(serviceInfo.isMessageEditEnable());
        editTextEmailMessage.setFocusableInTouchMode(serviceInfo.isMessageEditEnable());

        if (!TextUtils.isEmpty(serviceInfo.getSystemMessage())) {
            editTextEmailMessage.setText(serviceInfo.getSystemMessage());
        }
    }

    private void updateViewsFromIncomingMessageInfo() {
        switch (messageType) {
            case REPLY:
                switch (folderType) {
                    case SENT:
                    case OUTBOX:
                        editTextRecipientsTo.setText(prepareRecipients(incomingMessageInfo.getTo()));
                        break;

                    default:
                        editTextRecipientsTo.setText(prepareRecipients(incomingMessageInfo.getFrom()));
                        break;
                }

                if (!TextUtils.isEmpty(editTextRecipientsTo.getText())) {
                    editTextEmailMessage.requestFocus();
                }
                break;

            case REPLY_ALL:
                if (folderType == FoldersManager.FolderType.SENT || folderType == FoldersManager.FolderType.OUTBOX) {
                    editTextRecipientsTo.setText(prepareRecipients(incomingMessageInfo.getTo()));

                    if (incomingMessageInfo.getCc() != null && !incomingMessageInfo.getCc().isEmpty()) {
                        layoutCc.setVisibility(View.VISIBLE);
                        editTextRecipientsCc.append(prepareRecipients(incomingMessageInfo.getCc()));
                    }
                } else {
                    editTextRecipientsTo.setText(prepareRecipients(incomingMessageInfo.getFrom()));

                    Set<String> ccSet = new HashSet<>();

                    if (incomingMessageInfo.getTo() != null && !incomingMessageInfo.getTo().isEmpty()) {
                        ArrayList<String> toRecipients = new ArrayList<>(incomingMessageInfo.getTo());
                        toRecipients.remove(accountDao.getEmail());

                        if (AccountDao.ACCOUNT_TYPE_GOOGLE.equalsIgnoreCase(accountDao.getAccountType())) {
                            List<AccountAliasesDao> accountAliasesDaoList =
                                    new AccountAliasesDaoSource().getAliases(getContext(), accountDao);
                            for (AccountAliasesDao accountAliasesDao : accountAliasesDaoList) {
                                toRecipients.remove(accountAliasesDao.getSendAsEmail());
                            }
                        }

                        ccSet.addAll(toRecipients);
                    }

                    if (incomingMessageInfo.getCc() != null) {
                        ArrayList<String> ccRecipients = incomingMessageInfo.getCc();
                        ccRecipients.remove(accountDao.getEmail());
                        ccSet.addAll(ccRecipients);
                    }

                    if (!ccSet.isEmpty()) {
                        layoutCc.setVisibility(View.VISIBLE);
                        editTextRecipientsCc.append(prepareRecipients(new ArrayList<>(ccSet)));
                    }
                }

                if (!TextUtils.isEmpty(editTextRecipientsTo.getText())
                        || !TextUtils.isEmpty(editTextRecipientsCc.getText())) {
                    editTextEmailMessage.requestFocus();
                }
                break;

            case FORWARD:
                if (incomingMessageInfo.getAttachmentInfoList() != null
                        && !incomingMessageInfo.getAttachmentInfoList().isEmpty()) {
                    for (AttachmentInfo attachmentInfo : incomingMessageInfo.getAttachmentInfoList()) {
                        if (isAttachmentCanBeAdded(attachmentInfo)) {
                            attachmentInfoList.add(attachmentInfo);
                        } else {
                            showInfoSnackbar(getView(),
                                    getString(R.string.template_warning_max_total_attachments_size,
                                            FileUtils.byteCountToDisplaySize(
                                                    Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES)),
                                    Snackbar.LENGTH_LONG);
                        }
                    }
                }

                editTextEmailMessage.setText(getString(R.string.forward_template,
                        incomingMessageInfo.getFrom().get(0),
                        EmailUtil.prepareDateForForwardedMessage(incomingMessageInfo.getReceiveDate()),
                        incomingMessageInfo.getSubject(),
                        prepareRecipientsLineForForwarding(incomingMessageInfo.getTo())));

                if (incomingMessageInfo.getCc() != null && !incomingMessageInfo.getCc().isEmpty()) {
                    editTextEmailMessage.append("Cc: ");
                    editTextEmailMessage.append(prepareRecipientsLineForForwarding(incomingMessageInfo
                            .getCc()));
                    editTextEmailMessage.append("\n\n");
                }

                if (incomingMessageInfo.getMessageParts() != null
                        && !incomingMessageInfo.getMessageParts().isEmpty()) {
                    for (MessagePart messagePart : incomingMessageInfo.getMessageParts()) {
                        if (messagePart != null) {
                            switch (messagePart.getMessagePartType()) {
                                case PGP_MESSAGE:
                                case TEXT:
                                    editTextEmailMessage.append("\n\n");
                                    editTextEmailMessage.append(messagePart.getValue());
                                    break;

                                case PGP_PUBLIC_KEY:
                                    //TODO-denbond7 add implementation of the public key view
                                    break;
                            }
                        }
                    }
                } else if (!incomingMessageInfo.isPlainTextExists()
                        && !TextUtils.isEmpty(incomingMessageInfo.getHtmlMessage())) {
                    Toast.makeText(getContext(), R.string.cannot_forward_html_emails,
                            Toast.LENGTH_LONG).show();
                }

                break;
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
    private boolean isEmailValid(PgpContactsNachoTextView pgpContactsNachoTextView) {
        List<String> emails = pgpContactsNachoTextView.getChipAndTokenValues();
        for (String email : emails) {
            if (!js.str_is_email_valid(email)) {
                showInfoSnackbar(pgpContactsNachoTextView, getString(R.string.error_some_email_is_not_valid, email));
                pgpContactsNachoTextView.requestFocus();
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
        OutgoingMessageInfo outgoingMessageInfo = getOutgoingMessageInfo();

        ArrayList<AttachmentInfo> forwardedAttachmentInfoList = getForwardedAttachments();
        ArrayList<AttachmentInfo> attachmentInfoList = new ArrayList<>(this.attachmentInfoList);
        attachmentInfoList.removeAll(forwardedAttachmentInfoList);

        outgoingMessageInfo.setAttachmentInfoArrayList(attachmentInfoList);
        outgoingMessageInfo.setForwardedAttachmentInfoList(forwardedAttachmentInfoList);
        outgoingMessageInfo.setMessageEncryptionType(onChangeMessageEncryptedTypeListener.getMessageEncryptionType());
        outgoingMessageInfo.setForwarded(messageType == MessageType.FORWARD);

        if (onMessageSendListener != null) {
            onMessageSendListener.sendMessage(outgoingMessageInfo);
        }
    }

    /**
     * Generate a forwarded attachments list.
     *
     * @return The generated list.
     */
    private ArrayList<AttachmentInfo> getForwardedAttachments() {
        ArrayList<AttachmentInfo> forwardedAttachmentInfoList = new ArrayList<>();

        for (AttachmentInfo attachmentInfo : attachmentInfoList) {
            if (attachmentInfo.getId() != null && attachmentInfo.isForwarded()) {
                forwardedAttachmentInfoList.add(attachmentInfo);
            }
        }

        return forwardedAttachmentInfoList;
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
                if (attachmentInfo.getEncodedSize() > 0) {
                    textViewAttachmentSize.setVisibility(View.VISIBLE);
                    textViewAttachmentSize.setText(Formatter.formatFileSize(getContext(), attachmentInfo
                            .getEncodedSize()));
                } else {
                    textViewAttachmentSize.setVisibility(View.GONE);
                }

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

                            //Remove a temp file which was created by our app
                            Uri uri = attachmentInfo.getUri();
                            if (uri != null && Constants.FILE_PROVIDER_AUTHORITY.equalsIgnoreCase(uri.getAuthority())) {
                                getContext().getContentResolver().delete(uri, null, null);
                            }
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
