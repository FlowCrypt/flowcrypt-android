/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import java.io.IOException

/**
 * This exception indicates a common connection issue
 *
 * @author Denis Bondarenko
 *         Date: 12/3/20
 *         Time: 5:01 PM
 *         E-mail: DenBond7@gmail.com
 */
class CommonConnectionException(cause: Throwable?) : IOException(cause)
