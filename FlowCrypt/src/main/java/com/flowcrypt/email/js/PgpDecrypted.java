/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.flowcrypt.email.js.core.MeaningfulV8ObjectContainer;

public class PgpDecrypted extends MeaningfulV8ObjectContainer {

  public PgpDecrypted(V8Object o) {
    super(o);
  }

  public Boolean isSuccess() {
    return this.getAttributeAsBoolean("success");
  }

  public Boolean isEncrypted() {
    return this.getAttributeAsBoolean("encrypted");
  }

  public PgpSignature getSignature() {
    V8Object signature = this.getAttributeAsObject("signature");
    if (signature == null) {
      return null;
    }
    return new PgpSignature(signature);
  }

  public Integer countUnsecureMdcErrors() {
    return getCount("unsecure_mdc");
  }

  public Integer countFormatErrors() {
    return getCount("format_error");
  }

  public Integer countKeyMismatchErrors() {
    return getCount("key_mismatch");
  }

  public Integer countPotentiallyMatchingKeys() {
    return getCount("key_mismatch");
  }

  public Integer countAttempts() {
    return getCount("attempts");
  }

  public String[] getEncryptedForLongids() {
    return getStrings("encrypted_for");
  }

  public String[] getMissingPassphraseLongids() {
    return getStrings("missing_passphrases");
  }

  public String getFormatError() {
    return this.getAttributeAsString("format_error");
  }

  public String[] getOtherErrors() {
    return getStrings("errors");
  }

  public String getString() {
    V8Object content = this.getAttributeAsObject("content");
    if (content == null) {
      return null;
    }
    return getAttributeAsString(content, "data");
  }

  public byte[] getBytes() {
    V8Object content = this.getAttributeAsObject("content");
    if (content == null) {
      return null;
    }
    return getAttributeAsBytes(content, "data");
  }

  private Integer getCount(String name) {
    V8Object counts = this.getAttributeAsObject("counts");
    if (counts == null) {
      return null;
    }
    return getAttributeAsInteger(counts, name);
  }

  private String[] getStrings(String name) {
    V8Array strings = this.getAttributeAsArray(name);
    if (strings == null || strings.isUndefined()) {
      return null;
    }
    return strings.getStrings(0, strings.length());
  }

}

