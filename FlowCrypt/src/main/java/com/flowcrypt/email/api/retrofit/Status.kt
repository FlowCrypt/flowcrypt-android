/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit

/**
 * This class describes all available states for API calls which are making by [retrofit2.Retrofit]
 *
 * @author Denys Bondarenko
 */
enum class Status {
  SUCCESS,
  ERROR,
  EXCEPTION,
  LOADING
}
