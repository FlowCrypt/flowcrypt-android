package com.yourorg.sample.node.results;

import java.io.InputStream;

public class EncryptMsgResult extends RawNodeResult {

  public EncryptMsgResult(Exception err, InputStream inputStream, long startTime) {
    super(err, inputStream, startTime);
  }

  public String getEncryptedString() {
    throwIfErrNotTested();
    return getDataTextString();
  }

}
