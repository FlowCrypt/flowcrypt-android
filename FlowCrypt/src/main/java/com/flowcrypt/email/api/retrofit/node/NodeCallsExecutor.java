/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node;

import com.flowcrypt.email.api.retrofit.request.node.GmailBackupSearchRequest;
import com.flowcrypt.email.api.retrofit.request.node.ParseKeysRequest;
import com.flowcrypt.email.api.retrofit.response.model.node.KeyDetails;
import com.flowcrypt.email.api.retrofit.response.node.GmailBackupSearchResult;
import com.flowcrypt.email.api.retrofit.response.node.ParseKeysResult;
import com.flowcrypt.email.util.exception.NodeException;

import java.io.IOException;
import java.util.List;

/**
 * It's an utility class which contains only static methods. Don't call this methods in the UI thread.
 *
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 2:54 PM
 * E-mail: DenBond7@gmail.com
 */
public class NodeCallsExecutor {
  /**
   * Parse a list of {@link KeyDetails} from the given string. It can take one key or many keys, it can be private or
   * public keys, it can be armored or binary... doesn't matter.
   *
   * @param key The given key.
   * @return A list of {@link KeyDetails}
   * @throws IOException   Such exceptions can occur during network calls.
   * @throws NodeException If Node.js server will return any errors we will throw such type of errors.
   */
  public static List<KeyDetails> parseKeys(String key) throws IOException, NodeException {
    NodeService service = NodeRetrofitHelper.getInstance().getRetrofit().create(NodeService.class);
    ParseKeysRequest request = new ParseKeysRequest(key);

    retrofit2.Response<ParseKeysResult> response = service.parseKeys(request).execute();
    ParseKeysResult result = response.body();

    if (result == null) {
      throw new NullPointerException("ParseKeysResult == null");
    }

    if (result.getError() != null) {
      throw new NodeException(result.getError().getMsg());
    }

    return result.getKeyDetails();
  }

  /**
   * Get a special string which contains formatted template for the native Gmail search.
   *
   * @param email The account email.
   * @return A formatted template for the native Gmail search
   * @throws IOException   Such exceptions can occur during network calls.
   * @throws NodeException If Node.js server will return any errors we will throw such type of errors.
   */
  public static String getGmailBackupSearch(String email) throws IOException, NodeException {
    NodeService service = NodeRetrofitHelper.getInstance().getRetrofit().create(NodeService.class);
    GmailBackupSearchRequest request = new GmailBackupSearchRequest(email);

    retrofit2.Response<GmailBackupSearchResult> response = service.gmailBackupSearch(request).execute();
    GmailBackupSearchResult result = response.body();

    if (result == null) {
      throw new NullPointerException("GmailBackupSearchResult == null");
    }

    if (result.getError() != null) {
      throw new NodeException(result.getError().getMsg());
    }

    return result.getQuery();
  }


}
