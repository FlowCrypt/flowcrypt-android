package com.flowcrypt.email.api.retrofit.response.node;

import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * It's a base response from the Node.js server.
 *
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 11:42 AM
 * E-mail: DenBond7@gmail.com
 */
public interface BaseNodeResponse {
  void setData(byte[] data);

  void setTime(long time);

  void handleRawData(BufferedInputStream bufferedInputStream) throws IOException;
}
