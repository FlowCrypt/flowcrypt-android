/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js.tasks;

import android.content.Context;
import android.text.TextUtils;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor;
import com.flowcrypt.email.api.retrofit.node.NodeRetrofitHelper;
import com.flowcrypt.email.api.retrofit.node.NodeService;
import com.flowcrypt.email.api.retrofit.request.node.DecryptMsgRequest;
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.api.retrofit.response.node.DecryptedMsgResult;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.JsListener;
import com.flowcrypt.email.js.MessageBlock;
import com.flowcrypt.email.js.MimeAddress;
import com.flowcrypt.email.js.MimeMessage;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpDecrypted;
import com.flowcrypt.email.js.PgpKeyInfo;
import com.flowcrypt.email.js.ProcessedMime;
import com.flowcrypt.email.js.core.Js;
import com.flowcrypt.email.model.messages.MessagePart;
import com.flowcrypt.email.model.messages.MessagePartPgpMessage;
import com.flowcrypt.email.model.messages.MessagePartPgpPublicKey;
import com.flowcrypt.email.model.messages.MessagePartSignedMessage;
import com.flowcrypt.email.model.messages.MessagePartText;
import com.flowcrypt.email.model.messages.MessagePartType;
import com.flowcrypt.email.util.exception.NodeEncryptException;
import com.flowcrypt.email.util.exception.NodeException;
import com.google.android.gms.common.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;

/**
 * This task can be used for decryption a raw MIME message.
 *
 * @author Denis Bondarenko
 * Date: 16.02.2018
 * Time: 10:39
 * E-mail: DenBond7@gmail.com
 */

public class DecryptRawMimeMessageJsTask extends BaseJsTask {
  private String rawMimeMsg;

  public DecryptRawMimeMessageJsTask(String ownerKey, int requestCode, String rawMimeMsg) {
    super(ownerKey, requestCode);
    this.rawMimeMsg = rawMimeMsg;
  }

  @Override
  public void runAction(Js js, JsListener jsListener) {
    IncomingMessageInfo incomingMsgInfo = new IncomingMessageInfo();
    if (!TextUtils.isEmpty(rawMimeMsg)) {

      NodeService nodeService = NodeRetrofitHelper.getInstance().getRetrofit().create(NodeService.class);

      List<String> passphrases = new ArrayList<>();

      PgpKeyInfo[] pgpKeyInfoArray = js.getStorageConnector().getAllPgpPrivateKeys();

      for (PgpKeyInfo pgpKeyInfo : pgpKeyInfoArray) {
        passphrases.add(js.getStorageConnector().getPassphrase(pgpKeyInfo.getLongid()));
      }

      DecryptMsgRequest request = new DecryptMsgRequest(rawMimeMsg, pgpKeyInfoArray,
          passphrases.toArray(new String[0]), true);

      try {
        retrofit2.Response<DecryptedMsgResult> response = nodeService.decryptMsg(request).execute();
        DecryptedMsgResult result = response.body();

        if (result == null) {
          throw new NullPointerException("decryptedMsgResult == null");
        }

        if (result.getError() != null) {
          throw new NodeEncryptException(result.getError().getMsg());
        }

        List<MsgBlock> blocks = result.getMsgBlocks();

        System.out.println(blocks);
      } catch (IOException e) {
        e.printStackTrace();
      } catch (NodeEncryptException e) {
        e.printStackTrace();
      }


      ProcessedMime processedMime = js.mime_process(rawMimeMsg);
      ArrayList<String> addressesFrom = new ArrayList<>();
      ArrayList<String> addressesTo = new ArrayList<>();
      ArrayList<String> addressesCc = new ArrayList<>();

      for (MimeAddress mimeAddress : processedMime.getAddressHeader("from")) {
        addressesFrom.add(mimeAddress.getAddress());
      }

      for (MimeAddress mimeAddress : processedMime.getAddressHeader("to")) {
        addressesTo.add(mimeAddress.getAddress());
      }

      for (MimeAddress mimeAddress : processedMime.getAddressHeader("cc")) {
        addressesCc.add(mimeAddress.getAddress());
      }

      incomingMsgInfo.setFrom(addressesFrom);
      incomingMsgInfo.setTo(addressesTo);
      incomingMsgInfo.setCc(addressesCc);
      incomingMsgInfo.setSubject(processedMime.getStringHeader("subject"));
      incomingMsgInfo.setOrigRawMsgWithoutAtts(rawMimeMsg);
      incomingMsgInfo.setMsgParts(getMsgParts(jsListener.getContext(), js, processedMime));

      long timestamp = processedMime.getTimeHeader("date");
      if (timestamp != -1) {
        incomingMsgInfo.setReceiveDate(new Date(timestamp));
      }

      MimeMessage mimeMsg = js.mime_decode(rawMimeMsg);

      if (mimeMsg != null) {
        if (!hasPGPBlocks(incomingMsgInfo)) {
          incomingMsgInfo.setHtmlMsg(mimeMsg.getHtml());
        }
        incomingMsgInfo.setHasPlainText(!TextUtils.isEmpty(mimeMsg.getText()));
      }

      jsListener.onMsgDecrypted(ownerKey, requestCode, incomingMsgInfo);
    } else {
      Exception npe = new NullPointerException("The raw MIME message is null or empty!");
      jsListener.onError(JsErrorTypes.TASK_RUNNING_ERROR, npe, ownerKey, requestCode);
    }
  }

  /**
   * Check that {@link IncomingMessageInfo} contains PGP blocks.
   *
   * @param incomingMsgInfo The incoming message.
   * @return true if {@link IncomingMessageInfo} contains PGP blocks
   * ({@link MessagePartType#PGP_MESSAGE}, {@link MessagePartType#PGP_PUBLIC_KEY},
   * {@link MessagePartType#PGP_PASSWORD_MESSAGE},  {@link MessagePartType#PGP_SIGNED_MESSAGE}), otherwise - false
   */
  private boolean hasPGPBlocks(IncomingMessageInfo incomingMsgInfo) {
    if (incomingMsgInfo != null) {
      List<MessagePart> messageParts = incomingMsgInfo.getMsgParts();

      if (messageParts != null) {
        for (MessagePart messagePart : messageParts) {
          if (messagePart.getMsgPartType() != null) {
            switch (messagePart.getMsgPartType()) {
              case PGP_MESSAGE:
              case PGP_PUBLIC_KEY:
              case PGP_PASSWORD_MESSAGE:
              case PGP_SIGNED_MESSAGE:
                return true;
            }
          }
        }
      }
    }
    return false;
  }

  /**
   * Generate a list of {@link MessagePart} object which contains information about
   * {@link MessageBlock}
   *
   * @param context       Interface to global information about an application environment;
   * @param js            The {@link Js} util.
   * @param processedMime The {@link ProcessedMime} object which contains information about an
   *                      encrypted message.
   * @return The list of {@link MessagePart}.
   */
  private List<MessagePart> getMsgParts(Context context, Js js, ProcessedMime processedMime) {
    MessageBlock[] blocks = processedMime.getBlocks();

    LinkedList<MessagePart> msgParts = new LinkedList<>();

    for (MessageBlock messageBlock : blocks) {
      if (messageBlock != null && messageBlock.getType() != null) {
        switch (messageBlock.getType()) {
          case MessageBlock.TYPE_TEXT:
            msgParts.add(new MessagePartText(messageBlock.getContent()));
            break;

          case MessageBlock.TYPE_PGP_MESSAGE:
            msgParts.add(genPgpMsgPart(context, js, messageBlock));
            break;

          case MessageBlock.TYPE_PGP_PUBLIC_KEY:
            try {
              List<NodeKeyDetails> nodeKeyDetails = NodeCallsExecutor.parseKeys(messageBlock.getContent());
              if (!CollectionUtils.isEmpty(nodeKeyDetails)) {
                NodeKeyDetails keyDetails = nodeKeyDetails.get(0);
                String keyOwner = keyDetails.getPrimaryPgpContact().getEmail();
                PgpContact pgpContact = new ContactsDaoSource().getPgpContact(context, keyOwner);
                MessagePartPgpPublicKey part = new MessagePartPgpPublicKey(keyDetails, pgpContact);
                msgParts.add(part);
              }
            } catch (IOException | NodeException e) {
              e.printStackTrace();
            }

            break;

          case MessageBlock.TYPE_PGP_SIGNED_MESSAGE:
            msgParts.add(new MessagePartSignedMessage(messageBlock.getContent()));
            break;

          case MessageBlock.TYPE_VERIFICATION:
            msgParts.add(new MessagePart(MessagePartType.VERIFICATION, messageBlock.getContent()));
            break;

          case MessageBlock.TYPE_ATTEST_PACKET:
            msgParts.add(new MessagePart(MessagePartType.ATTEST_PACKET, messageBlock.getContent()));
            break;

          case MessageBlock.TYPE_PGP_PASSWORD_MESSAGE:
            msgParts.add(new MessagePart(MessagePartType.PGP_PASSWORD_MESSAGE, messageBlock.getContent()));
            break;
        }
      }
    }
    return msgParts;
  }

  /**
   * Generate {@link MessagePartPgpMessage} from encrypted {@link MessageBlock}.
   *
   * @param context  Interface to global information about an application environment;
   * @param js       The {@link Js} util;
   * @param msgBlock The encrypted {@link MessageBlock}.
   * @return Generated {@link MessagePartPgpMessage}.
   */
  @NonNull
  private MessagePartPgpMessage genPgpMsgPart(Context context, Js js, MessageBlock msgBlock) {
    String encryptedContent = msgBlock.getContent();
    String value = encryptedContent;
    String errorMsg = null;
    MessagePartPgpMessage.PgpMessageDecryptError pgpDecryptError = null;

    if (TextUtils.isEmpty(encryptedContent)) {
      return new MessagePartPgpMessage("", null, null);
    }

    PgpDecrypted pgpDecrypt = js.crypto_message_decrypt(encryptedContent);

    if (pgpDecrypt != null) {
      if (pgpDecrypt.isSuccess()) {
        value = pgpDecrypt.getString();
      } else if (!TextUtils.isEmpty(pgpDecrypt.getFormatError())) {
        errorMsg = context.getString(R.string.decrypt_error_message_badly_formatted,
            context.getString(R.string.app_name)) + "\n\n" + pgpDecrypt.getFormatError();
        pgpDecryptError = MessagePartPgpMessage.PgpMessageDecryptError.FORMAT_ERROR;
      } else if (pgpDecrypt.getMissingPassphraseLongids() != null
          && pgpDecrypt.getMissingPassphraseLongids().length > 0) {
        pgpDecryptError = MessagePartPgpMessage.PgpMessageDecryptError.MISSING_PASS_PHRASES;
      } else if (Objects.equals(pgpDecrypt.countPotentiallyMatchingKeys(), pgpDecrypt.countAttempts())
          && Objects.equals(pgpDecrypt.countKeyMismatchErrors(), pgpDecrypt.countAttempts())) {
        pgpDecryptError = MessagePartPgpMessage.PgpMessageDecryptError.MISSING_PRIVATE_KEY;
        if (pgpDecrypt.getEncryptedForLongids().length > 1) {
          errorMsg = context.getString(R.string.decrypt_error_current_key_cannot_message);
        } else {
          errorMsg = context.getString(R.string.decrypt_error_could_not_open_message,
              context.getString(R.string.app_name)) + "\n\n" +
              context.getString(R.string.decrypt_error_single_sender);
        }
      } else if (pgpDecrypt.countUnsecureMdcErrors() > 0) {
        pgpDecryptError = MessagePartPgpMessage.PgpMessageDecryptError.UNSECURED_MDC_ERROR;
      } else if (pgpDecrypt.getOtherErrors() != null && pgpDecrypt.getOtherErrors().length > 0) {
        pgpDecryptError = MessagePartPgpMessage.PgpMessageDecryptError.OTHER_ERRORS;
        StringBuilder builder = new StringBuilder();
        builder.append(context.getString(R.string.decrypt_error_could_not_open_message,
            context.getString(R.string.app_name)));
        builder.append("\n\n");
        builder.append(context.getString(R.string.decrypt_error_please_write_me, context
            .getString(R.string.support_email)));
        builder.append("\n\n");

        for (String s : pgpDecrypt.getOtherErrors()) {
          builder.append(s);
          builder.append("\n");
        }

        errorMsg = builder.toString();
      } else {
        pgpDecryptError = MessagePartPgpMessage.PgpMessageDecryptError.UNKNOWN_ERROR;
        errorMsg = context.getString(R.string.decrypt_error_could_not_open_message,
            context.getString(R.string.app_name)) +
            "\n\n" + context.getString(R.string.decrypt_error_please_write_me);
      }
    } else {
      pgpDecryptError = MessagePartPgpMessage.PgpMessageDecryptError.JS_TOOL_ERROR;
      errorMsg = context.getString(R.string.decrypt_error_js_tool_error) + "\n\n" +
          context.getString(R.string.decrypt_error_please_write_me);
    }

    return new MessagePartPgpMessage(value, errorMsg, pgpDecryptError);
  }
}
