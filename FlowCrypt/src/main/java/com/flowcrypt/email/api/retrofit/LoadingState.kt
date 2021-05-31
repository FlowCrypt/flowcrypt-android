/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit

/**
 * @author Denis Bondarenko
 *         Date: 11/25/19
 *         Time: 10:29 AM
 *         E-mail: DenBond7@gmail.com
 */
enum class LoadingState {
  PREPARE_REQUEST,
  PREPARE_SERVICE,
  RUN_REQUEST,
  RESPONSE_RECEIVED
}
