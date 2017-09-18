/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class describes a details information about auth settings for some IMAP and SMTP servers.
 *
 * @author DenBond7
 *         Date: 14.09.2017.
 *         Time: 15:11.
 *         E-mail: DenBond7@gmail.com
 */
public class AuthCredentials implements Parcelable {
    public static final Parcelable.Creator<AuthCredentials> CREATOR = new Parcelable.Creator<AuthCredentials>() {
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
    private SecurityType imapSecurityType;
    private String smtpServer;
    private int smtpPort;
    private SecurityType smtpSecurityType;
    private boolean isUseCustomSignInForSmtp;
    private String smtpSigInUsername;
    private String smtpSignInPassword;

    public AuthCredentials() {
    }

    public AuthCredentials(String email, String username, String password, String imapServer, int imapPort,
                           SecurityType imapSecurityType, String smtpServer, int smtpPort, SecurityType
                                   smtpSecurityType, boolean isUseCustomSignInForSmtp, String smtpSigInUsername, String
                                   smtpSignInPassword) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.imapServer = imapServer;
        this.imapPort = imapPort;
        this.imapSecurityType = imapSecurityType;
        this.smtpServer = smtpServer;
        this.smtpPort = smtpPort;
        this.smtpSecurityType = smtpSecurityType;
        this.isUseCustomSignInForSmtp = isUseCustomSignInForSmtp;
        this.smtpSigInUsername = smtpSigInUsername;
        this.smtpSignInPassword = smtpSignInPassword;
    }

    protected AuthCredentials(Parcel in) {
        this.email = in.readString();
        this.username = in.readString();
        this.password = in.readString();
        this.imapServer = in.readString();
        this.imapPort = in.readInt();
        this.imapSecurityType = in.readParcelable(SecurityType.class.getClassLoader());
        this.smtpServer = in.readString();
        this.smtpPort = in.readInt();
        this.smtpSecurityType = in.readParcelable(SecurityType.class.getClassLoader());
        this.isUseCustomSignInForSmtp = in.readByte() != 0;
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
        dest.writeParcelable(this.imapSecurityType, flags);
        dest.writeString(this.smtpServer);
        dest.writeInt(this.smtpPort);
        dest.writeParcelable(this.smtpSecurityType, flags);
        dest.writeByte(this.isUseCustomSignInForSmtp ? (byte) 1 : (byte) 0);
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

    public String getImapServer() {
        return imapServer;
    }

    public int getImapPort() {
        return imapPort;
    }

    public SecurityType getImapSecurityType() {
        return imapSecurityType;
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public SecurityType getSmtpSecurityType() {
        return smtpSecurityType;
    }

    public boolean isUseCustomSignInForSmtp() {
        return isUseCustomSignInForSmtp;
    }

    public String getSmtpSigInUsername() {
        return smtpSigInUsername;
    }

    public String getSmtpSignInPassword() {
        return smtpSignInPassword;
    }

    public static class Builder {
        private String email;
        private String username;
        private String password;
        private String imapServer;
        private int imapPort;
        private SecurityType imapSecurityType;
        private String smtpServer;
        private int smtpPort;
        private SecurityType smtpSecurityType;
        private boolean isRequireSignInForSmtp;
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

        public Builder setImapSecurityType(SecurityType imapSecurityType) {
            this.imapSecurityType = imapSecurityType;
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

        public Builder setSmtpSecurityType(SecurityType smtpSecurityType) {
            this.smtpSecurityType = smtpSecurityType;
            return this;
        }

        public Builder setIsRequireSignInForSmtp(boolean isRequireSignInForSmtp) {
            this.isRequireSignInForSmtp = isRequireSignInForSmtp;
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
            return new AuthCredentials(email, username, password, imapServer, imapPort, imapSecurityType, smtpServer,
                    smtpPort, smtpSecurityType, isRequireSignInForSmtp, smtpSigInUsername, smtpSignInPassword);
        }
    }
}
