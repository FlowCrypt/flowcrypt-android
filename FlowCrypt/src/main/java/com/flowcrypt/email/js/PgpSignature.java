/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import com.eclipsesource.v8.V8Object;

public class PgpSignature extends MeaningfulV8ObjectContainer {

  public PgpSignature(V8Object o) {
    super(o); // { signer: null, contact: optional_contact || null,  match: null, error: null }
  }

  public String getSignerLongid() {
    return this.getAttributeAsString("signer");
  }

  public Boolean isMatch() {
    return this.getAttributeAsBoolean("match");
  }

  public String getError() {
    return this.getAttributeAsString("error");
  }

}
