/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration

/**
 * @author Denys Bondarenko
 */
class EkmNotSupportedException(val clientConfiguration: ClientConfiguration) : FlowCryptException()
