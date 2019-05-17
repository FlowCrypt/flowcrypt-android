/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The request model for the https://flowcrypt.com/api/message/reply API.
 *
 * @author Denis Bondarenko
 * Date: 13.07.2017
 * Time: 16:32
 * E-mail: DenBond7@gmail.com
 */

public class MessageReplyModel extends BaseRequestModel {

  @SerializedName("short")
  @Expose
  private String shortValue;

  @SerializedName("token")
  @Expose
  private String token;

  @SerializedName("message")
  @Expose
  private String message;

  @SerializedName("subject")
  @Expose
  private String subject;

  @SerializedName("from")
  @Expose
  private String from;

  @SerializedName("to")
  @Expose
  private String to;

  public MessageReplyModel(String shortValue, String token, String message, String subject,
                           String from, String to) {
    this.shortValue = shortValue;
    this.token = token;
    this.message = message;
    this.subject = subject;
    this.from = from;
    this.to = to;
  }

  public String getShortValue() {
    return shortValue;
  }

  public String getToken() {
    return token;
  }

  public String getMessage() {
    return message;
  }

  public String getSubject() {
    return subject;
  }

  public String getFrom() {
    return from;
  }

  public String getTo() {
    return to;
  }
}
