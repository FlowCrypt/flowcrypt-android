package com.flowcrypt.email.test;

import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import java.util.Date;

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

    public PgpKey toPublic() {
        return new PgpKey(this.v8object.executeObjectFunction("toPublic", new V8Array(this.v8object.getRuntime())), this.js);
    }

    /**
     * Get the timestamp of a creation date of the key by using the key long_id.
     *
     * @return The timestamp of the key.
     */
    public long getCreated() {
        V8Object created = getAttributeAsObject("primaryKey").getObject("created");
        return this.js.time_to_utc_timestamp(created.executeStringFunction("toString", new V8Array(this.v8object.getRuntime())));
    }

    /**
     * Get information about the key owner
     *
     * @return The PgpContact of the key owner / primary user
     */
    public PgpContact getPrimaryUserId() {
        V8Array users = getAttributeAsArray("users");
        if(users.length() == 0) { // could happen to some keys. But will not happen to keys that are saved because we will check keys before saving
            return null;
        }
        return js.str_parse_email(users.getObject(0).getObject("userId").getString("userid"));
    }

}
