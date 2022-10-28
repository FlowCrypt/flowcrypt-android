/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model

import com.google.gson.annotations.Expose

/**
 * The request model for the https://flowcrypt.com/api/account/login(or get) API.
 *
 * @author Denis Bondarenko
 *         Date: 10/23/19
 *         Time: 3:53 PM
 *         E-mail: DenBond7@gmail.com
 */
data class LoginModel(@Expose val account: String)
