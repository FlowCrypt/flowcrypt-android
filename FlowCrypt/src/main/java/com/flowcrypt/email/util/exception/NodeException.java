/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception;

/**
 * It's a base Node exception.
 *
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 10:08 AM
 * E-mail: DenBond7@gmail.com
 */
public class NodeException extends FlowCryptException {
  public NodeException(String message) {
    super(message);
  }
}
