/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

/**
 * It's an exception which should be sent via ACRA reporter
 *
 * @author Denys Bondarenko
 */
class ForceHandlingException(e: Exception) : Exception(e)
