/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception;

import com.flowcrypt.email.api.retrofit.response.base.ApiError;

import java.io.IOException;

/**
 * An API exception.
 *
 * @author Denis Bondarenko
 *         Date: 30.01.2018
 *         Time: 18:39
 *         E-mail: DenBond7@gmail.com
 */

public class ApiException extends IOException {
    public ApiException(ApiError apiError) {
        super("API error: code = " + apiError.getCode() + ", message = " + apiError.getMessage());
    }
}
