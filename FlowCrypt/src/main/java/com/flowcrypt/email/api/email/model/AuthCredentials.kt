/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class describes a details information about auth settings for some IMAP and SMTP servers.
 *
 * @author DenBond7
 * Date: 14.09.2017.
 * Time: 15:11.
 * E-mail: DenBond7@gmail.com
 */
public class AuthCredentials implements Parcelable {

  public static final Creator<AuthCredentials> CREATOR = new Creator<AuthCredentials>() {
    @Override
    public AuthCredentials createFromParcel(Parcel source) {
      return new AuthCredentials(source);
    }

    @Override
    public AuthCredentials[] newArray(int size) {
      return new AuthCredentials[size];
    }
  };
  private String email;
  private String username;
  private String password;
  private String imapServer;
  private int imapPort;
  private SecurityType.Option imapOpt;
  private String smtpServer;
  private int smtpPort;
  private SecurityType.Option smtpOpt;
  private boolean hasCustomSignInForSmtp;
  private String smtpSigInUsername;
  private String smtpSignInPassword;

  public AuthCredentials() {
  }

  public AuthCredentials(String email, String username, String password, String imapServer, int imapPort,
                         SecurityType.Option imapOpt, String smtpServer, int smtpPort,
                         SecurityType.Option smtpSecurityType, boolean hasCustomSignInForSmtp,
                         String smtpSigInUsername, String smtpSignInPassword) {
    this.email = email;
    this.username = username;
    this.password = password;
    this.imapServer = imapServer;
    this.imapPort = imapPort;
    this.imapOpt = imapOpt;
    this.smtpServer = smtpServer;
    this.smtpPort = smtpPort;
    this.smtpOpt = smtpSecurityType;
    this.hasCustomSignInForSmtp = hasCustomSignInForSmtp;
    this.smtpSigInUsername = smtpSigInUsername;
    this.smtpSignInPassword = smtpSignInPassword;
  }

  protected AuthCredentials(Parcel in) {
    this.email = in.readString();
    this.username = in.readString();
    this.password = in.readString();
    this.imapServer = in.readString();
    this.imapPort = in.readInt();
    int tmpImapSecurityTypeOpt = in.readInt();
    this.imapOpt = tmpImapSecurityTypeOpt == -1 ? null : SecurityType.Option.values()
        [tmpImapSecurityTypeOpt];
    this.smtpServer = in.readString();
    this.smtpPort = in.readInt();
    int tmpSmtpSecurityTypeOpt = in.readInt();
    this.smtpOpt = tmpSmtpSecurityTypeOpt == -1 ? null : SecurityType.Option.values()
        [tmpSmtpSecurityTypeOpt];
    this.hasCustomSignInForSmtp = in.readByte() != 0;
    this.smtpSigInUsername = in.readString();
    this.smtpSignInPassword = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.email);
    dest.writeString(this.username);
    dest.writeString(this.password);
    dest.writeString(this.imapServer);
    dest.writeInt(this.imapPort);
    dest.writeInt(this.imapOpt == null ? -1 : this.imapOpt.ordinal());
    dest.writeString(this.smtpServer);
    dest.writeInt(this.smtpPort);
    dest.writeInt(this.smtpOpt == null ? -1 : this.smtpOpt.ordinal());
    dest.writeByte(this.hasCustomSignInForSmtp ? (byte) 1 : (byte) 0);
    dest.writeString(this.smtpSigInUsername);
    dest.writeString(this.smtpSignInPassword);
  }

  public String getEmail() {
    return email;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getImapServer() {
    return imapServer;
  }

  public int getImapPort() {
    return imapPort;
  }

  public SecurityType.Option getImapOpt() {
    return imapOpt;
  }

  public SecurityType.Option getSmtpOpt() {
    return smtpOpt;
  }

  public String getSmtpServer() {
    return smtpServer;
  }

  public int getSmtpPort() {
    return smtpPort;
  }

  public boolean hasCustomSignInForSmtp() {
    return hasCustomSignInForSmtp;
  }

  public String getSmtpSigInUsername() {
    return smtpSigInUsername;
  }

  public String getSmtpSignInPassword() {
    return smtpSignInPassword;
  }

  public void setSmtpSignInPassword(String smtpSignInPassword) {
    this.smtpSignInPassword = smtpSignInPassword;
  }

  public static class Builder {
    private String email;
    private String username;
    private String password;
    private String imapServer;
    private int imapPort;
    private SecurityType.Option imapSecurityTypeOpt;
    private String smtpServer;
    private int smtpPort;
    private SecurityType.Option smtpSecurityTypeOpt;
    private boolean hasCustomSignInForSmtp;
    private String smtpSigInUsername;
    private String smtpSignInPassword;

    public Builder setEmail(String email) {
      this.email = email;
      return this;
    }

    public Builder setUsername(String username) {
      this.username = username;
      return this;
    }

    public Builder setPassword(String password) {
      this.password = password;
      return this;
    }

    public Builder setImapServer(String imapServer) {
      this.imapServer = imapServer;
      return this;
    }

    public Builder setImapPort(int imapPort) {
      this.imapPort = imapPort;
      return this;
    }

    public Builder setImapSecurityTypeOpt(SecurityType.Option imapSecurityTypeOpt) {
      this.imapSecurityTypeOpt = imapSecurityTypeOpt;
      return this;
    }

    public Builder setSmtpServer(String smtpServer) {
      this.smtpServer = smtpServer;
      return this;
    }

    public Builder setSmtpPort(int smtpPort) {
      this.smtpPort = smtpPort;
      return this;
    }

    public Builder setSmtpSecurityTypeOpt(SecurityType.Option smtpSecurityTypeOpt) {
      this.smtpSecurityTypeOpt = smtpSecurityTypeOpt;
      return this;
    }

    public Builder setIsUseCustomSignInForSmtp(boolean hasCustomSignInForSmtp) {
      this.hasCustomSignInForSmtp = hasCustomSignInForSmtp;
      return this;
    }

    public Builder setSmtpSigInUsername(String smtpSigInUsername) {
      this.smtpSigInUsername = smtpSigInUsername;
      return this;
    }

    public Builder setSmtpSignInPassword(String smtpSignInPassword) {
      this.smtpSignInPassword = smtpSignInPassword;
      return this;
    }

    public AuthCredentials build() {
      return new AuthCredentials(email, username, password, imapServer, imapPort, imapSecurityTypeOpt,
          smtpServer, smtpPort, smtpSecurityTypeOpt, hasCustomSignInForSmtp, smtpSigInUsername,
          smtpSignInPassword);
    }
  }
}
