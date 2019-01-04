package com.yourorg.sample.node.results;

import java.io.IOException;
import java.io.InputStream;

public class EncryptFileResult extends RawNodeResult {

  public EncryptFileResult(Exception err, InputStream inputStream, long startTime) {
    super(err, inputStream, startTime);
  }

  public byte[] getEncryptedDataBytes() {
    throwIfErrNotTested();
    try {
      return getDataBinaryBytes();
    } catch (IOException e) {
      return null;
    }
  }

  public InputStream getEncryptedDataStream() {
    throwIfErrNotTested();
    return getInputStream();
  }

}
