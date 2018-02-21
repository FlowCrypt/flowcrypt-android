/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.attester;

import com.flowcrypt.email.api.retrofit.ApiName;
import com.flowcrypt.email.api.retrofit.request.BaseRequest;
import com.flowcrypt.email.api.retrofit.request.model.TestWelcomeModel;

/**
 * This class describes a request to the https://attester.flowcrypt.com/test/welcome API.
 * <p>
 * <code>POST /test/welcome  {
 * "email" (<type 'str'>)  # email to send a welcome to
 * "pubkey" (<type 'str'>)  # ascii armored pubkey to encrypt welcome message for
 * }</code>
 *
 * @author Denis Bondarenko
 *         Date: 12.07.2017
 *         Time: 16:39
 *         E-mail: DenBond7@gmail.com
 */

public class TestWelcomeRequest extends BaseRequest<TestWelcomeModel> {
    public TestWelcomeRequest(TestWelcomeModel testWelcomeModel) {
        super(ApiName.POST_TEST_WELCOME, testWelcomeModel);
    }
}
