/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import android.text.TextUtils;

import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import java.util.ArrayList;

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
    return new PgpKey(v8object.executeObjectFunction("toPublic", new V8Array(this.v8object.getRuntime())), this.js);
  }

  /**
   * Get the timestamp of a creation date of the key by using the key long_id.
   *
   * @return The timestamp of the key.
   */
  public long getCreated() {
    V8Object created = getAttributeAsObject("primaryKey").getObject("created");
    return this.js.time_to_utc_timestamp(created.executeStringFunction("toString",
        new V8Array(this.v8object.getRuntime())));
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
    ArrayList<PgpContact> pgpContacts = new ArrayList<>();

    if (users != null) {
      for (int i = 0; i < users.length(); i++) {
        V8Object user = users.getObject(i);
        if (user != null) {
          V8Object userId = user.getObject("userId");
          if (userId != null) {
            String userIdValue = userId.getString("userid");
            if (!TextUtils.isEmpty(userIdValue)) {
              pgpContacts.add(js.str_parse_email(userIdValue));
            }
          }
        }
      }
    }

    return pgpContacts.toArray(new PgpContact[0]);
  }

  /**
   * Generate a file name for the given key.
   *
   * @return The generated file name.
   */
  public String genFileName() {
    return "0x" + getLongid();
  }
}
