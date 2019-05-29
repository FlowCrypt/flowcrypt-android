/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import com.flowcrypt.email.api.retrofit.response.base.ApiError

import java.io.IOException

/**
 * An API exception.
 *
 * @author Denis Bondarenko
 * Date: 30.01.2018
 * Time: 18:39
 * E-mail: DenBond7@gmail.com
 */

class ApiException(apiError: ApiError) :
    IOException("API error: code = " + apiError.code + ", message = " + apiError.msg)
