/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception;

/**
 * @author Denis Bondarenko
 * Date: 10.08.2018
 * Time: 12:51
 * E-mail: DenBond7@gmail.com
 */
public class DifferentPassPhrasesException extends FlowCryptException {
    public DifferentPassPhrasesException() {
    }

    public DifferentPassPhrasesException(String message) {
        super(message);
    }

    public DifferentPassPhrasesException(String message, Throwable cause) {
        super(message, cause);
    }

    public DifferentPassPhrasesException(Throwable cause) {
        super(cause);
    }
}
