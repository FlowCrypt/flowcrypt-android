/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.attester

import com.flowcrypt.email.api.retrofit.ApiName
import com.flowcrypt.email.api.retrofit.request.BaseRequest
import com.flowcrypt.email.api.retrofit.request.model.TestWelcomeModel

/**
 * This class describes a request to the https://flowcrypt.com/attester/test/welcome API.
 *
 *
 * `POST /test/welcome  {
 * "email" (<type></type>'str'>)  # email to send a welcome to
 * "pubkey" (<type></type>'str'>)  # ascii armored pubkey to encrypt welcome message for
 * }`
 *
 * @author Denis Bondarenko
 * Date: 12.07.2017
 * Time: 16:39
 * E-mail: DenBond7@gmail.com
 */

class TestWelcomeRequest(override val apiName: ApiName = ApiName.POST_TEST_WELCOME,
                         override val requestModel: TestWelcomeModel) : BaseRequest<TestWelcomeModel>

