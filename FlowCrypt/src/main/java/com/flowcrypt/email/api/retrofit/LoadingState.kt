/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit

/**
 * @author Denys Bondarenko
 */
enum class LoadingState {
  PREPARE_REQUEST,
  PREPARE_SERVICE,
  RUN_REQUEST,
  RESPONSE_RECEIVED
}
