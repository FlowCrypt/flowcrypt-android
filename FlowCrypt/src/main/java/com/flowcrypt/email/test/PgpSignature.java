/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/tree/master/src/LICENSE
 * Contributors: Tom James Holub
 */

package com.flowcrypt.email.test;

import com.eclipsesource.v8.V8Object;

public class PgpSignature extends MeaningfulV8ObjectContainer {

    public PgpSignature(V8Object o) {
        super(o); // { signer: null, contact: optional_contact || null,  match: null, error: null }
    }

    public String getSignerLongid() {
        return this.getAttributeAsString("signer");
    }

    public Boolean isMatch() {
        return this.getAttributeAsBoolean("match");
    }

    public String getError() {
        return this.getAttributeAsString("error");
    }

}
