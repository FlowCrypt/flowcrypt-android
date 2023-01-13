/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration

/**
 * @author Denis Bondarenko
 *         Date: 6/22/21
 *         Time: 5:53 PM
 *         E-mail: DenBond7@gmail.com
 */
class EkmNotSupportedException(val clientConfiguration: ClientConfiguration) : FlowCryptException()
