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
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.LocalFolder;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.api.email.model.ServiceInfo;
import com.flowcrypt.email.api.email.sync.SyncErrorTypes;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.PgpKeyInfo;
import com.flowcrypt.email.js.UiJsManager;
import com.flowcrypt.email.js.core.Js;
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
  private ViewGroup layoutMsgParts;
  private View layoutContent;
  private View imageBtnReplyAll;
  private View progressBarActionRunning;
  private View layoutMsgContainer;
  private View layoutReplyBtns;

  private java.text.DateFormat dateFormat;
  private IncomingMessageInfo msgInfo;
  private GeneralMessageDetails details;
  private LocalFolder localFolder;
  private FoldersManager.FolderType folderType;

  private boolean isAdditionalActionEnabled;
  private boolean isDeleteActionEnabled;
  private boolean isArchiveActionEnabled;
  private boolean isMoveToInboxActionEnabled;
  private OnActionListener onActionListener;
  private AttachmentInfo lastClickedAtt;
  private MessageEncryptionType msgEncryptType = MessageEncryptionType.STANDARD;
  private ArrayList<AttachmentInfo> atts;

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
      this.details = activityIntent.getParcelableExtra(MessageDetailsActivity.EXTRA_KEY_GENERAL_MESSAGE_DETAILS);
      this.localFolder = activityIntent.getParcelableExtra(MessageDetailsActivity.EXTRA_KEY_FOLDER);
    }

    updateActionsVisibility(localFolder);
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
            UIUtil.exchangeViewVisibility(getContext(), true, progressView, layoutMsgContainer);
            getBaseActivity().decryptMsg(R.id.js_decrypt_message, details.getRawMsgWithoutAtts());
            break;
        }
        break;

      case REQUEST_CODE_SHOW_DIALOG_WITH_SEND_KEY_OPTION:
        switch (resultCode) {
          case Activity.RESULT_OK:
            List<AttachmentInfo> atts;
            if (data != null) {
              atts = data.getParcelableArrayListExtra(PrepareSendUserPublicKeyDialogFragment.KEY_ATTACHMENT_INFO_LIST);

              if (!CollectionUtils.isEmpty(atts)) {
                makeAttsProtected(atts);
                sendTemplateMsgWithPublicKey(atts.get(0));
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
    return layoutMsgContainer;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.fragment_message_details, menu);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);

    MenuItem menuItemArchiveMsg = menu.findItem(R.id.menuActionArchiveMessage);
    MenuItem menuItemDeleteMsg = menu.findItem(R.id.menuActionDeleteMessage);
    MenuItem menuActionMoveToInbox = menu.findItem(R.id.menuActionMoveToInbox);

    if (menuItemArchiveMsg != null) {
      menuItemArchiveMsg.setVisible(isArchiveActionEnabled && isAdditionalActionEnabled);
    }

    if (menuItemDeleteMsg != null) {
      menuItemDeleteMsg.setVisible(isDeleteActionEnabled && isAdditionalActionEnabled);
    }

    if (menuActionMoveToInbox != null) {
      menuActionMoveToInbox.setVisible(isMoveToInboxActionEnabled && isAdditionalActionEnabled);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menuActionArchiveMessage:
      case R.id.menuActionDeleteMessage:
      case R.id.menuActionMoveToInbox:
        runMsgAction(item.getItemId());
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.layoutReplyButton:
        startActivity(CreateMessageActivity.generateIntent(getContext(), msgInfo, MessageType.REPLY, msgEncryptType));
        break;

      case R.id.imageButtonReplyAll:
      case R.id.layoutReplyAllButton:
        startActivity(CreateMessageActivity.generateIntent(getContext(), msgInfo, MessageType.REPLY_ALL,
            msgEncryptType));
        break;

      case R.id.layoutForwardButton:
        if (msgEncryptType == MessageEncryptionType.ENCRYPTED) {
          Toast.makeText(getContext(), R.string.cannot_forward_encrypted_attachments,
              Toast.LENGTH_LONG).show();
        } else {
          if (!CollectionUtils.isEmpty(atts)) {
            for (AttachmentInfo att : atts) {
              att.setForwarded(true);
            }
          }

          msgInfo.setAtts(atts);
        }
        startActivity(CreateMessageActivity.generateIntent(getContext(), msgInfo, MessageType.FORWARD, msgEncryptType));
        break;
    }
  }

  @Override
  public void onErrorOccurred(final int requestCode, int errorType, Exception e) {
    super.onErrorOccurred(requestCode, errorType, e);
    isAdditionalActionEnabled = true;
    UIUtil.exchangeViewVisibility(getContext(), false, progressBarActionRunning, layoutContent);
    if (getActivity() != null) {
      getActivity().invalidateOptionsMenu();
    }

    switch (requestCode) {
      case R.id.syns_request_code_load_message_details:
        switch (errorType) {
          case SyncErrorTypes.CONNECTION_TO_STORE_IS_LOST:
            showConnLostHint();
            return;
        }
        break;

      case R.id.syns_request_archive_message:
      case R.id.syns_request_delete_message:
      case R.id.syns_request_move_message_to_inbox:
        UIUtil.exchangeViewVisibility(getContext(), false, statusView, layoutMsgContainer);
        showRetryActionHint(requestCode, e);
        break;
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    switch (requestCode) {
      case REQUEST_CODE_REQUEST_WRITE_EXTERNAL_STORAGE:
        if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          Intent intent = AttachmentDownloadManagerService.newIntent(getContext(), lastClickedAtt);
          getContext().startService(intent);
        } else {
          Toast.makeText(getActivity(), R.string.cannot_save_attachment_without_permission, Toast.LENGTH_LONG).show();
        }
        break;

      default:
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  /**
   * Show an incoming message info.
   *
   * @param msgInfo An incoming message info which have received from {@link JsBackgroundService}
   */
  public void showIncomingMsgInfo(IncomingMessageInfo msgInfo) {
    this.msgInfo = msgInfo;
    imageBtnReplyAll.setVisibility(View.VISIBLE);
    isAdditionalActionEnabled = true;
    if (getActivity() != null) {
      getActivity().invalidateOptionsMenu();
    }
    msgInfo.setLocalFolder(localFolder);
    msgInfo.setUid(details.getUid());
    updateMsgBody();
    UIUtil.exchangeViewVisibility(getContext(), false, progressView, layoutMsgContainer);
  }

  /**
   * Update message details.
   *
   * @param details This object contains general message details.
   */
  public void updateMsgDetails(GeneralMessageDetails details) {
    this.details = details;
  }

  public void notifyUserAboutActionError(int requestCode) {
    isAdditionalActionEnabled = true;
    getActivity().invalidateOptionsMenu();

    UIUtil.exchangeViewVisibility(getContext(), false, progressBarActionRunning, layoutContent);

    switch (requestCode) {
      case R.id.syns_request_archive_message:
        UIUtil.showInfoSnackbar(getView(), getString(R.string.error_occurred_while_archiving_message));
        break;

      case R.id.syns_request_delete_message:
        UIUtil.showInfoSnackbar(getView(), getString(R.string.error_occurred_while_deleting_message));
        break;
    }
  }

  public void updateAttInfos(ArrayList<AttachmentInfo> attInfoList) {
    this.atts = attInfoList;
    showAttsIfTheyExist();
  }

  protected void updateMsgBody() {
    if (msgInfo != null) {
      updateMsgView();
      showAttsIfTheyExist();
    }
  }

  private void showRetryActionHint(final int requestCode, Exception e) {
    showSnackbar(getView(), e.getMessage(), getString(R.string.retry), Snackbar.LENGTH_LONG,
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            switch (requestCode) {
              case R.id.syns_request_archive_message:
                runMsgAction(R.id.menuActionArchiveMessage);
                break;

              case R.id.syns_request_delete_message:
                runMsgAction(R.id.menuActionDeleteMessage);
                break;

              case R.id.syns_request_move_message_to_inbox:
                runMsgAction(R.id.menuActionMoveToInbox);
                break;
            }
          }
        });
  }

  private void showConnLostHint() {
    showSnackbar(getView(), getString(R.string.failed_load_message_from_email_server),
        getString(R.string.retry), new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            UIUtil.exchangeViewVisibility(getContext(), true, progressView, statusView);
            ((BaseSyncActivity) getBaseActivity()).loadMsgDetails(
                R.id.syns_request_code_load_message_details, localFolder,
                details.getUid());
          }
        });
  }

  private void makeAttsProtected(List<AttachmentInfo> atts) {
    for (AttachmentInfo att : atts) {
      att.setProtected(true);
    }
  }

  /**
   * Get the matched {@link PgpKey}. If the sender email matched to the email from {@link PgpContact} which got
   * from the private key than we return a relevant public key.
   *
   * @return A matched {@link PgpKey} or null.
   */
  private PgpKey getMatchedPublicPgpKey() {
    Js js = UiJsManager.getInstance(getContext()).getJs();
    PgpKeyInfo[] pgpKeyInfoArray = js.getStorageConnector().getAllPgpPrivateKeys();
    PgpKey matchedPgpKey = null;
    for (PgpKeyInfo pgpKeyInfo : pgpKeyInfoArray) {
      PgpKey pgpKey = js.crypto_key_read(pgpKeyInfo.getPrivate());
      if (pgpKey != null) {
        PgpContact primaryUserId = pgpKey.getPrimaryUserId();
        if (details.getEmail().equalsIgnoreCase(primaryUserId.getEmail())) {
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
    PrepareSendUserPublicKeyDialogFragment fragment = new PrepareSendUserPublicKeyDialogFragment();
    fragment.setTargetFragment(MessageDetailsFragment.this, REQUEST_CODE_SHOW_DIALOG_WITH_SEND_KEY_OPTION);
    fragment.show(getFragmentManager(), PrepareSendUserPublicKeyDialogFragment.class.getSimpleName());
  }

  /**
   * Send a template message with a sender public key.
   *
   * @param att An {@link AttachmentInfo} object which contains information about a sender public key.
   */
  private void sendTemplateMsgWithPublicKey(AttachmentInfo att) {
    List<AttachmentInfo> atts = null;
    if (att != null) {
      atts = new ArrayList<>();
      att.setProtected(true);
      atts.add(att);
    }

    startActivity(CreateMessageActivity.generateIntent(getContext(), msgInfo, MessageType.REPLY,
        MessageEncryptionType.STANDARD, new ServiceInfo.Builder()
            .setIsFromFieldEditable(false)
            .setIsToFieldEditable(false)
            .setIsSubjectEditable(false)
            .setIsMsgTypeSwitchable(false)
            .setHasAbilityToAddNewAtt(false)
            .setSystemMsg(getString(R.string.message_was_encrypted_for_wrong_key))
            .setAtts(atts)
            .build()));
  }

  /**
   * Update actions visibility using {@link FoldersManager.FolderType}
   *
   * @param localFolder The localFolder where current message exists.
   */
  private void updateActionsVisibility(LocalFolder localFolder) {
    folderType = FoldersManager.getFolderType(localFolder);

    if (folderType != null) {
      switch (folderType) {
        case INBOX:
          if (JavaEmailConstants.EMAIL_PROVIDER_GMAIL.equalsIgnoreCase(EmailUtil.getDomain(details.getEmail()))) {
            isArchiveActionEnabled = true;
          }
          isDeleteActionEnabled = true;
          break;

        case SENT:
          isDeleteActionEnabled = true;
          break;

        case TRASH:
          isMoveToInboxActionEnabled = true;
          isDeleteActionEnabled = false;
          break;

        case DRAFTS:
        case OUTBOX:
          isMoveToInboxActionEnabled = false;
          isArchiveActionEnabled = false;
          isDeleteActionEnabled = true;
          break;

        default:
          isMoveToInboxActionEnabled = true;
          isArchiveActionEnabled = false;
          isDeleteActionEnabled = true;
          break;
      }
    } else {
      isArchiveActionEnabled = false;
      isMoveToInboxActionEnabled = false;
      isDeleteActionEnabled = true;
    }

    getActivity().invalidateOptionsMenu();
  }

  /**
   * Run the message action (archive/delete/move to inbox).
   *
   * @param menuId The action menu id.
   */
  private void runMsgAction(final int menuId) {
    boolean isOutbox = JavaEmailConstants.FOLDER_OUTBOX.equalsIgnoreCase(details.getLabel());
    if (GeneralUtil.isInternetConnectionAvailable(getContext()) || isOutbox) {
      if (!isOutbox) {
        isAdditionalActionEnabled = false;
        getActivity().invalidateOptionsMenu();
        statusView.setVisibility(View.GONE);
        UIUtil.exchangeViewVisibility(getContext(), true, progressBarActionRunning, layoutContent);
      }

      switch (menuId) {
        case R.id.menuActionArchiveMessage:
          onActionListener.onArchiveMsgClicked();
          break;

        case R.id.menuActionDeleteMessage:
          onActionListener.onDeleteMsgClicked();
          break;

        case R.id.menuActionMoveToInbox:
          onActionListener.onMoveMsgToInboxClicked();
          break;
      }
    } else {
      showSnackbar(getView(), getString(R.string.internet_connection_is_not_available), getString(R.string.retry),
          Snackbar.LENGTH_LONG, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              runMsgAction(menuId);
            }
          });
    }
  }

  private void initViews(View view) {
    textViewSenderAddress = view.findViewById(R.id.textViewSenderAddress);
    textViewDate = view.findViewById(R.id.textViewDate);
    textViewSubject = view.findViewById(R.id.textViewSubject);
    viewFooterOfHeader = view.findViewById(R.id.layoutFooterOfHeader);
    layoutMsgParts = view.findViewById(R.id.layoutMessageParts);
    layoutMsgContainer = view.findViewById(R.id.layoutMessageContainer);
    layoutReplyBtns = view.findViewById(R.id.layoutReplyButtons);
    progressBarActionRunning = view.findViewById(R.id.progressBarActionRunning);

    layoutContent = view.findViewById(R.id.layoutContent);
    imageBtnReplyAll = view.findViewById(R.id.imageButtonReplyAll);
    imageBtnReplyAll.setOnClickListener(this);
  }

  private void updateViews() {
    if (details != null) {
      String subject = TextUtils.isEmpty(details.getSubject()) ? getString(R.string.no_subject) : details.getSubject();

      if (folderType == FoldersManager.FolderType.SENT) {
        textViewSenderAddress.setText(EmailUtil.getFirstAddressString(details.getTo()));
      } else {
        textViewSenderAddress.setText(EmailUtil.getFirstAddressString(details.getFrom()));
      }
      textViewSubject.setText(subject);
      if (JavaEmailConstants.FOLDER_OUTBOX.equalsIgnoreCase(details.getLabel())) {
        textViewDate.setText(dateFormat.format(details.getSentDate()));
      } else {
        textViewDate.setText(dateFormat.format(details.getReceivedDate()));
      }
    }

    updateMsgBody();
  }

  private void showAttsIfTheyExist() {
    if (details != null && details.hasAtts()) {
      LayoutInflater layoutInflater = LayoutInflater.from(getContext());

      if (!CollectionUtils.isEmpty(atts)) {
        for (final AttachmentInfo att : atts) {
          View rootView = layoutInflater.inflate(R.layout.attachment_item, layoutMsgParts, false);

          TextView textViewAttName = rootView.findViewById(R.id.textViewAttchmentName);
          textViewAttName.setText(att.getName());

          TextView textViewAttSize = rootView.findViewById(R.id.textViewAttSize);
          textViewAttSize.setText(Formatter.formatFileSize(getContext(), att.getEncodedSize()));

          final View button = rootView.findViewById(R.id.imageButtonDownloadAtt);
          button.setOnClickListener(getDownloadAttClickListener(att));

          if (att.getUri() != null) {
            View layoutAtt = rootView.findViewById(R.id.layoutAtt);
            layoutAtt.setOnClickListener(getOpenFileClickListener(att, button));
          }

          layoutMsgParts.addView(rootView);
        }
      }
    }
  }

  private View.OnClickListener getOpenFileClickListener(final AttachmentInfo att, final View button) {
    return new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (att.getUri().getLastPathSegment().endsWith(Constants.PGP_FILE_EXT)) {
          button.performClick();
        } else {
          Intent intentOpenFile = new Intent(Intent.ACTION_VIEW, att.getUri());
          intentOpenFile.setAction(Intent.ACTION_VIEW);
          intentOpenFile.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          intentOpenFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
          if (intentOpenFile.resolveActivity(getContext().getPackageManager()) != null) {
            startActivity(intentOpenFile);
          }
        }
      }
    };
  }

  private View.OnClickListener getDownloadAttClickListener(final AttachmentInfo att) {
    return new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        lastClickedAtt = att;
        lastClickedAtt.setOrderNumber(GeneralUtil.genAttOrderId(getContext()));
        boolean isPermissionGranted = ContextCompat.checkSelfPermission(getContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
        if (isPermissionGranted) {
          requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
              REQUEST_CODE_REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
          getContext().startService(AttachmentDownloadManagerService.newIntent
              (getContext(), lastClickedAtt));
        }
      }
    };
  }

  private void updateMsgView() {
    layoutMsgParts.removeAllViews();
    if (!TextUtils.isEmpty(msgInfo.getHtmlMsg())) {
      EmailWebView emailWebView = new EmailWebView(getContext());
      emailWebView.configure();

      int margin = getResources().getDimensionPixelOffset(R.dimen.default_margin_content);
      LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      layoutParams.setMargins(margin, 0, margin, 0);
      emailWebView.setLayoutParams(layoutParams);

      emailWebView.loadDataWithBaseURL(null, EmailUtil.genViewportHtml(msgInfo.getHtmlMsg()),
          "text/html", StandardCharsets.UTF_8.displayName(), null);

      layoutMsgParts.addView(emailWebView);
      emailWebView.setOnPageFinishedListener(new EmailWebView.OnPageFinishedListener() {
        public void onPageFinished() {
          updateReplyButtons();
        }
      });
    } else if (msgInfo.getMsgParts() != null && !msgInfo.getMsgParts().isEmpty()) {
      boolean isFirstMsgPartText = true;
      for (MessagePart msgPart : msgInfo.getMsgParts()) {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        if (msgPart != null) {
          switch (msgPart.getMsgPartType()) {
            case PGP_MESSAGE:
              msgEncryptType = MessageEncryptionType.ENCRYPTED;
              layoutMsgParts.addView(generatePgpMsgPart((MessagePartPgpMessage) msgPart, layoutInflater));
              break;

            case TEXT:
              layoutMsgParts.addView(generateTextPart(msgPart, layoutInflater));
              if (isFirstMsgPartText) {
                viewFooterOfHeader.setVisibility(View.VISIBLE);
              }
              break;

            case PGP_PUBLIC_KEY:
              layoutMsgParts.addView(generatePublicKeyPart((MessagePartPgpPublicKey) msgPart, layoutInflater));
              break;

            default:
              layoutMsgParts.addView(generateMsgPart(msgPart, layoutInflater, R.layout.message_part_other,
                  layoutMsgParts));
              break;
          }
        }
        isFirstMsgPartText = false;
      }
      updateReplyButtons();
    } else {
      layoutMsgParts.removeAllViews();
      updateReplyButtons();
    }
  }

  /**
   * Update the reply buttons layout depending on the {@link MessageEncryptionType}
   */
  private void updateReplyButtons() {
    if (layoutReplyBtns != null) {
      ImageView imageViewReply = layoutReplyBtns.findViewById(R.id.imageViewReply);
      ImageView imageViewReplyAll = layoutReplyBtns.findViewById(R.id.imageViewReplyAll);
      ImageView imageViewForward = layoutReplyBtns.findViewById(R.id.imageViewForward);

      TextView textViewReply = layoutReplyBtns.findViewById(R.id.textViewReply);
      TextView textViewReplyAll = layoutReplyBtns.findViewById(R.id.textViewReplyAll);
      TextView textViewForward = layoutReplyBtns.findViewById(R.id.textViewForward);

      if (msgEncryptType == MessageEncryptionType.ENCRYPTED) {
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

      layoutReplyBtns.findViewById(R.id.layoutReplyButton).setOnClickListener(this);
      layoutReplyBtns.findViewById(R.id.layoutReplyAllButton).setOnClickListener(this);
      layoutReplyBtns.findViewById(R.id.layoutForwardButton).setOnClickListener(this);

      layoutReplyBtns.setVisibility(View.VISIBLE);
    }
  }

  /**
   * Generate the public key part. There we can see the public key details and save/update the
   * key owner information to the local database.
   *
   * @param messagePartPgpPublicKey The {@link MessagePartPgpPublicKey} object which contains
   *                                information about a public key and his owner.
   * @param inflater                The {@link LayoutInflater} instance.
   * @return The generated view.
   */
  @NonNull
  private View generatePublicKeyPart(final MessagePartPgpPublicKey messagePartPgpPublicKey, LayoutInflater inflater) {

    final ViewGroup pubKeyView = (ViewGroup) inflater.inflate(R.layout.message_part_public_key, layoutMsgParts, false);
    final TextView textViewPgpPublicKey = pubKeyView.findViewById(R.id.textViewPgpPublicKey);
    Switch switchShowPublicKey = pubKeyView.findViewById(R.id.switchShowPublicKey);

    switchShowPublicKey.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        TransitionManager.beginDelayedTransition(pubKeyView);
        textViewPgpPublicKey.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        buttonView.setText(isChecked ? R.string.hide_the_public_key : R.string.show_the_public_key);
      }
    });

    if (!TextUtils.isEmpty(messagePartPgpPublicKey.getKeyOwner())) {
      TextView textViewKeyOwnerTemplate = pubKeyView.findViewById(R.id.textViewKeyOwnerTemplate);
      textViewKeyOwnerTemplate.setText(getString(R.string.template_message_part_public_key_owner,
          messagePartPgpPublicKey.getKeyOwner()));
    }

    TextView textViewKeyWordsTemplate = pubKeyView.findViewById(R.id.textViewKeyWordsTemplate);
    UIUtil.setHtmlTextToTextView(getString(R.string.template_message_part_public_key_key_words,
        messagePartPgpPublicKey.getKeyWords()), textViewKeyWordsTemplate);

    TextView textViewFingerprintTemplate = pubKeyView.findViewById(R.id.textViewFingerprintTemplate);
    UIUtil.setHtmlTextToTextView(getString(R.string.template_message_part_public_key_fingerprint,
        GeneralUtil.doSectionsInText(" ", messagePartPgpPublicKey.getFingerprint(), 4)),
        textViewFingerprintTemplate);

    textViewPgpPublicKey.setText(messagePartPgpPublicKey.getValue());

    if (messagePartPgpPublicKey.hasPgpContact()) {
      if (messagePartPgpPublicKey.isPgpContactUpdateEnabled()) {
        initUpdateContactButton(messagePartPgpPublicKey, pubKeyView);
      }
    } else {
      initSaveContactButton(messagePartPgpPublicKey, pubKeyView);
    }

    return pubKeyView;
  }

  /**
   * Init the save contact button. When we press this button a new contact will be saved to the
   * local database.
   *
   * @param msgPart                  The {@link MessagePartPgpPublicKey} object which contains
   *                                 information about a public key and his owner.
   * @param messagePartPublicKeyView The public key view container.
   */
  private void initSaveContactButton(final MessagePartPgpPublicKey msgPart, View messagePartPublicKeyView) {
    Button buttonSaveContact = messagePartPublicKeyView.findViewById(R.id.buttonSaveContact);
    if (buttonSaveContact != null) {
      buttonSaveContact.setVisibility(View.VISIBLE);
      buttonSaveContact.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          PgpContact pgpContact = new PgpContact(msgPart.getKeyOwner(), null, msgPart.getValue(), false, null, false,
              msgPart.getFingerprint(), msgPart.getLongId(), msgPart.getKeyWords(), 0);
          Uri uri = new ContactsDaoSource().addRow(getContext(), pgpContact);
          if (uri != null) {
            Toast.makeText(getContext(), R.string.contact_successfully_saved, Toast.LENGTH_SHORT).show();
            v.setVisibility(View.GONE);
          } else {
            Toast.makeText(getContext(), R.string.error_occurred_while_saving_contact, Toast.LENGTH_SHORT).show();
          }
        }
      });
    }
  }

  /**
   * Init the update contact button. When we press this button the contact will be updated in the
   * local database.
   *
   * @param messagePartPgpPublicKey The {@link MessagePartPgpPublicKey} object which contains
   *                                information about a public key and his owner.
   * @param view                    The public key view container.
   */
  private void initUpdateContactButton(final MessagePartPgpPublicKey messagePartPgpPublicKey, View view) {
    Button buttonUpdateContact = view.findViewById(R.id.buttonUpdateContact);
    if (buttonUpdateContact != null) {
      buttonUpdateContact.setVisibility(View.VISIBLE);
      buttonUpdateContact.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          PgpContact pgpContact = new PgpContact(messagePartPgpPublicKey.getKeyOwner(), null,
              messagePartPgpPublicKey.getValue(), false, null, false, messagePartPgpPublicKey.getFingerprint(),
              messagePartPgpPublicKey.getLongId(), messagePartPgpPublicKey.getKeyWords(), 0);
          boolean isUpdated = new ContactsDaoSource().updatePgpContact(getContext(), pgpContact) > 0;
          if (isUpdated) {
            Toast.makeText(getContext(), R.string.contact_successfully_updated, Toast.LENGTH_SHORT).show();
          } else {
            Toast.makeText(getContext(), R.string.error_occurred_while_updating_contact, Toast.LENGTH_SHORT).show();
          }
        }
      });
    }
  }

  @NonNull
  private TextView generateMsgPart(MessagePart part, LayoutInflater inflater, int res, ViewGroup viewGroup) {
    TextView textViewMsgPartOther = (TextView) inflater.inflate(res, viewGroup, false);
    textViewMsgPartOther.setText(part.getValue());
    return textViewMsgPartOther;
  }

  @NonNull
  private TextView generateTextPart(MessagePart messagePart, LayoutInflater layoutInflater) {
    return generateMsgPart(messagePart, layoutInflater, R.layout.message_part_text, layoutMsgParts);
  }

  @NonNull
  private View generatePgpMsgPart(MessagePartPgpMessage part, LayoutInflater layoutInflater) {
    if (part != null) {
      if (TextUtils.isEmpty(part.getErrorMsg())) {
        return generateMsgPart(part, layoutInflater, R.layout.message_part_pgp_message, layoutMsgParts);
      } else {
        switch (part.getPgpMsgDecryptError()) {
          case FORMAT_ERROR:
            final ViewGroup formatErrorLayout = (ViewGroup) layoutInflater.inflate(
                R.layout.message_part_pgp_message_format_error, layoutMsgParts, false);
            TextView textViewFormatError = formatErrorLayout.findViewById(R.id.textViewFormatError);
            textViewFormatError.setText(part.getErrorMsg());
            formatErrorLayout.addView(genShowOriginalMsgLayout
                (part.getValue(), layoutInflater, formatErrorLayout));
            return formatErrorLayout;

          case MISSING_PRIVATE_KEY:
            return generateMissingPrivateKeyLayout(part, layoutInflater);

          default:
            ViewGroup viewGroup = (ViewGroup) layoutInflater.inflate(
                R.layout.message_part_pgp_message_error, layoutMsgParts, false);
            TextView textViewErrorMsg = viewGroup.findViewById(R.id.textViewErrorMessage);
            textViewErrorMsg.setText(part.getErrorMsg());
            viewGroup.addView(genShowOriginalMsgLayout(part.getValue(), layoutInflater, viewGroup));

            return viewGroup;
        }
      }
    } else return new TextView(getContext());
  }

  /**
   * Generate a layout which describes the missing private keys situation.
   *
   * @param part     The {@link MessagePartPgpMessage} which contains info about an error.
   * @param inflater The {@link LayoutInflater} instance.
   * @return Generated layout.
   */
  @NonNull
  private View generateMissingPrivateKeyLayout(MessagePartPgpMessage part, LayoutInflater inflater) {
    ViewGroup viewGroup = (ViewGroup) inflater.inflate(
        R.layout.message_part_pgp_message_missing_private_key, layoutMsgParts, false);
    TextView textViewErrorMsg = viewGroup.findViewById(R.id.textViewErrorMessage);
    textViewErrorMsg.setText(part.getErrorMsg());

    Button buttonImportPrivateKey = viewGroup.findViewById(R.id.buttonImportPrivateKey);
    buttonImportPrivateKey.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivityForResult(ImportPrivateKeyActivity.newIntent(
            getContext(), getString(R.string.import_private_key), true, ImportPrivateKeyActivity.class),
            REQUEST_CODE_START_IMPORT_KEY_ACTIVITY);
      }
    });

    Button buttonSendOwnPublicKey = viewGroup.findViewById(R.id.buttonSendOwnPublicKey);
    buttonSendOwnPublicKey.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        PgpKey publicKey = getMatchedPublicPgpKey();
        if (publicKey == null) {
          showSendersPublicKeyDialog();
        } else {
          sendTemplateMsgWithPublicKey(EmailUtil.genAttInfoFromPubKey(publicKey));
        }
      }
    });

    viewGroup.addView(genShowOriginalMsgLayout(part.getValue(), inflater, viewGroup));
    return viewGroup;
  }

  /**
   * Generate a layout with switch button which will be regulate visibility of original message info.
   *
   * @param msg            The original pgp message info.
   * @param layoutInflater The {@link LayoutInflater} instance.
   * @param rootView       The root view which will be used while we create a new layout using
   *                       {@link LayoutInflater}.
   * @return A generated layout.
   */
  @NonNull
  private ViewGroup genShowOriginalMsgLayout(String msg, LayoutInflater layoutInflater,
                                             final ViewGroup rootView) {
    ViewGroup viewGroup = (ViewGroup) layoutInflater.inflate(R.layout.pgp_show_original_message, rootView, false);
    final TextView textViewOriginalPgpMsg = viewGroup.findViewById(R.id.textViewOriginalPgpMessage);
    textViewOriginalPgpMsg.setText(msg);

    Switch switchShowOriginalMsg = viewGroup.findViewById(R.id.switchShowOriginalMessage);

    switchShowOriginalMsg.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        TransitionManager.beginDelayedTransition(rootView);
        textViewOriginalPgpMsg.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        buttonView.setText(isChecked ? R.string.hide_original_message : R.string.show_original_message);
      }
    });
    return viewGroup;
  }

  public interface OnActionListener {
    void onArchiveMsgClicked();

    void onDeleteMsgClicked();

    void onMoveMsgToInboxClicked();
  }
}
