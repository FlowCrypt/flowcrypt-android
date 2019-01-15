package com.flowcrypt.email.api.retrofit.response.node;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * It's a result for "encryptMsg" requests.
 *
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 12:51 PM
 * E-mail: DenBond7@gmail.com
 */
public class EncryptedMsgResult extends BaseNodeResult {

  public final String getEncryptedMsg() {
    byte[] bytes = getData();

    if (bytes == null) {
      return "";
    }
    try {
      return IOUtils.toString(bytes, StandardCharsets.UTF_8.displayName());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "";
  }
}
