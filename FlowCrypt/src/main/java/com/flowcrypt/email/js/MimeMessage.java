/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.flowcrypt.email.js.core.Js;
import com.flowcrypt.email.js.core.MeaningfulV8ObjectContainer;

public class MimeMessage extends MeaningfulV8ObjectContainer {

  private Js js;

  public MimeMessage(V8Object o, Js js) {
    super(o);
    this.js = js;
  }

  public String getText() {
    return getAttributeAsString("text");
  }

  public String getHtml() {
    return this.getAttributeAsString("html");
  }

  public String getSignature() {
    return this.getAttributeAsString("signature");
  }

  public V8Object getHeaders() {
    return this.v8object.getObject("headers");
  }

  public String getStringHeader(String headerName) {
    return getAttributeAsString(getHeaders(), headerName);
  }

  public MimeAddress[] getAddressHeader(String headerName) {
    V8Array addresses = getAttributeAsArray(getHeaders(), headerName);
    MimeAddress[] results = new MimeAddress[addresses.length()];
    for (Integer i = 0; i < addresses.length(); i++) {
      results[i] = new MimeAddress(addresses.getObject(i));
    }
    return results;
  }

  public Attachment[] getAtts() {
    V8Array jsAtts = this.v8object.getArray("attachments");
    Attachment[] atts = new Attachment[jsAtts.length()];
    for (Integer i = 0; i < jsAtts.length(); i++) {
      atts[i] = new Attachment(jsAtts.getObject(i));
    }
    return atts;
  }

}
