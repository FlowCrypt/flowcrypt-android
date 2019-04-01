/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node;

import com.flowcrypt.email.api.retrofit.node.NodeService;
import com.flowcrypt.email.js.PgpContact;
import com.google.gson.annotations.Expose;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

/**
 * Using this class we can create a request to create a new key with the given parameters.
 *
 * @author Denis Bondarenko
 * Date: 4/1/19
 * Time: 9:44 AM
 * E-mail: DenBond7@gmail.com
 */
public class GenerateKeyRequest extends BaseNodeRequest {
  private static final String DEFAULT_KEY_VARIANT = "rsa2048";

  @Expose
  private String variant;

  @Expose
  private String passphrase;

  @Expose
  private List<UserId> userIds;

  public GenerateKeyRequest(String passphrase, List<PgpContact> pgpContacts) {
    this.variant = DEFAULT_KEY_VARIANT;
    this.passphrase = passphrase;
    this.userIds = new ArrayList<>();
    for (PgpContact pgpContact : pgpContacts) {
      userIds.add(new UserId(pgpContact.getEmail(), pgpContact.getName()));
    }
  }

  @Override
  public String getEndpoint() {
    return "generateKey";
  }

  @Override
  public Response getResponse(NodeService nodeService) throws IOException {
    if (nodeService != null) {
      return nodeService.generateKey(this).execute();
    } else return null;
  }

  private static class UserId {
    @Expose
    private String email;

    @Expose
    private String name;

    UserId(String email, String name) {
      this.email = email;
      this.name = name;
    }

    public String getEmail() {
      return email;
    }

    public String getName() {
      return name;
    }
  }
}
