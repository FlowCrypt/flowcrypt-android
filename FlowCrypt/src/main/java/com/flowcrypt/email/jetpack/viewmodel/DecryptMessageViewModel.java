/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel;

import android.app.Application;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor;
import com.flowcrypt.email.api.retrofit.node.PgpApiRepository;
import com.flowcrypt.email.api.retrofit.request.node.DecryptMsgRequest;
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.api.retrofit.response.node.DecryptedMsgResult;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpKeyInfo;
import com.flowcrypt.email.js.UiJsManager;
import com.flowcrypt.email.model.messages.MessagePart;
import com.flowcrypt.email.model.messages.MessagePartHtml;
import com.flowcrypt.email.model.messages.MessagePartPgpMessage;
import com.flowcrypt.email.model.messages.MessagePartPgpPublicKey;
import com.flowcrypt.email.model.messages.MessagePartSignedMessage;
import com.flowcrypt.email.model.messages.MessagePartText;
import com.flowcrypt.email.model.messages.MessagePartType;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.util.exception.NodeException;
import com.google.android.gms.common.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

/**
 * This {@link ViewModel} implementation can be used to parse and decrypt (if needed) an incoming message.
 *
 * @author Denis Bondarenko
 * Date: 3/21/19
 * Time: 11:47 AM
 * E-mail: DenBond7@gmail.com
 */
public class DecryptMessageViewModel extends BaseNodeApiViewModel implements SecurityStorageConnector.OnRefreshListener {
  private SecurityStorageConnector connector;
  private PgpApiRepository apiRepository;
  private String rawMessage;

  public DecryptMessageViewModel(@NonNull Application application) {
    super(application);
  }

  @Override
  public void onRefresh() {
    decryptMessage(rawMessage);
  }

  public void init(PgpApiRepository apiRepository) {
    this.apiRepository = apiRepository;
    this.connector = UiJsManager.getInstance(getApplication()).getSecurityStorageConnector();
    this.connector.attachOnRefreshListener(this);
  }

  public void decryptMessage(String rawMessage) {
    this.rawMessage = rawMessage;
    List<String> passphrases = new ArrayList<>();

    SecurityStorageConnector connector = UiJsManager.getInstance(getApplication()).getSecurityStorageConnector();

    PgpKeyInfo[] pgpKeyInfoArray = connector.getAllPgpPrivateKeys();

    for (PgpKeyInfo pgpKeyInfo : pgpKeyInfoArray) {
      passphrases.add(connector.getPassphrase(pgpKeyInfo.getLongid()));
    }

    apiRepository.parseAndDecryptMsg(R.id.live_data_id_parse_and_decrypt_msg, responsesLiveData,
        new DecryptMsgRequest(rawMessage, pgpKeyInfoArray, passphrases.toArray(new String[0]), true));
  }

  public IncomingMessageInfo getIncomingMsgInfo(GeneralMessageDetails details, DecryptedMsgResult decryptedMsgResult) {
    IncomingMessageInfo msgInfo = new IncomingMessageInfo(details);
    msgInfo.setSubject(details.getSubject());
    msgInfo.setMsgParts(getMsgParts(decryptedMsgResult.getMsgBlocks()));

    return msgInfo;
  }

  /**
   * Generate a list of {@link MessagePart} object which contains information about
   * {@link MsgBlock}
   *
   * @return The list of {@link MessagePart}.
   */
  private List<MessagePart> getMsgParts(List<MsgBlock> blocks) {

    LinkedList<MessagePart> msgParts = new LinkedList<>();

    for (MsgBlock msgBlock : blocks) {
      if (msgBlock != null && msgBlock.getType() != null) {
        switch (msgBlock.getType()) {
          case MsgBlock.TYPE_TEXT:
            msgParts.add(new MessagePartText(msgBlock.getContent()));
            break;

          case MsgBlock.TYPE_PGP_MESSAGE:
            msgParts.add(new MessagePartPgpMessage(msgBlock.getContent(), null, null));
            break;

          case MsgBlock.TYPE_PGP_PUBLIC_KEY:
            try {
              List<NodeKeyDetails> nodeKeyDetails = NodeCallsExecutor.parseKeys(msgBlock.getContent());
              if (!CollectionUtils.isEmpty(nodeKeyDetails)) {
                NodeKeyDetails keyDetails = nodeKeyDetails.get(0);
                String keyOwner = keyDetails.getPrimaryPgpContact().getEmail();
                PgpContact pgpContact = new ContactsDaoSource().getPgpContact(getApplication(), keyOwner);
                MessagePartPgpPublicKey part = new MessagePartPgpPublicKey(keyDetails, pgpContact);
                msgParts.add(part);
              }
            } catch (IOException | NodeException e) {
              e.printStackTrace();
            }

            break;

          case MsgBlock.TYPE_PGP_SIGNED_MESSAGE:
            msgParts.add(new MessagePartSignedMessage(msgBlock.getContent()));
            break;

          case MsgBlock.TYPE_VERIFICATION:
            msgParts.add(new MessagePart(MessagePartType.VERIFICATION, msgBlock.getContent()));
            break;

          case MsgBlock.TYPE_ATTEST_PACKET:
            msgParts.add(new MessagePart(MessagePartType.ATTEST_PACKET, msgBlock.getContent()));
            break;

          case MsgBlock.TYPE_PGP_PASSWORD_MESSAGE:
            msgParts.add(new MessagePart(MessagePartType.PGP_PASSWORD_MESSAGE, msgBlock.getContent()));
            break;

          case MsgBlock.TYPE_HTML:
            msgParts.add(new MessagePartHtml(msgBlock.getContent()));
            break;
        }
      }
    }
    return msgParts;
  }
}
