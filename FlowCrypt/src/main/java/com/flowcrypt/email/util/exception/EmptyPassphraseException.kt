/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

/**
 * @author Denis Bondarenko
 *         Date: 6/14/21
 *         Time: 11:55 AM
 *         E-mail: DenBond7@gmail.com
 */
class EmptyPassphraseException(val fingerprints: List<String>, message: String) :
  FlowCryptException(message)
