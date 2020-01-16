/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit

/**
 * This class describes all available states for API calls which are making by [retrofit2.Retrofit]
 *
 * @author Denis Bondarenko
 * Date: 2/15/19
 * Time: 2:54 PM
 * E-mail: DenBond7@gmail.com
 */
enum class Status {
  SUCCESS,
  ERROR,
  EXCEPTION,
  LOADING
}
