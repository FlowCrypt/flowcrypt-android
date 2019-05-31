/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source;

/**
 * This object describes information about an account alias.
 *
 * @author Denis Bondarenko
 * Date: 26.10.2017
 * Time: 16:04
 * E-mail: DenBond7@gmail.com
 */

public class AccountAliasesDao {

  private String email;
  private String accountType;
  private String sendAsEmail;
  private String displayName;
  private boolean isDefault;
  private String verificationStatus;

  public AccountAliasesDao() {
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getAccountType() {
    return accountType;
  }

  public void setAccountType(String accountType) {
    this.accountType = accountType;
  }

  public String getSendAsEmail() {
    return sendAsEmail;
  }

  public void setSendAsEmail(String sendAsEmail) {
    this.sendAsEmail = sendAsEmail;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public void setDefault(boolean aDefault) {
    isDefault = aDefault;
  }

  public String getVerificationStatus() {
    return verificationStatus;
  }

  public void setVerificationStatus(String verificationStatus) {
    this.verificationStatus = verificationStatus;
  }
}
