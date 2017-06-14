/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: Tom James Holub
 */

package com.flowcrypt.email.test;

import com.eclipsesource.v8.V8Object;

public class MimeAddress extends MeaningfulV8ObjectContainer {

    public MimeAddress(V8Object o) {
        super(o);
    }

    public String getAddress() {
        return this.getAttributeAsString("address");
    }

    public String getName() {
        return this.getAttributeAsString("name");
    }

    public static String stringify(MimeAddress[] addresses) {
        String stringified = "";
        for(Integer i = 0; i < addresses.length; i++) {
            stringified += stringify(addresses[i].getName(), addresses[i].getAddress()) + (i < addresses.length - 1 ? "," : "");
        }
        return stringified;
    }

    public static String stringify(String name, String address) {
        return name != null && !name.isEmpty() ? name + " <" + address + ">" : address;
    }

}
