/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.base;

import com.flowcrypt.email.api.email.model.SecurityType;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * The base test implementation.
 *
 * @author Denis Bondarenko
 *         Date: 26.12.2017
 *         Time: 16:37
 *         E-mail: DenBond7@gmail.com
 */

public class BaseTest {
    /**
     * Match the {@link SecurityType.Option}.
     *
     * @param option An input {@link SecurityType.Option}.
     */
    public static <T> Matcher<T> matchOption(final SecurityType.Option option) {
        return new BaseMatcher<T>() {
            @Override
            public boolean matches(Object item) {
                if (item instanceof SecurityType) {
                    SecurityType securityType = (SecurityType) item;
                    return securityType.getOption() == option;
                } else {
                    return false;
                }

            }

            @Override
            public void describeTo(Description description) {
                description.appendText("The input option = " + option);
            }
        };
    }
}
