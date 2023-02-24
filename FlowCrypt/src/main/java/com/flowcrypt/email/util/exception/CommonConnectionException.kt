/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import java.io.IOException

/**
 * This exception indicates a common connection issue
 *
 * @author Denys Bondarenko
 */
class CommonConnectionException(cause: Throwable?, val hasInternetAccess: Boolean? = null) :
  IOException(cause)
