/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import com.eclipsesource.v8.V8Object;
import com.flowcrypt.email.js.core.Js;
import com.flowcrypt.email.js.core.MeaningfulV8ObjectContainer;

public class PgpKey extends MeaningfulV8ObjectContainer {

  private final Js js;

  public PgpKey(V8Object o, Js js) {
    super(o);
    this.js = js;
  }

  public String armor() {
    return this.v8object.executeStringFunction("armor", null);
  }

  public String getLongid() {
    return this.js.crypto_key_longid(this);
  }
}
