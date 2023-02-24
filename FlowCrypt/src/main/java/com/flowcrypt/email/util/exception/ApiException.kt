/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import com.flowcrypt.email.api.retrofit.response.base.ApiError

import java.io.IOException

/**
 * An API exception.
 *
 * @author Denys Bondarenko
 */
class ApiException(val apiError: ApiError?) :
  IOException("API error: code = " + apiError?.code + ", message = " + apiError?.msg)
