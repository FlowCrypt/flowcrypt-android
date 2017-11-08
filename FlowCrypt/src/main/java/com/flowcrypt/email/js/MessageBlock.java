/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: Tom James Holub
 */

package com.flowcrypt.email.js;

import com.eclipsesource.v8.V8Object;

import java.util.Objects;

public class MessageBlock extends MeaningfulV8ObjectContainer {

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_PGP_MESSAGE = "message";
    public static final String TYPE_PGP_PUBLIC_KEY = "public_key";
    public static final String TYPE_PGP_SIGNED_MESSAGE = "signed_message";
    public static final String TYPE_PGP_PASSWORD_MESSAGE = "password_message";
    public static final String TYPE_ATTEST_PACKET = "attest_packet";
    public static final String TYPE_VERIFICATION = "cryptup_verification";


    private String type;
    private String content;
    private Boolean complete;
    private String signature;

    public MessageBlock (V8Object o) {
        super(o);
        type = getAttributeAsString("type");
        content = getAttributeAsString("content");
        complete = getAttributeAsBoolean("complete");
        signature = Objects.equals(type, TYPE_PGP_SIGNED_MESSAGE) ? getAttributeAsString("signature") : null;
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
