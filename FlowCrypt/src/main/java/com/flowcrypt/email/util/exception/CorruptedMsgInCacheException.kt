/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import java.io.IOException

/**
 * This exception can happen when we try to use a snapshot of a corrupted message.
 *
 * @author Denys Bondarenko
 */
class CorruptedMsgInCacheException : IOException()
