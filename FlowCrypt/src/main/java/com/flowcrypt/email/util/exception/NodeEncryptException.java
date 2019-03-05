/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception;

/**
 * This exception can occur during the encryption process.
 *
 * @author Denis Bondarenko
 * Date: 1/25/19
 * Time: 6:34 PM
 * E-mail: DenBond7@gmail.com
 */
public class NodeEncryptException extends NodeException {
  public NodeEncryptException(String message) {
    super(message);
  }
}
