package com.flowcrypt.email.test;

import com.eclipsesource.v8.V8Object;

public class PgpKey extends MeaningfulV8ObjectContainer {

    private final Js js;

    public PgpKey(V8Object o, Js js) {
        super(o);
        this.js = js;
    }

    public Boolean isPrivate() {
        return this.v8object.executeBooleanFunction("isPrivate", null);
    }

    public String armor() {
        return this.v8object.executeStringFunction("armor", null);
    }

    public String getLongid() {
        return this.js.crypto_key_longid(this);
    }

    public String getFingerprint() {
        return this.js.crypto_key_fingerprint(this);
    }
}
