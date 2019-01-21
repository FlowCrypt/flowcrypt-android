/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.flowcrypt.email.js.core.MeaningfulV8ObjectContainer;

import java.util.Objects;

public class MessageBlock extends MeaningfulV8ObjectContainer {

  public static final String TYPE_TEXT = "text";
  public static final String TYPE_PGP_MESSAGE = "message";
  public static final String TYPE_PGP_PUBLIC_KEY = "public_key";
  public static final String TYPE_PGP_SIGNED_MESSAGE = "signed_message";
  public static final String TYPE_PGP_PASSWORD_MESSAGE = "password_message";
  public static final String TYPE_ATTEST_PACKET = "attest_packet";
  public static final String TYPE_VERIFICATION = "cryptup_verification";
  public static final String TYPE_PGP_PRIVATE_KEY = "private_key";

  private String type;
  private String content;
  private Boolean complete;
  private String signature;

  public MessageBlock(V8Object o) {
    super(o);
    type = getAttributeAsString("type");
    content = getAttributeAsString("content");
    complete = getAttributeAsBoolean("complete");
    signature = Objects.equals(type, TYPE_PGP_SIGNED_MESSAGE) ? getAttributeAsString("signature") : null;
  }

  public static MessageBlock[] arrayFromV8Array(V8Array blocks) {
    int length = (blocks != null) ? blocks.length() : 0;
    MessageBlock[] result = new MessageBlock[length];
    for (int i = 0; i < length; i++) {
      result[i] = new MessageBlock(blocks.getObject(i));
    }
    return result;
  }

  public String getType() {
    return type;
  }

  public String getContent() {
    return content;
  }

  public Boolean isComplete() {
    return complete;
  }

  public String getSignature() {
    return signature;
  }
}
