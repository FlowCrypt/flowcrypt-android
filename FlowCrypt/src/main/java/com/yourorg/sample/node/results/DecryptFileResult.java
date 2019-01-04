package com.yourorg.sample.node.results;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;

/**
 * json: { success: true, name: decryptedMeta.content.filename || '' }
 * data: raw decrypted bytes
 */
public class DecryptFileResult extends DecryptResult {

  public DecryptFileResult(Exception err, InputStream inputStream, long startTime) {
    super(err, inputStream, startTime);
  }

  public byte[] getDecryptedDataBytes() {
    throwIfDecryptErrNotTested();
    try {
      return getDataBinaryBytes();
    } catch (IOException e) {
      return null;
    }
  }

  public InputStream getDecryptedByteStream() {
    throwIfDecryptErrNotTested();
    return getInputStream();
  }

  public String getName() {
    throwIfDecryptErrNotTested();
    try {
      return jsonResponseParsed.getString("name");
    } catch (JSONException | NullPointerException e) {
      return "";
    }
  }

}
