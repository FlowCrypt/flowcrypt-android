/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node;

import com.flowcrypt.email.api.retrofit.request.node.DecryptKeyRequest;
import com.flowcrypt.email.api.retrofit.request.node.EncryptKeyRequest;
import com.flowcrypt.email.api.retrofit.request.node.GenerateKeyRequest;
import com.flowcrypt.email.api.retrofit.request.node.GmailBackupSearchRequest;
import com.flowcrypt.email.api.retrofit.request.node.ParseKeysRequest;
import com.flowcrypt.email.api.retrofit.request.node.ZxcvbnStrengthBarRequest;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.api.retrofit.response.node.BaseNodeResult;
import com.flowcrypt.email.api.retrofit.response.node.DecryptKeyResult;
import com.flowcrypt.email.api.retrofit.response.node.EncryptKeyResult;
import com.flowcrypt.email.api.retrofit.response.node.GenerateKeyResult;
import com.flowcrypt.email.api.retrofit.response.node.GmailBackupSearchResult;
import com.flowcrypt.email.api.retrofit.response.node.ParseKeysResult;
import com.flowcrypt.email.api.retrofit.response.node.ZxcvbnStrengthBarResult;
import com.flowcrypt.email.model.PgpContact;
import com.flowcrypt.email.util.exception.NodeException;
import com.google.android.gms.common.util.CollectionUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import retrofit2.Response;

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
   * Parse a list of {@link NodeKeyDetails} from the given string. It can take one key or many keys, it can be
   * private or
   * public keys, it can be armored or binary... doesn't matter.
   *
   * @param key The given key.
   * @return A list of {@link NodeKeyDetails}
   * @throws IOException   Such exceptions can occur during network calls.
   * @throws NodeException If Node.js server will return any errors we will throw such type of errors.
   */
  public static List<NodeKeyDetails> parseKeys(String key) throws IOException, NodeException {
    NodeService service = NodeRetrofitHelper.getInstance().getRetrofit().create(NodeService.class);
    ParseKeysRequest request = new ParseKeysRequest(key);

    Response<ParseKeysResult> response = service.parseKeys(request).execute();
    ParseKeysResult result = response.body();

    checkResult(result);

    List<NodeKeyDetails> details = result.getNodeKeyDetails();

    return CollectionUtils.isEmpty(details) ? Collections.<NodeKeyDetails>emptyList() : details;
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

    Response<GmailBackupSearchResult> response = service.gmailBackupSearch(request).execute();
    GmailBackupSearchResult result = response.body();

    checkResult(result);

    return result.getQuery();
  }

  /**
   * Decrypt the given private key using an input passphrase.
   *
   * @param key        The given private key.
   * @param passphrase The given passphrase candidate.
   * @return An instance of {@link DecryptKeyResult}
   * @throws IOException   Such exceptions can occur during network calls.
   * @throws NodeException If Node.js server will return any errors we will throw such type of errors.
   */
  public static DecryptKeyResult decryptKey(String key, String passphrase) throws IOException, NodeException {
    return decryptKey(key, Collections.singletonList(passphrase));
  }

  /**
   * Decrypt the given private key using input passphrases.
   *
   * @param key         The given private key.
   * @param passphrases A list of passphrase candidates.
   * @return An instance of {@link DecryptKeyResult}
   * @throws IOException   Such exceptions can occur during network calls.
   * @throws NodeException If Node.js server will return any errors we will throw such type of errors.
   */
  public static DecryptKeyResult decryptKey(String key, List<String> passphrases) throws IOException, NodeException {
    NodeService service = NodeRetrofitHelper.getInstance().getRetrofit().create(NodeService.class);
    DecryptKeyRequest request = new DecryptKeyRequest(key, passphrases);

    Response<DecryptKeyResult> response = service.decryptKey(request).execute();
    DecryptKeyResult result = response.body();

    checkResult(result);

    return result;
  }

  /**
   * Encrypt a private key using the given passphrase.
   *
   * @param key        A private key.
   * @param passphrase The given passphrase.
   * @return An instance of {@link EncryptKeyResult}
   * @throws IOException   Such exceptions can occur during network calls.
   * @throws NodeException If Node.js server will return any errors we will throw such type of errors.
   */
  public static EncryptKeyResult encryptKey(String key, String passphrase) throws IOException, NodeException {
    NodeService service = NodeRetrofitHelper.getInstance().getRetrofit().create(NodeService.class);
    EncryptKeyRequest request = new EncryptKeyRequest(key, passphrase);

    Response<EncryptKeyResult> response = service.encryptKey(request).execute();
    EncryptKeyResult result = response.body();

    checkResult(result);

    return result;
  }

  /**
   * Generate a private key using the given parameters.
   *
   * @param passphrase  The given passphrase.
   * @param pgpContacts A list of contacts.
   * @return An instance of {@link GenerateKeyResult}
   * @throws IOException   Such exceptions can occur during network calls.
   * @throws NodeException If Node.js server will return any errors we will throw such type of errors.
   */
  public static GenerateKeyResult genKey(String passphrase, List<PgpContact> pgpContacts)
      throws IOException, NodeException {
    NodeService service = NodeRetrofitHelper.getInstance().getRetrofit().create(NodeService.class);
    GenerateKeyRequest request = new GenerateKeyRequest(passphrase, pgpContacts);

    Response<GenerateKeyResult> response = service.generateKey(request).execute();
    GenerateKeyResult result = response.body();

    checkResult(result);

    return result;
  }

  /**
   * Check the passphrase strength.
   *
   * @param guesses The given passphrase.
   * @return An instance of {@link ZxcvbnStrengthBarResult}
   * @throws IOException   Such exceptions can occur during network calls.
   * @throws NodeException If Node.js server will return any errors we will throw such type of errors.
   */
  public static ZxcvbnStrengthBarResult zxcvbnStrengthBar(double guesses) throws IOException, NodeException {
    NodeService service = NodeRetrofitHelper.getInstance().getRetrofit().create(NodeService.class);
    ZxcvbnStrengthBarRequest request = new ZxcvbnStrengthBarRequest(guesses);

    Response<ZxcvbnStrengthBarResult> response = service.zxcvbnStrengthBar(request).execute();
    ZxcvbnStrengthBarResult result = response.body();

    checkResult(result);

    return result;
  }

  private static void checkResult(BaseNodeResult result) throws NodeException {
    if (result == null) {
      throw new NullPointerException("Result is null");
    }

    if (result.getError() != null) {
      throw new NodeException(result.getError().getMsg());
    }
  }
}
