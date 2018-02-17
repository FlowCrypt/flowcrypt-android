/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.base;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.support.design.widget.AppBarLayout;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.view.View;
import android.widget.Toast;

import com.flowcrypt.email.api.email.model.SecurityType;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

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

    /**
     * Match a color in the {@link AppBarLayout}.
     *
     * @param color An input color value.
     * @return true if matched, otherwise false
     */
    public static Matcher<View> matchAppBarLayoutBackgroundColor(final int color) {
        return new BoundedMatcher<View, AppBarLayout>(AppBarLayout.class) {
            @Override
            public boolean matchesSafely(AppBarLayout appBarLayout) {
                return color == ((ColorDrawable) appBarLayout.getBackground()).getColor();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Background color AppBarLayout: " + color);
            }
        };
    }

    /**
     * Check is {@link Toast} displaying.
     *
     * @param activity A root {@link Activity}
     * @param message  A message which was displayed.
     */
    public static void checkIsToastDisplayed(Activity activity, String message) {
        onView(withText(message))
                .inRoot(withDecorView(not(is(activity.getWindow().getDecorView()))))
                .check(matches(isDisplayed()));
    }
}
