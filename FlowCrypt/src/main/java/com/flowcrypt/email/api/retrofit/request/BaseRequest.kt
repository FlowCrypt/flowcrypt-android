/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request

import com.flowcrypt.email.api.retrofit.ApiName

/**
 * The base class for all requests.
 *
 * @author Denis Bondarenko
 * Date: 10.03.2015
 * Time: 13:41
 * E-mail: DenBond7@gmail.com
 */
//todo-denbond7 It's old code. Need to remove this after refactoring
interface BaseRequest<T> {
  val apiName: ApiName

  val requestModel: T
}
