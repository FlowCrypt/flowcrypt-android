/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

/**
 * @author Denis Bondarenko
 *         Date: 4/15/21
 *         Time: 3:41 PM
 *         E-mail: DenBond7@gmail.com
 */
class WrongPassPhraseException(message: String, cause: Throwable) : FlowCryptException(message, cause)