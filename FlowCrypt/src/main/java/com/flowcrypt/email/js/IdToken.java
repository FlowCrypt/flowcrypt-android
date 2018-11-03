/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import com.eclipsesource.v8.V8Object;

public class IdToken extends MeaningfulV8ObjectContainer {

  IdToken(V8Object o) {
    super(o);
        /* keys: [
            "azp", "aud", "sub", "email", "email_verified", "iss", "iat", "exp", "name", "picture",
            "given_name", "family_name", "locale"
        ] */
  }

  public String getEmail() {
    return this.v8object.getString("email");
  }

  public Boolean isEmailVerified() {
    return this.v8object.getBoolean("email_verified");
  }

  public String getName() {
    return this.v8object.getString("name");
  }

  public String getGivenName() {
    return this.v8object.getString("given_name");
  }

  public String getFamilyName() {
    return this.v8object.getString("family_name");
  }

  public String getPicture() {
    return this.v8object.getString("picture");
  }

  public String getLocale() {
    return this.v8object.getString("locale");
  }

}
