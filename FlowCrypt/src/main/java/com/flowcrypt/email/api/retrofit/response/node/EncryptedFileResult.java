package com.flowcrypt.email.api.retrofit.response.node;

/**
 * It's a result for "encryptFile" requests.
 *
 * @author Denis Bondarenko
 * Date: 1/15/19
 * Time: 8:59 AM
 * E-mail: DenBond7@gmail.com
 */
public class EncryptedFileResult extends BaseNodeResult {
  public byte[] getEncryptedBytes() {
    return getData();
  }
}
