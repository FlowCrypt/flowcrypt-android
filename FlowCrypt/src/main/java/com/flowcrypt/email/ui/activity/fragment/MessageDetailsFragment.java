/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.api.email.model.ServiceInfo;
import com.flowcrypt.email.api.email.sync.SyncErrorTypes;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.JsForUiManager;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.PgpKeyInfo;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.flowcrypt.email.model.MessageType;
import com.flowcrypt.email.model.messages.MessagePart;
import com.flowcrypt.email.model.messages.MessagePartPgpMessage;
import com.flowcrypt.email.model.messages.MessagePartPgpPublicKey;
import com.flowcrypt.email.service.JsBackgroundService;
import com.flowcrypt.email.service.attachment.AttachmentDownloadManagerService;
import com.flowcrypt.email.ui.activity.CreateMessageActivity;
import com.flowcrypt.email.ui.activity.ImportPrivateKeyActivity;
import com.flowcrypt.email.ui.activity.MessageDetailsActivity;
import com.flowcrypt.email.ui.activity.base.BaseSyncActivity;
import com.flowcrypt.email.ui.activity.fragment.base.BaseSyncFragment;
import com.flowcrypt.email.ui.activity.fragment.dialog.PrepareSendUserPublicKeyDialogFragment;
import com.flowcrypt.email.ui.widget.EmailWebView;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;
import com.google.android.gms.common.util.CollectionUtils;
import com.google.android.material.snackbar.Snackbar;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * This fragment describe details of some message.
 *
 * @author DenBond7
 * Date: 03.05.2017
 * Time: 16:29
 * E-mail: DenBond7@gmail.com
 */
public class MessageDetailsFragment extends BaseSyncFragment implements View.OnClickListener {
  private static final int REQUEST_CODE_REQUEST_WRITE_EXTERNAL_STORAGE = 100;
  private static final int REQUEST_CODE_START_IMPORT_KEY_ACTIVITY = 101;
  private static final int REQUEST_CODE_SHOW_DIALOG_WITH_SEND_KEY_OPTION = 102;

  private TextView textViewSenderAddress;
  private TextView textViewDate;
  private TextView textViewSubject;
  private View viewFooterOfHeader;
  private ViewGroup layoutMessageParts;
  private View layoutContent;
  private View imageButtonReplyAll;
  private View progressBarActionRunning;
  private View layoutMessageContainer;
  private View layoutReplyButtons;

  private java.text.DateFormat dateFormat;
  private IncomingMessageInfo incomingMessageInfo;
  private GeneralMessageDetails generalMessageDetails;
  private Folder folder;
  private FoldersManager.FolderType folderType;

  private boolean isAdditionalActionEnable;
  private boolean isDeleteActionEnable;
  private boolean isArchiveActionEnable;
  private boolean isMoveToInboxActionEnable;
  private OnActionListener onActionListener;
  private AttachmentInfo lastClickedAttachmentInfo;
  private MessageEncryptionType messageEncryptionType = MessageEncryptionType.STANDARD;
  private ArrayList<AttachmentInfo> attachmentInfoList;

  public MessageDetailsFragment() {
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (context instanceof BaseSyncActivity) {
      this.onActionListener = (OnActionListener) context;
    } else throw new IllegalArgumentException(context.toString() + " must implement " +
        OnActionListener.class.getSimpleName());
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);

    dateFormat = DateFormat.getTimeFormat(getContext());
    Intent activityIntent = getActivity().getIntent();

    if (activityIntent != null) {
      this.generalMessageDetails = activityIntent.getParcelableExtra(MessageDetailsActivity
          .EXTRA_KEY_GENERAL_MESSAGE_DETAILS);
      this.folder = activityIntent.getParcelableExtra(MessageDetailsActivity.EXTRA_KEY_FOLDER);
    }

    updateActionsVisibility(folder);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_message_details, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initViews(view);
    updateViews();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_CODE_START_IMPORT_KEY_ACTIVITY:
        switch (resultCode) {
          case Activity.RESULT_OK:
            getBaseActivity().restartJsService();
            Toast.makeText(getContext(), R.string.key_successfully_imported, Toast.LENGTH_SHORT).show();
            UIUtil.exchangeViewVisibility(getContext(), true, progressView, layoutMessageContainer);
            getBaseActivity().decryptMessage(R.id.js_decrypt_message,
                generalMessageDetails.getRawMessageWithoutAttachments());
            break;
        }
        break;

      case REQUEST_CODE_SHOW_DIALOG_WITH_SEND_KEY_OPTION:
        switch (resultCode) {
          case Activity.RESULT_OK:
            List<AttachmentInfo> attachmentInfoList;
            if (data != null) {
              attachmentInfoList = data.getParcelableArrayListExtra
                  (PrepareSendUserPublicKeyDialogFragment.KEY_ATTACHMENT_INFO_LIST);

              if (!CollectionUtils.isEmpty(attachmentInfoList)) {
                for (AttachmentInfo attachmentInfo : attachmentInfoList) {
                  attachmentInfo.setCanBeDeleted(false);
                }
                sendTemplateMessageWithPublicKey(attachmentInfoList.get(0));
              }
            }

            break;
        }
        break;

      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public View getContentView() {
    return layoutMessageContainer;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.fragment_message_details, menu);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);

    MenuItem menuItemArchiveMessage = menu.findItem(R.id.menuActionArchiveMessage);
    MenuItem menuItemDeleteMessage = menu.findItem(R.id.menuActionDeleteMessage);
    MenuItem menuActionMoveToInbox = menu.findItem(R.id.menuActionMoveToInbox);

    if (menuItemArchiveMessage != null) {
      menuItemArchiveMessage.setVisible(isArchiveActionEnable && isAdditionalActionEnable);
    }

    if (menuItemDeleteMessage != null) {
      menuItemDeleteMessage.setVisible(isDeleteActionEnable && isAdditionalActionEnable);
    }

    if (menuActionMoveToInbox != null) {
      menuActionMoveToInbox.setVisible(isMoveToInboxActionEnable && isAdditionalActionEnable);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menuActionArchiveMessage:
      case R.id.menuActionDeleteMessage:
      case R.id.menuActionMoveToInbox:
        runMessageAction(item.getItemId());
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.layoutReplyButton:
        startActivity(CreateMessageActivity.generateIntent(getContext(), incomingMessageInfo,
            MessageType.REPLY, messageEncryptionType));
        break;

      case R.id.imageButtonReplyAll:
      case R.id.layoutReplyAllButton:
        startActivity(CreateMessageActivity.generateIntent(getContext(), incomingMessageInfo,
            MessageType.REPLY_ALL, messageEncryptionType));
        break;

      case R.id.layoutForwardButton:
        if (messageEncryptionType == MessageEncryptionType.ENCRYPTED) {
          Toast.makeText(getContext(), R.string.cannot_forward_encrypted_attachments,
              Toast.LENGTH_LONG).show();
        } else {
          if (!CollectionUtils.isEmpty(attachmentInfoList)) {
            for (AttachmentInfo attachmentInfo : attachmentInfoList) {
              attachmentInfo.setForwarded(true);
            }
          }

          incomingMessageInfo.setAttachmentInfoList(attachmentInfoList);
        }
        startActivity(CreateMessageActivity.generateIntent(getContext(), incomingMessageInfo,
            MessageType.FORWARD, messageEncryptionType));
        break;
    }
  }

  @Override
  public void onErrorOccurred(final int requestCode, int errorType, Exception e) {
    super.onErrorOccurred(requestCode, errorType, e);
    isAdditionalActionEnable = true;
    UIUtil.exchangeViewVisibility(getContext(), false, progressBarActionRunning, layoutContent);
    if (getActivity() != null) {
      getActivity().invalidateOptionsMenu();
    }

    switch (requestCode) {
      case R.id.syns_request_code_load_message_details:
        switch (errorType) {
          case SyncErrorTypes.CONNECTION_TO_STORE_IS_LOST:
            showSnackbar(getView(), getString(R.string.failed_load_message_from_email_server),
                getString(R.string.retry), new View.OnClickListener() {
                  @Override
                  public void onClick(View v) {
                    UIUtil.exchangeViewVisibility(getContext(), true, progressView, statusView);
                    ((BaseSyncActivity) getBaseActivity()).loadMessageDetails(
                        R.id.syns_request_code_load_message_details, folder,
                        generalMessageDetails.getUid());
                  }
                });
            return;
        }
        break;

      case R.id.syns_request_archive_message:
      case R.id.syns_request_delete_message:
      case R.id.syns_request_move_message_to_inbox:
        UIUtil.exchangeViewVisibility(getContext(), false, statusView, layoutMessageContainer);
        showSnackbar(getView(), e.getMessage(),
            getString(R.string.retry), Snackbar.LENGTH_LONG, new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                switch (requestCode) {
                  case R.id.syns_request_archive_message:
                    runMessageAction(R.id.menuActionArchiveMessage);
                    break;

                  case R.id.syns_request_delete_message:
                    runMessageAction(R.id.menuActionDeleteMessage);
                    break;

                  case R.id.syns_request_move_message_to_inbox:
                    runMessageAction(R.id.menuActionMoveToInbox);
                    break;
                }
              }
            });
        break;
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    switch (requestCode) {
      case REQUEST_CODE_REQUEST_WRITE_EXTERNAL_STORAGE:
        if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          getContext().startService(AttachmentDownloadManagerService.newAttachmentDownloadIntent(
              getContext(), lastClickedAttachmentInfo));
        } else {
          Toast.makeText(getActivity(), R.string.cannot_save_attachment_without_permission,
              Toast.LENGTH_LONG).show();
        }
        break;

      default:
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  /**
   * Show an incoming message info.
   *
   * @param incomingMessageInfo An incoming message info which have received from {@link JsBackgroundService}
   */
  public void showIncomingMessageInfo(IncomingMessageInfo incomingMessageInfo) {
    this.incomingMessageInfo = incomingMessageInfo;
    imageButtonReplyAll.setVisibility(View.VISIBLE);
    isAdditionalActionEnable = true;
    if (getActivity() != null) {
      getActivity().invalidateOptionsMenu();
    }
    incomingMessageInfo.setFolder(folder);
    incomingMessageInfo.setUid(generalMessageDetails.getUid());
    updateMessageBody();
    UIUtil.exchangeViewVisibility(getContext(), false, progressView, layoutMessageContainer);
  }

  /**
   * Update message details.
   *
   * @param generalMessageDetails This object contains general message details.
   */
  public void updateMessageDetails(GeneralMessageDetails generalMessageDetails) {
    this.generalMessageDetails = generalMessageDetails;
  }

  public void notifyUserAboutActionError(int requestCode) {
    isAdditionalActionEnable = true;
    getActivity().invalidateOptionsMenu();

    UIUtil.exchangeViewVisibility(getContext(), false, progressBarActionRunning, layoutContent);

    switch (requestCode) {
      case R.id.syns_request_archive_message:
        UIUtil.showInfoSnackbar(getView(),
            getString(R.string.error_occurred_while_archiving_message));
        break;

      case R.id.syns_request_delete_message:
        UIUtil.showInfoSnackbar(getView(),
            getString(R.string.error_occurred_while_deleting_message));
        break;
    }
  }

  protected void updateMessageBody() {
    if (incomingMessageInfo != null) {
      updateMessageView();
      showAttachmentsIfTheyExist();
    }
  }

  /**
   * Get the matched {@link PgpKey}. If the sender email matched to the email from {@link PgpContact} which got
   * from the private key than we return a relevant public key.
   *
   * @return A matched {@link PgpKey} or null.
   */
  private PgpKey getMatchedPublicPgpKey() {
    Js js = JsForUiManager.getInstance(getContext()).getJs();
    PgpKeyInfo[] pgpKeyInfoArray = js.getStorageConnector().getAllPgpPrivateKeys();
    PgpKey matchedPgpKey = null;
    for (PgpKeyInfo pgpKeyInfo : pgpKeyInfoArray) {
      PgpKey pgpKey = js.crypto_key_read(pgpKeyInfo.getPrivate());
      if (pgpKey != null) {
        PgpContact primaryUserId = pgpKey.getPrimaryUserId();
        if (generalMessageDetails.getEmail().equalsIgnoreCase(primaryUserId.getEmail())) {
          matchedPgpKey = pgpKey;
        }
      }
    }

    return matchedPgpKey != null ? matchedPgpKey.toPublic() : null;
  }

  /**
   * Show a dialog where the user can select some public key which will be attached to a message.
   */
  private void showSendersPublicKeyDialog() {
    PrepareSendUserPublicKeyDialogFragment prepareSendUserPublicKeyDialogFragment
        = new PrepareSendUserPublicKeyDialogFragment();
    prepareSendUserPublicKeyDialogFragment.setTargetFragment(MessageDetailsFragment.this,
        REQUEST_CODE_SHOW_DIALOG_WITH_SEND_KEY_OPTION);
    prepareSendUserPublicKeyDialogFragment.show(getFragmentManager(),
        PrepareSendUserPublicKeyDialogFragment.class.getSimpleName());
  }

  /**
   * Send a template message with a sender public key.
   *
   * @param attachmentInfo An {@link AttachmentInfo} object which contains information about a sender public key.
   */
  private void sendTemplateMessageWithPublicKey(AttachmentInfo attachmentInfo) {
    List<AttachmentInfo> attachmentInfoList = null;
    if (attachmentInfo != null) {
      attachmentInfoList = new ArrayList<>();
      attachmentInfo.setCanBeDeleted(false);
      attachmentInfoList.add(attachmentInfo);
    }

    startActivity(CreateMessageActivity.generateIntent(getContext(),
        incomingMessageInfo, MessageType.REPLY, MessageEncryptionType.STANDARD, new ServiceInfo.Builder()
            .setIsFromFieldEditEnable(false)
            .setIsToFieldEditEnable(false)
            .setIsSubjectEditEnable(false)
            .setIsMessageTypeCanBeSwitched(false)
            .setIsAddNewAttachmentsEnable(false)
            .setSystemMessage(getString(R.string.message_was_encrypted_for_wrong_key))
            .setAttachmentInfoList(attachmentInfoList)
            .createServiceInfo()));
  }

  /**
   * Update actions visibility using {@link FoldersManager.FolderType}
   *
   * @param folder The folder where current message exists.
   */
  private void updateActionsVisibility(Folder folder) {
    folderType = FoldersManager.getFolderTypeForImapFolder(folder);

    if (folderType != null) {
      switch (folderType) {
        case INBOX:
          if (JavaEmailConstants.EMAIL_PROVIDER_GMAIL.equalsIgnoreCase(
              EmailUtil.getDomain(generalMessageDetails.getEmail()))) {
            isArchiveActionEnable = true;
          }
          isDeleteActionEnable = true;
          break;

        case SENT:
          isDeleteActionEnable = true;
          break;

        case TRASH:
          isMoveToInboxActionEnable = true;
          isDeleteActionEnable = false;
          break;

        case DRAFTS:
        case OUTBOX:
          isMoveToInboxActionEnable = false;
          isArchiveActionEnable = false;
          isDeleteActionEnable = true;
          break;

        default:
          isMoveToInboxActionEnable = true;
          isArchiveActionEnable = false;
          isDeleteActionEnable = true;
          break;
      }
    } else {
      isArchiveActionEnable = false;
      isMoveToInboxActionEnable = false;
      isDeleteActionEnable = true;
    }

    getActivity().invalidateOptionsMenu();
  }

  /**
   * Run the message action (archive/delete/move to inbox).
   *
   * @param menuId The action menu id.
   */
  private void runMessageAction(final int menuId) {
    if (GeneralUtil.isInternetConnectionAvailable(getContext())
        || JavaEmailConstants.FOLDER_OUTBOX.equalsIgnoreCase(generalMessageDetails.getLabel())) {
      if (!JavaEmailConstants.FOLDER_OUTBOX.equalsIgnoreCase(generalMessageDetails.getLabel())) {
        isAdditionalActionEnable = false;
        getActivity().invalidateOptionsMenu();
        statusView.setVisibility(View.GONE);
        UIUtil.exchangeViewVisibility(getContext(), true, progressBarActionRunning, layoutContent);
      }

      switch (menuId) {
        case R.id.menuActionArchiveMessage:
          onActionListener.onArchiveMessageClicked();
          break;

        case R.id.menuActionDeleteMessage:
          onActionListener.onDeleteMessageClicked();
          break;

        case R.id.menuActionMoveToInbox:
          onActionListener.onMoveMessageToInboxClicked();
          break;
      }
    } else {
      showSnackbar(getView(),
          getString(R.string.internet_connection_is_not_available),
          getString(R.string.retry), Snackbar.LENGTH_LONG, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              runMessageAction(menuId);
            }
          });
    }
  }

  private void initViews(View view) {
    textViewSenderAddress = view.findViewById(R.id.textViewSenderAddress);
    textViewDate = view.findViewById(R.id.textViewDate);
    textViewSubject = view.findViewById(R.id.textViewSubject);
    viewFooterOfHeader = view.findViewById(R.id.layoutFooterOfHeader);
    layoutMessageParts = view.findViewById(R.id.layoutMessageParts);
    layoutMessageContainer = view.findViewById(R.id.layoutMessageContainer);
    layoutReplyButtons = view.findViewById(R.id.layoutReplyButtons);
    progressBarActionRunning = view.findViewById(R.id.progressBarActionRunning);

    layoutContent = view.findViewById(R.id.layoutContent);
    imageButtonReplyAll = view.findViewById(R.id.imageButtonReplyAll);
    imageButtonReplyAll.setOnClickListener(this);
  }

  private void updateViews() {
    if (generalMessageDetails != null) {
      String subject = TextUtils.isEmpty(generalMessageDetails.getSubject()) ? getString(R.string.no_subject) :
          generalMessageDetails.getSubject();

      if (folderType == FoldersManager.FolderType.SENT) {
        textViewSenderAddress.setText(EmailUtil.getFirstAddressString(generalMessageDetails.getTo()));
      } else {
        textViewSenderAddress.setText(EmailUtil.getFirstAddressString(generalMessageDetails.getFrom()));
      }
      textViewSubject.setText(subject);
      if (JavaEmailConstants.FOLDER_OUTBOX.equalsIgnoreCase(generalMessageDetails.getLabel())) {
        textViewDate.setText(dateFormat.format(generalMessageDetails.getSentDateInMillisecond()));
      } else {
        textViewDate.setText(dateFormat.format(generalMessageDetails.getReceivedDateInMillisecond()));
      }
    }

    updateMessageBody();
  }

  private void showAttachmentsIfTheyExist() {
    if (generalMessageDetails != null && generalMessageDetails.isMessageHasAttachment()) {
      attachmentInfoList = new AttachmentDaoSource()
          .getAttachmentInfoList(getContext(), generalMessageDetails.getEmail(),
              generalMessageDetails.getLabel(), generalMessageDetails.getUid());
      LayoutInflater layoutInflater = LayoutInflater.from(getContext());

      for (final AttachmentInfo attachmentInfo : attachmentInfoList) {
        View rootView = layoutInflater.inflate(R.layout.attachment_item, layoutMessageParts, false);

        TextView textViewAttachmentName = rootView.findViewById(R.id.textViewAttchmentName);
        textViewAttachmentName.setText(attachmentInfo.getName());

        TextView textViewAttachmentSize = rootView.findViewById(R.id.textViewAttachmentSize);
        textViewAttachmentSize.setText(Formatter.formatFileSize(getContext(), attachmentInfo.getEncodedSize()));

        final View imageButtonDownloadAttachment = rootView.findViewById(R.id.imageButtonDownloadAttachment);
        imageButtonDownloadAttachment.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            lastClickedAttachmentInfo = attachmentInfo;
            lastClickedAttachmentInfo.setOrderNumber(GeneralUtil.genAttOrderId(getContext()));
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
              requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                  REQUEST_CODE_REQUEST_WRITE_EXTERNAL_STORAGE);
            } else {
              getContext().startService(AttachmentDownloadManagerService.newAttachmentDownloadIntent
                  (getContext(), lastClickedAttachmentInfo));
            }
          }
        });

        if (attachmentInfo.getUri() != null) {
          View layoutAttachment = rootView.findViewById(R.id.layoutAttachment);
          layoutAttachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              if (attachmentInfo.getUri().getLastPathSegment().endsWith(Constants.PGP_FILE_EXT)) {
                imageButtonDownloadAttachment.performClick();
              } else {
                Intent intentOpenFile = new Intent(Intent.ACTION_VIEW, attachmentInfo.getUri());
                intentOpenFile.setAction(Intent.ACTION_VIEW);
                intentOpenFile.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intentOpenFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (intentOpenFile.resolveActivity(getContext().getPackageManager()) != null) {
                  startActivity(intentOpenFile);
                }
              }
            }
          });
        }

        layoutMessageParts.addView(rootView);
      }
    }
  }

  private void updateMessageView() {
    layoutMessageParts.removeAllViews();
    if (!TextUtils.isEmpty(incomingMessageInfo.getHtmlMessage())) {
      EmailWebView emailWebView = new EmailWebView(getContext());
      emailWebView.configure();

      int margin = getResources().getDimensionPixelOffset(R.dimen.default_margin_content);
      LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      layoutParams.setMargins(margin, 0, margin, 0);
      emailWebView.setLayoutParams(layoutParams);

      emailWebView.loadDataWithBaseURL(null, EmailUtil.prepareViewportHtml(incomingMessageInfo.getHtmlMessage()),
          "text/html",
          StandardCharsets.UTF_8.displayName(), null);

      layoutMessageParts.addView(emailWebView);
      emailWebView.setOnPageFinishedListener(new EmailWebView.OnPageFinishedListener() {
        public void onPageFinished() {
          updateReplyButtons();
        }
      });
    } else if (incomingMessageInfo.getMessageParts() != null && !incomingMessageInfo.getMessageParts().isEmpty()) {
      boolean isFirstMessagePartIsText = true;
      for (MessagePart messagePart : incomingMessageInfo.getMessageParts()) {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        if (messagePart != null) {
          switch (messagePart.getMessagePartType()) {
            case PGP_MESSAGE:
              messageEncryptionType = MessageEncryptionType.ENCRYPTED;
              layoutMessageParts.addView(generatePgpMessagePart((MessagePartPgpMessage) messagePart,
                  layoutInflater));
              break;

            case TEXT:
              layoutMessageParts.addView(generateTextPart(messagePart, layoutInflater));
              if (isFirstMessagePartIsText) {
                viewFooterOfHeader.setVisibility(View.VISIBLE);
              }
              break;

            case PGP_PUBLIC_KEY:
              layoutMessageParts.addView(generatePublicKeyPart(
                  (MessagePartPgpPublicKey) messagePart, layoutInflater));
              break;

            default:
              layoutMessageParts.addView(generateMessagePart(messagePart, layoutInflater,
                  R.layout.message_part_other, layoutMessageParts));
              break;
          }
        }
        isFirstMessagePartIsText = false;
      }
      updateReplyButtons();
    } else {
      layoutMessageParts.removeAllViews();
      updateReplyButtons();
    }
  }

  /**
   * Update the reply buttons layout depending on the {@link MessageEncryptionType}
   */
  private void updateReplyButtons() {
    if (layoutReplyButtons != null) {
      ImageView imageViewReply = layoutReplyButtons.findViewById(R.id.imageViewReply);
      ImageView imageViewReplyAll = layoutReplyButtons.findViewById(R.id.imageViewReplyAll);
      ImageView imageViewForward = layoutReplyButtons.findViewById(R.id.imageViewForward);

      TextView textViewReply = layoutReplyButtons.findViewById(R.id.textViewReply);
      TextView textViewReplyAll = layoutReplyButtons.findViewById(R.id.textViewReplyAll);
      TextView textViewForward = layoutReplyButtons.findViewById(R.id.textViewForward);

      if (messageEncryptionType == MessageEncryptionType.ENCRYPTED) {
        imageViewReply.setImageResource(R.mipmap.ic_reply_green);
        imageViewReplyAll.setImageResource(R.mipmap.ic_reply_all_green);
        imageViewForward.setImageResource(R.mipmap.ic_forward_green);

        textViewReply.setText(R.string.reply_encrypted);
        textViewReplyAll.setText(R.string.reply_all_encrypted);
        textViewForward.setText(R.string.forward_encrypted);
      } else {
        imageViewReply.setImageResource(R.mipmap.ic_reply_red);
        imageViewReplyAll.setImageResource(R.mipmap.ic_reply_all_red);
        imageViewForward.setImageResource(R.mipmap.ic_forward_red);

        textViewReply.setText(R.string.reply);
        textViewReplyAll.setText(R.string.reply_all);
        textViewForward.setText(R.string.forward);
      }

      layoutReplyButtons.findViewById(R.id.layoutReplyButton).setOnClickListener(this);
      layoutReplyButtons.findViewById(R.id.layoutReplyAllButton).setOnClickListener(this);
      layoutReplyButtons.findViewById(R.id.layoutForwardButton).setOnClickListener(this);

      layoutReplyButtons.setVisibility(View.VISIBLE);
    }
  }

  /**
   * Generate the public key part. There we can see the public key details and save/update the
   * key owner information to the local database.
   *
   * @param messagePartPgpPublicKey The {@link MessagePartPgpPublicKey} object which contains
   *                                information about a public key and his owner.
   * @param layoutInflater          The {@link LayoutInflater} instance.
   * @return The generated view.
   */
  @NonNull
  private View generatePublicKeyPart(final MessagePartPgpPublicKey messagePartPgpPublicKey,
                                     LayoutInflater layoutInflater) {

    final ViewGroup messagePartPublicKeyView = (ViewGroup) layoutInflater.inflate(
        R.layout.message_part_public_key, layoutMessageParts, false);

    TextView textViewKeyOwnerTemplate = messagePartPublicKeyView.findViewById(R.id.textViewKeyOwnerTemplate);
    TextView textViewKeyWordsTemplate = messagePartPublicKeyView.findViewById(R.id.textViewKeyWordsTemplate);
    TextView textViewFingerprintTemplate = messagePartPublicKeyView.findViewById(R.id.textViewFingerprintTemplate);
    final TextView textViewPgpPublicKey = messagePartPublicKeyView.findViewById(R.id.textViewPgpPublicKey);
    Switch switchShowPublicKey = messagePartPublicKeyView.findViewById(R.id.switchShowPublicKey);

    switchShowPublicKey.setOnCheckedChangeListener(new CompoundButton
        .OnCheckedChangeListener() {

      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean
          isChecked) {
        TransitionManager.beginDelayedTransition(messagePartPublicKeyView);
        textViewPgpPublicKey.setVisibility(isChecked ? View.VISIBLE : View.GONE);

        buttonView.setText(isChecked ? R.string.hide_the_public_key :
            R.string.show_the_public_key);
      }
    });

    if (!TextUtils.isEmpty(messagePartPgpPublicKey.getKeyOwner())) {
      textViewKeyOwnerTemplate.setText(
          getString(R.string.template_message_part_public_key_owner,
              messagePartPgpPublicKey.getKeyOwner()));
    }

    UIUtil.setHtmlTextToTextView(getString(R.string.template_message_part_public_key_key_words,
        messagePartPgpPublicKey.getKeyWords()), textViewKeyWordsTemplate);

    UIUtil.setHtmlTextToTextView(
        getString(R.string.template_message_part_public_key_fingerprint,
            GeneralUtil.doSectionsInText(" ",
                messagePartPgpPublicKey.getFingerprint(), 4)),
        textViewFingerprintTemplate);

    textViewPgpPublicKey.setText(messagePartPgpPublicKey.getValue());

    if (messagePartPgpPublicKey.isPgpContactExists()) {
      if (messagePartPgpPublicKey.isPgpContactCanBeUpdated()) {
        initUpdateContactButton(messagePartPgpPublicKey, messagePartPublicKeyView);
      }
    } else {
      initSaveContactButton(messagePartPgpPublicKey, messagePartPublicKeyView);
    }

    return messagePartPublicKeyView;
  }

  /**
   * Init the save contact button. When we press this button a new contact will be saved to the
   * local database.
   *
   * @param messagePartPgpPublicKey  The {@link MessagePartPgpPublicKey} object which contains
   *                                 information about a public key and his owner.
   * @param messagePartPublicKeyView The public key view container.
   */
  private void initSaveContactButton(final MessagePartPgpPublicKey messagePartPgpPublicKey,
                                     View messagePartPublicKeyView) {
    Button buttonSaveContact = messagePartPublicKeyView.findViewById(R.id.buttonSaveContact);
    if (buttonSaveContact != null) {
      buttonSaveContact.setVisibility(View.VISIBLE);
      buttonSaveContact.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Uri uri = new ContactsDaoSource().addRow(getContext(),
              new PgpContact(messagePartPgpPublicKey.getKeyOwner(),
                  null,
                  messagePartPgpPublicKey.getValue(),
                  false,
                  null,
                  false,
                  messagePartPgpPublicKey.getFingerprint(),
                  messagePartPgpPublicKey.getLongId(),
                  messagePartPgpPublicKey.getKeyWords(), 0));
          if (uri != null) {
            Toast.makeText(getContext(),
                R.string.contact_successfully_saved, Toast.LENGTH_SHORT).show();
            v.setVisibility(View.GONE);
          } else {
            Toast.makeText(getContext(),
                R.string.error_occurred_while_saving_contact,
                Toast.LENGTH_SHORT).show();
          }
        }
      });
    }
  }

  /**
   * Init the update contact button. When we press this button the contact will be updated in the
   * local database.
   *
   * @param messagePartPgpPublicKey  The {@link MessagePartPgpPublicKey} object which contains
   *                                 information about a public key and his owner.
   * @param messagePartPublicKeyView The public key view container.
   */
  private void initUpdateContactButton(final MessagePartPgpPublicKey messagePartPgpPublicKey,
                                       View messagePartPublicKeyView) {
    Button buttonUpdateContact = messagePartPublicKeyView.findViewById(R.id.buttonUpdateContact);
    if (buttonUpdateContact != null) {
      buttonUpdateContact.setVisibility(View.VISIBLE);
      buttonUpdateContact.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          boolean isUpdated = new ContactsDaoSource().updatePgpContact
              (getContext(),
                  new PgpContact(messagePartPgpPublicKey.getKeyOwner(),
                      null,
                      messagePartPgpPublicKey.getValue(),
                      false,
                      null,
                      false,
                      messagePartPgpPublicKey.getFingerprint(),
                      messagePartPgpPublicKey.getLongId(),
                      messagePartPgpPublicKey.getKeyWords(), 0)) > 0;
          if (isUpdated) {
            Toast.makeText(getContext(),
                R.string.contact_successfully_updated,
                Toast.LENGTH_SHORT).show();
          } else {
            Toast.makeText(getContext(),
                R.string.error_occurred_while_updating_contact,
                Toast.LENGTH_SHORT).show();
          }
        }
      });
    }
  }

  @NonNull
  private TextView generateMessagePart(MessagePart messagePart, LayoutInflater layoutInflater,
                                       int message_part_other, ViewGroup layoutMessageParts) {
    TextView textViewMessagePartOther = (TextView) layoutInflater.inflate(
        message_part_other, layoutMessageParts, false);

    textViewMessagePartOther.setText(messagePart.getValue());
    return textViewMessagePartOther;
  }

  @NonNull
  private TextView generateTextPart(MessagePart messagePart, LayoutInflater layoutInflater) {
    return generateMessagePart(messagePart, layoutInflater, R.layout.message_part_text, layoutMessageParts);
  }

  @NonNull
  private View generatePgpMessagePart(MessagePartPgpMessage messagePartPgpMessage,
                                      LayoutInflater layoutInflater) {
    if (messagePartPgpMessage != null) {
      if (TextUtils.isEmpty(messagePartPgpMessage.getErrorMessage())) {
        return generateMessagePart(messagePartPgpMessage, layoutInflater, R.layout.message_part_pgp_message,
            layoutMessageParts);
      } else {
        switch (messagePartPgpMessage.getPgpMessageDecryptError()) {
          case FORMAT_ERROR:
            final ViewGroup formatErrorLayout = (ViewGroup) layoutInflater.inflate(
                R.layout.message_part_pgp_message_format_error, layoutMessageParts, false);
            TextView textViewFormatError = formatErrorLayout.findViewById(R.id.textViewFormatError);
            textViewFormatError.setText(messagePartPgpMessage.getErrorMessage());
            formatErrorLayout.addView(generateShowOriginalMessageLayout
                (messagePartPgpMessage.getValue(), layoutInflater, formatErrorLayout));
            return formatErrorLayout;

          case MISSING_PRIVATE_KEY:
            return generateMissingPrivateKeyLayout(messagePartPgpMessage, layoutInflater);

          default:
            ViewGroup viewGroup = (ViewGroup) layoutInflater.inflate(
                R.layout.message_part_pgp_message_error, layoutMessageParts, false);
            TextView textViewErrorMessage = viewGroup.findViewById(R.id.textViewErrorMessage);
            textViewErrorMessage.setText(messagePartPgpMessage.getErrorMessage());
            viewGroup.addView(generateShowOriginalMessageLayout
                (messagePartPgpMessage.getValue(), layoutInflater, viewGroup));

            return viewGroup;
        }
      }
    } else return new TextView(getContext());
  }

  /**
   * Generate a layout which describes the missing private keys situation.
   *
   * @param messagePartPgpMessage The {@link MessagePartPgpMessage} which contains info about an error.
   * @param layoutInflater        The {@link LayoutInflater} instance.
   * @return Generated layout.
   */
  @NonNull
  private View generateMissingPrivateKeyLayout(MessagePartPgpMessage messagePartPgpMessage,
                                               LayoutInflater layoutInflater) {
    ViewGroup missingPrivateKeyLayout = (ViewGroup) layoutInflater.inflate(
        R.layout.message_part_pgp_message_missing_private_key, layoutMessageParts, false);
    TextView textViewErrorMessage = missingPrivateKeyLayout.findViewById(R.id.textViewErrorMessage);
    textViewErrorMessage.setText(messagePartPgpMessage.getErrorMessage());

    Button buttonImportPrivateKey = missingPrivateKeyLayout.findViewById(R.id.buttonImportPrivateKey);
    buttonImportPrivateKey.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivityForResult(ImportPrivateKeyActivity.newIntent(
            getContext(), getString(R.string.import_private_key), true, ImportPrivateKeyActivity.class),
            REQUEST_CODE_START_IMPORT_KEY_ACTIVITY);
      }
    });

    Button buttonSendOwnPublicKey = missingPrivateKeyLayout.findViewById(R.id.buttonSendOwnPublicKey);
    buttonSendOwnPublicKey.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        PgpKey publicKey = getMatchedPublicPgpKey();
        if (publicKey == null) {
          showSendersPublicKeyDialog();
        } else {
          sendTemplateMessageWithPublicKey(EmailUtil.generateAttachmentInfoFromPublicKey(publicKey));
        }
      }
    });

    missingPrivateKeyLayout.addView(generateShowOriginalMessageLayout
        (messagePartPgpMessage.getValue(), layoutInflater, missingPrivateKeyLayout));
    return missingPrivateKeyLayout;
  }

  /**
   * Generate a layout with switch button which will be regulate visibility of original message info.
   *
   * @param originalPgpMessage The original pgp message info.
   * @param layoutInflater     The {@link LayoutInflater} instance.
   * @param rootView           The root view which will be used while we create a new layout using
   *                           {@link LayoutInflater}.
   * @return A generated layout.
   */
  @NonNull
  private ViewGroup generateShowOriginalMessageLayout(String originalPgpMessage, LayoutInflater layoutInflater,
                                                      final ViewGroup rootView) {
    ViewGroup showOriginalMessageLayout = (ViewGroup) layoutInflater.inflate(
        R.layout.pgp_show_original_message, rootView, false);
    final TextView textViewOriginalPgpMessage
        = showOriginalMessageLayout.findViewById(R.id.textViewOriginalPgpMessage);
    textViewOriginalPgpMessage.setText(originalPgpMessage);

    Switch switchShowOriginalMessage = showOriginalMessageLayout.findViewById(R.id
        .switchShowOriginalMessage);

    switchShowOriginalMessage.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        TransitionManager.beginDelayedTransition(rootView);
        textViewOriginalPgpMessage.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        buttonView.setText(isChecked ? R.string.hide_original_message : R.string.show_original_message);
      }
    });
    return showOriginalMessageLayout;
  }

  public interface OnActionListener {
    void onArchiveMessageClicked();

    void onDeleteMessageClicked();

    void onMoveMessageToInboxClicked();
  }
}
