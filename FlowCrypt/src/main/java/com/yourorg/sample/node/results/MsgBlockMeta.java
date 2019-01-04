package com.yourorg.sample.node.results;

public class MsgBlockMeta {
  public final long length;
  public final String type;

  MsgBlockMeta(String type, long length) {
    this.type = type;
    this.length = length;
  }
}
