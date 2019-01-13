package com.flowcrypt.email.api.retrofit.request.node;

/**
 * This {@linkplain BaseNodeRequest request} can be used to receive information about a version.
 *
 * @author Denis Bondarenko
 * Date: 1/10/19
 * Time: 5:46 PM
 * E-mail: DenBond7@gmail.com
 */
public final class VersionRequest implements BaseNodeRequest {

  @Override
  public String getEndpoint() {
    return "version";
  }

  @Override
  public byte[] getData() {
    return null;
  }
}
