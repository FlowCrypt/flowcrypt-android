package com.yourorg.sample.node.results;

public class DecryptErr {

  public final String type;
  public final String error;

  DecryptErr(String type, String error) {
    this.type = type;
    this.error = error;
  }

}
