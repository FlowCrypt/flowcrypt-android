/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import com.eclipsesource.v8.V8Object;
import com.flowcrypt.email.js.core.MeaningfulV8ObjectContainer;

public class Attachment extends MeaningfulV8ObjectContainer {

  public Attachment(V8Object o) {
    super(o);
  }

  public String getName() {
    return this.v8object.getString("name");
  }

  public String getType() {
    return this.v8object.getString("type");
  }

  public Double getSize() {
    return this.v8object.getDouble("size");
  }

}
