/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

/**
 * It's an exception which should be sent via ACRA reporter
 *
 * @author Denis Bondarenko
 *         Date: 6/25/20
 *         Time: 3:44 PM
 *         E-mail: DenBond7@gmail.com
 */
class ForceHandlingException(e: Exception) : Exception(e)