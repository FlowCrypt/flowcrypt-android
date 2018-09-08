/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import com.eclipsesource.v8.V8Array;
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

    public void encrypt(String passphrase) {
        this.v8object.executeVoidFunction("encrypt", new V8Array(this.v8object.getRuntime()).push(passphrase));
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
        return new PgpKey(this.v8object.executeObjectFunction("toPublic", new V8Array(this.v8object.getRuntime())),
                this.js);
    }

    /**
     * Get the timestamp of a creation date of the key by using the key long_id.
     *
     * @return The timestamp of the key.
     */
    public long getCreated() {
        V8Object created = getAttributeAsObject("primaryKey").getObject("created");
        return this.js.time_to_utc_timestamp(created.executeStringFunction("toString", new V8Array(this.v8object
                .getRuntime())));
    }

    /**
     * Get information about the key owner
     *
     * @return The PgpContact of the key owner / primary user
     */
    public PgpContact getPrimaryUserId() {
        V8Array users = getAttributeAsArray("users");
        if (users.length() == 0) { // could happen to some keys. But will not happen to keys that are saved because
            // we will check keys before saving
            return null;
        }
        return js.str_parse_email(users.getObject(0).getObject("userId").getString("userid"));
    }

    /**
     * Get information about all available <code>uid(s)</code> of a given key
     *
     * @return An array of {@link PgpContact} of the given key.
     */
    public PgpContact[] getUserIds() {
        V8Array users = getAttributeAsArray("users");

        PgpContact[] pgpContacts = new PgpContact[users.length()];

        for (int i = 0; i < users.length(); i++) {
            pgpContacts[i] = js.str_parse_email(users.getObject(i).getObject("userId").getString("userid"));
        }

        return pgpContacts;
    }
}
