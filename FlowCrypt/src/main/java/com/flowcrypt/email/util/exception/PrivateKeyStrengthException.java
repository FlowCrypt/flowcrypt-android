/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception;

/**
 * @author Denis Bondarenko
 * Date: 10.08.2018
 * Time: 9:39
 * E-mail: DenBond7@gmail.com
 */
public class PrivateKeyStrengthException extends FlowCryptException {
  public PrivateKeyStrengthException() {
  }

  public PrivateKeyStrengthException(String message) {
    super(message);
  }

  public PrivateKeyStrengthException(String message, Throwable cause) {
    super(message, cause);
  }

  public PrivateKeyStrengthException(Throwable cause) {
    super(cause);
  }
}
