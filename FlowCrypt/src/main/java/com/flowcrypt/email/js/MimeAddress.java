/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import com.eclipsesource.v8.V8Object;

public class MimeAddress extends MeaningfulV8ObjectContainer {

    public MimeAddress(V8Object o) {
        super(o);
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

    public String getAddress() {
        return this.getAttributeAsString("address");
    }

    public String getName() {
        return this.getAttributeAsString("name");
    }

}
