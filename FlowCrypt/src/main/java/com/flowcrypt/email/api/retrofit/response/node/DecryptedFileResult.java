package com.flowcrypt.email.api.retrofit.response.node;

import com.google.gson.annotations.Expose;

/**
 * It's a result for "decryptFile" requests.
 *
 * @author Denis Bondarenko
 * Date: 1/15/19
 * Time: 4:37 PM
 * E-mail: DenBond7@gmail.com
 */
public class DecryptedFileResult extends BaseNodeResult {
  @Expose
  private boolean success;

  @Expose
  private String name;

  public byte[] getDecryptedBytes() {
    return getData();
  }

  public boolean isSuccess() {
    return success;
  }

  public String getName() {
    return name;
  }
}
