package com.flowcrypt.email.api.retrofit.response.node;

import com.flowcrypt.email.api.retrofit.node.NodeGson;
import com.flowcrypt.email.api.retrofit.response.model.node.BlockMetas;
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock;
import com.google.gson.annotations.Expose;

import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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

  private List<MsgBlock> msgBlocks;

  public DecryptedMsgResult() {
    this.msgBlocks = new ArrayList<>();
  }

  @Override
  public void handleRawData(BufferedInputStream bufferedInputStream) {
    //todo-denbond7 Currently we peek only the first block. Need to fix that
    MsgBlock block = NodeGson.getInstance().getGson().fromJson(new InputStreamReader(bufferedInputStream),
        MsgBlock.class);

    if (block != null) {
      msgBlocks.add(block);
    }
  }

  public boolean isSuccess() {
    return success;
  }

  public List<BlockMetas> getBlockMetas() {
    return blockMetas;
  }

  public List<MsgBlock> getMsgBlocks() {
    return msgBlocks;
  }
}
