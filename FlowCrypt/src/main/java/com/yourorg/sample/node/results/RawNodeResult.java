package com.yourorg.sample.node.results;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;

public class RawNodeResult {
  /**
   * Responses from Node.js come formatted as a JSON before the first \n mark, and optional binary data afterwards
   * stripAndParseJsonResponseLineIfNotStrippedYet() will strip the first JSON line off the rest when accessing data
   */

  private final int bufferSize = 1024; // buffer sizes: 1024*8 or 1024*16 or 1024*32
  public long ms;
  protected boolean jsonResponseLineAlreadyStripped = false;
  protected String jsonResponseRaw;
  protected JSONObject jsonResponseParsed;
  private Exception err;
  private InputStream inputStream;
  private boolean errWasTested = false;

  public RawNodeResult(Exception err, InputStream inputStream, long ms) {
    this.err = err;
    this.inputStream = inputStream;
    this.ms = ms;
  }

  public <T> T convertTo(Class<T> cls) {
    try {
      Class[] argClasses = new Class[]{Exception.class, InputStream.class, long.class};
      return cls.getDeclaredConstructor(argClasses).newInstance(this.err, this.inputStream, this.ms);
    } catch (Exception e) {
      throw new RuntimeException("RawNodeResult wrong constructor definition", e);
    }
  }

  public Exception getErr() {
    errWasTested = true;
    stripAndParseJsonResponseLineIfNotStrippedYet();
    return err;
  }

  protected void throwIfErrNotTested() {
    if (!errWasTested) {
      throw new Error("RawNodeResult getErr() must be called before accessing data");
    }
  }

  protected InputStream getInputStream() {
    throwIfErrNotTested();
    return inputStream;
  }

  protected void closeInputStream() {
    throwIfErrNotTested();
    if (inputStream != null) {
      try {
        inputStream.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @NonNull
  @Override
  public String toString() {
    return jsonResponseParsed.toString();
  }

  private String markAndInterpretUtfBytesFromInputStream(ByteArrayOutputStream bytes, InputStream inputStream) {
    inputStream.mark(0); // do not re-read the same bytes next time
    return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
  }

  protected String readOneUtfLineFromInputStream() {
    throwIfErrNotTested();
    if (inputStream == null) {
      return null;
    }
    // do not use StringBuilder, it cannot do utf one byte at a time
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    int c;
    try {
      while ((c = inputStream.read()) != -1) {
        if (c == '\n') {
          return markAndInterpretUtfBytesFromInputStream(bytes, inputStream);
        }
        bytes.write((byte) c);
      }
      return markAndInterpretUtfBytesFromInputStream(bytes, inputStream);
    } catch (IOException e) {
      return null;
    }
  }

  protected JSONObject parseJson(String line) {
    if (line == null) {
      return null;
    }
    try {
      return new JSONObject(line);
    } catch (JSONException e) {
      return null;
    }
  }

  private void stripAndParseJsonResponseLineIfNotStrippedYet() {
    throwIfErrNotTested();
    if (!jsonResponseLineAlreadyStripped) {
      jsonResponseLineAlreadyStripped = true;
      jsonResponseRaw = readOneUtfLineFromInputStream();
      jsonResponseParsed = parseJson(jsonResponseRaw);
    }
  }

  protected byte[] getDataBinaryBytes() throws IOException {
    throwIfErrNotTested();
    if (inputStream == null) {
      return null;
    }
    ByteArrayOutputStream collector = new ByteArrayOutputStream();
    byte[] buffer = new byte[bufferSize];
    int bytesReadCount;
    while ((bytesReadCount = inputStream.read(buffer, 0, buffer.length)) != -1) {
      collector.write(buffer, 0, bytesReadCount);
    }
    return collector.toByteArray();
  }

  protected String getDataTextString() {
    throwIfErrNotTested();
    InputStream inputStream = getInputStream();
    if (inputStream != null) {
      BufferedReader br = new BufferedReader(new InputStreamReader(getInputStream()), bufferSize);
      return br.lines().collect(Collectors.joining("\n"));
    } else return "";
  }
}

/**
 * DecryptErrTypes: key_mismatch | use_password | wrong_password | no_mdc | need_passphrase | format | other
 * DecryptError$longids: { message: string[]; matching: string[]; chosen: string[]; needPassphrase: string[]; }
 * json: { success: false; error: { type: DecryptErrTypes; error?: string; }; longids: DecryptError$longids; isEncrypted?: boolean; }
 */
abstract class DecryptResult extends RawNodeResult {

  public String ERR_KEY_MISMATCH = "key_mismatch";
  public String ERR_USE_PASSWORD = "use_password";
  public String ERR_WRONG_PASSWORD = "wrong_password";
  public String ERR_NO_MDC = "no_mdc";
  public String ERR_NEED_PASSPHRASE = "need_passphrase";
  public String ERR_FORMAT = "format";
  public String ERR_OTHER = "other";
  public String ERR_BAD_RESPONSE = "bad_response"; // this is only defined in Java when we get unexpected data from Node

  protected boolean decryptErrTested = false;

  DecryptResult(Exception err, InputStream inputStream, long startTime) {
    super(err, inputStream, startTime);
  }

  public DecryptErr getDecryptErr() {
    throwIfErrNotTested();
    decryptErrTested = true;
    try {
      if (jsonResponseParsed.getBoolean("success")) {
        return null;
      }
      JSONObject error = jsonResponseParsed.getJSONObject("error");
      return new DecryptErr(error.getString("type"), error.has("error") ? error.getString("error") : "");
    } catch (JSONException | NullPointerException e) {
      e.printStackTrace();
      return new DecryptErr(this.ERR_BAD_RESPONSE, "DecryptResult.getDecryptErr exception: " + e.getMessage());
    }
  }

  protected void throwIfDecryptErrNotTested() {
    if (!decryptErrTested) {
      throw new Error("DecryptResult getDecryptErr() must be called before accessing data");
    }
  }

}
