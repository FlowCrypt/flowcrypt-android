/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;


public class ProcessedMime extends MeaningfulV8ObjectContainer {

  private Js js;

  ProcessedMime(V8Object o, Js js) {
    super(o);
    this.js = js;
  }

  public V8Object getHeaders() {
    return this.v8object.getObject("headers");
  }

  public String getStringHeader(String header_name) {
    return getAttributeAsString(getHeaders(), header_name);
  }

  public long getTimeHeader(String name) {
    return js.time_to_utc_timestamp(getStringHeader(name));
  }

  public MimeAddress[] getAddressHeader(String header_name) {
    V8Array addresses = getAttributeAsArray(getHeaders(), header_name);
    if (addresses == null) {
      return new MimeAddress[0];
    }
    MimeAddress[] results = new MimeAddress[addresses.length()];
    for (int i = 0; i < addresses.length(); i++) {
      results[i] = new MimeAddress(addresses.getObject(i));
    }
    return results;
  }

  public MessageBlock[] getBlocks() {
    return MessageBlock.arrayFromV8Array(getAttributeAsArray("blocks"));
  }
}
