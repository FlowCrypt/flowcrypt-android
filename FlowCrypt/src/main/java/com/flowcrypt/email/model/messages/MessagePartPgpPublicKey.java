/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model.messages;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.js.PgpContact;

/**
 * This class describes the public key details.
 *
 * @author Denis Bondarenko
 * Date: 19.07.2017
 * Time: 12:02
 * E-mail: DenBond7@gmail.com
 */

public class MessagePartPgpPublicKey extends MessagePart {

  public static final Creator<MessagePartPgpPublicKey> CREATOR = new
      Creator<MessagePartPgpPublicKey>() {
        @Override
        public MessagePartPgpPublicKey createFromParcel(Parcel source) {
          return new MessagePartPgpPublicKey(source);
        }

        @Override
        public MessagePartPgpPublicKey[] newArray(int size) {
          return new MessagePartPgpPublicKey[size];
        }
      };

  private NodeKeyDetails nodeKeyDetails;
  private PgpContact existingPgpContact;

  public MessagePartPgpPublicKey(NodeKeyDetails nodeKeyDetails, PgpContact existingPgpContact) {
    super(MessagePartType.PGP_PUBLIC_KEY, nodeKeyDetails.getPublicKey());
    this.nodeKeyDetails = nodeKeyDetails;
    this.existingPgpContact = existingPgpContact;
  }

  protected MessagePartPgpPublicKey(Parcel in) {
    super(in);
    this.msgPartType = MessagePartType.PGP_PUBLIC_KEY;
    this.nodeKeyDetails = in.readParcelable(NodeKeyDetails.class.getClassLoader());
    this.existingPgpContact = in.readParcelable(PgpContact.class.getClassLoader());
  }

  @Override
  public String toString() {
    return "MessagePartPgpPublicKey{" +
        "nodeKeyDetails=" + nodeKeyDetails +
        ", existingPgpContact=" + existingPgpContact +
        "} " + super.toString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeParcelable(this.nodeKeyDetails, flags);
    dest.writeParcelable(this.existingPgpContact, flags);
  }

  public PgpContact getExistingPgpContact() {
    return existingPgpContact;
  }

  public NodeKeyDetails getNodeKeyDetails() {
    return nodeKeyDetails;
  }

  public boolean isPgpContactUpdateEnabled() {
    return existingPgpContact != null && existingPgpContact.getLongid() != null
        && !existingPgpContact.getLongid().equals(nodeKeyDetails.getLongId());
  }
}
