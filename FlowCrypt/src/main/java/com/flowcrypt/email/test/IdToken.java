/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: Tom James Holub
 */

package com.flowcrypt.email.test;

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
