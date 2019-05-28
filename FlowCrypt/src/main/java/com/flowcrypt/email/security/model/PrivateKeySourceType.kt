/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.model;

/**
 * @author DenBond7
 * Date: 13.05.2017
 * Time: 15:09
 * E-mail: DenBond7@gmail.com
 */

public enum PrivateKeySourceType {
  BACKUP("backup"),
  NEW("new"),
  IMPORT("import");

  private final String text;

  /**
   * @param text
   */
  PrivateKeySourceType(final String text) {
    this.text = text;
  }

  /* (non-Javadoc)
   * @see java.lang.Enum#toString()
   */
  @Override
  public String toString() {
    return text;
  }
}
