/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.attester

import com.flowcrypt.email.api.retrofit.ApiName
import com.flowcrypt.email.api.retrofit.request.BaseRequest
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel

/**
 * This class describes a request to the https://flowcrypt.com/attester/initial/legacy_submit API.
 *
 *
 * `POST /initial/legacy_submit  {
 * "email" (<type></type>'str'>)  # email to use pubkey for
 * "pubkey" (<type></type>'str'>)  # ascii armored pubkey
 * [voluntary] "attest" (True, False)  # send attestation email
 * }`
 *
 * @author Denis Bondarenko
 * Date: 15.01.2018
 * Time: 16:42
 * E-mail: DenBond7@gmail.com
 */

class InitialLegacySubmitRequest(override val apiName: ApiName = ApiName.POST_INITIAL_LEGACY_SUBMIT,
                                 override val requestModel: InitialLegacySubmitModel) : BaseRequest<InitialLegacySubmitModel>
