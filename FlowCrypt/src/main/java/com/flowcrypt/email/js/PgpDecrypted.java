/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: Tom James Holub
 */

package com.flowcrypt.email.js;

import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

public class PgpDecrypted extends MeaningfulV8ObjectContainer {

    public PgpDecrypted(V8Object o) {
        super(o);
    }

    public Boolean isSuccess() {
        return this.getAttributeAsBoolean("success");
    }

    public Boolean isEncrypted() {
        return this.getAttributeAsBoolean("encrypted");
    }

    public PgpSignature getSignature() {
        V8Object signature = this.getAttributeAsObject("signature");
        if(signature == null) {
            return null;
        }
        return new PgpSignature(signature);
    }

    public Integer countUnsecureMdcErrors() {
        return getCount("unsecure_mdc");
    }

    public Integer countFormatErrors() {
        return getCount("format_error");
    }

    public Integer countKeyMismatchErrors() {
        return getCount("key_mismatch");
    }

    public Integer countPotentiallyMatchingKeys() {
        return getCount("key_mismatch");
    }

    public String[] getEncryptedForLongids() {
        return getStrings("encrypted_for");
    }

    public String[] getMissingPassphraseLongids() {
        return getStrings("missing_passphrases");
    }

    public String getFormatError() {
        return this.getAttributeAsString("format_error");
    }

    public String[] getOtherErrors() {
        return getStrings("errors");
    }

    public String getContent() {
        V8Object content = this.getAttributeAsObject("content");
        if(content == null) {
            return null;
        }
        return this.getAttributeAsString(content, "data"); // todo - may need to convert from uint8
    }

    private Integer getCount(String name) {
        V8Object counts = this.getAttributeAsObject("counts");
        if(counts == null) {
            return null;
        }
        return this.getAttributeAsInteger(counts, name);
    }

    private String[] getStrings(String name) {
        V8Array strings = this.getAttributeAsArray(name);
        if(strings == null || strings.isUndefined()) {
            return null;
        }
        return strings.getStrings(0, strings.length());
    }

}

