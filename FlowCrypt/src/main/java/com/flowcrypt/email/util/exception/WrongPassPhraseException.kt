/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

/**
 * @author Denys Bondarenko
 */
class WrongPassPhraseException(message: String, cause: Throwable) :
  FlowCryptException(message, cause)
