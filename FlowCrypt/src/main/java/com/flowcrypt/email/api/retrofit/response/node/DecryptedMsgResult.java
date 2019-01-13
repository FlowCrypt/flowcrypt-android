package com.flowcrypt.email.api.retrofit.response.node;

import com.flowcrypt.email.api.retrofit.response.model.node.BlockMetas;
import com.google.gson.annotations.Expose;

import java.util.List;

/**
 * It's a result for "decryptMsg" requests.
 *
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 3:48 PM
 * E-mail: DenBond7@gmail.com
 */
public class DecryptedMsgResult extends BaseNodeResult {

  @Expose
  private boolean success;

  @Expose
  private List<BlockMetas> blockMetas;

  public boolean isSuccess() {
    return success;
  }

  public List<BlockMetas> getBlockMetas() {
    return blockMetas;
  }
}
