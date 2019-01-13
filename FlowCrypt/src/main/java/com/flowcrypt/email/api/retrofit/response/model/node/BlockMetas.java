package com.flowcrypt.email.api.retrofit.response.model.node;

import com.google.gson.annotations.Expose;

/**
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 4:08 PM
 * E-mail: DenBond7@gmail.com
 */
public class BlockMetas {
  @Expose
  private String type;

  @Expose
  private int length;

  public BlockMetas(String type, int length) {
    this.type = type;
    this.length = length;
  }

  public String getType() {
    return type;
  }

  public int getLength() {
    return length;
  }
}
