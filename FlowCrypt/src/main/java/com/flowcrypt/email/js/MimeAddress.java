/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import com.eclipsesource.v8.V8Object;
import com.flowcrypt.email.js.core.MeaningfulV8ObjectContainer;

public class MimeAddress extends MeaningfulV8ObjectContainer {

  public MimeAddress(V8Object o) {
    super(o);
  }

  public static String stringify(MimeAddress[] addresses) {
    StringBuilder builder = new StringBuilder();
    for (Integer i = 0; i < addresses.length; i++) {
      builder.append(stringify(addresses[i].getName(), addresses[i].getAddress()));
      builder.append(i < addresses.length - 1 ? "," : "");
    }
    return builder.toString();
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
