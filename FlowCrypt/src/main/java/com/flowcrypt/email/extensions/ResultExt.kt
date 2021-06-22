/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import com.flowcrypt.email.api.retrofit.response.base.Result

/**
 * @author Denis Bondarenko
 *         Date: 6/15/21
 *         Time: 3:46 PM
 *         E-mail: DenBond7@gmail.com
 */
val <T> Result<T>.exceptionMsg: String
  get() {
    return exception?.message
      ?: exception?.cause?.message
      ?: "Unknown error"
  }
