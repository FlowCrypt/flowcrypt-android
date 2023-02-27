/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

/**
 * @author Denys Bondarenko
 */
class EmptyPassphraseException(val fingerprints: List<String>, message: String) :
  FlowCryptException(message)
