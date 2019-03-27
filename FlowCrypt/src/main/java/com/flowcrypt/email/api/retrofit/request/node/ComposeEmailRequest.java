/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node;

import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

/**
 * Using this class we can create a request to create a raw MIME message(encrypted or plane).
 *
 * @author Denis Bondarenko
 * Date: 3/27/19
 * Time: 3:00 PM
 * E-mail: DenBond7@gmail.com
 */
public class ComposeEmailRequest extends BaseNodeRequest {
  private static final String FORMAT_ENCRYPT_INLINE = "encrypt-inline";
  private static final String FORMAT_PLAIN = "plain";

  @Expose
  private List<String> pubKeys;

  @Expose
  private String format;

  @Expose
  private String text;

  @Expose
  private List<String> to;

  @Expose
  private List<String> cc;

  @Expose
  private List<String> bcc;

  @Expose
  private String from;

  @Expose
  private String subject;

  @Expose
  private String replyToMimeMsg;

  public ComposeEmailRequest(OutgoingMessageInfo info, List<String> pubKeys) {
    this.pubKeys = pubKeys;

    if (info != null) {
      format = info.getEncryptionType() == MessageEncryptionType.ENCRYPTED ? FORMAT_ENCRYPT_INLINE : FORMAT_PLAIN;
      text = info.getMsg();
      to = prepareRecipientsArray(info.getToPgpContacts());
      cc = prepareRecipientsArray(info.getCcPgpContacts());
      bcc = prepareRecipientsArray(info.getBccPgpContacts());
      from = info.getFromPgpContact().getEmail();
      subject = info.getSubject();
      replyToMimeMsg = info.getRawReplyMsg();
    }
  }

  @Override
  public String getEndpoint() {
    return "composeEmail";
  }

  private List<String> prepareRecipientsArray(PgpContact[] pgpContacts) {
    List<String> recipients = new ArrayList<>();
    for (PgpContact pgpContact : pgpContacts) {
      recipients.add(pgpContact.getEmail());
    }
    return recipients;
  }
}
