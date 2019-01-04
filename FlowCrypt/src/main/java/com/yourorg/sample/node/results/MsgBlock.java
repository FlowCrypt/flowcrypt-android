package com.yourorg.sample.node.results;

public class MsgBlock {

  //  type KeyDetails$ids = {
  //      longid: string;
  //    fingerprint: string;
  //    keywords: string;
  //    };
  //    export interface KeyDetails {
  //  private?: string;
  //  public: string;
  //  ids: KeyDetails$ids[];
  //  users: string[];
  //}
  // AttMeta: { name: att.name }

  public static final String TYPE_TEXT = "text";
  public static final String TYPE_PGP_MESSAGE = "message";
  public static final String TYPE_PGP_PUBLIC_KEY = "public_key";
  public static final String TYPE_PGP_SIGNED_MESSAGE = "signed_message";
  public static final String TYPE_PGP_PASSWORD_MESSAGE = "password_message";
  public static final String TYPE_ATTEST_PACKET = "attest_packet";
  public static final String TYPE_VERIFICATION = "cryptup_verification";
  public static final String TYPE_PGP_PRIVATE_KEY = "private_key";
  public static final String TYPE_ATTACHMENT = "attachment";
  public static final String TYPE_HTML = "html";

  private String type;
  private String content;

  public MsgBlock(String type, String content) { // todo: add keyDetails, attMeta
    this.type = type;
    this.content = content;
  }

  public String getType() {
    return type;
  }

  public String getContent() {
    return content;
  }

}
