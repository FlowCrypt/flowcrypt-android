/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers;

import android.view.View;

import org.hamcrest.Matcher;

/**
 * @author Denis Bondarenko
 * Date: 3/15/19
 * Time: 5:20 PM
 * E-mail: DenBond7@gmail.com
 */
public class CustomMatchers {

  public static Matcher<View> withDrawable(final int resourceId) {
    return new DrawableMatcher(resourceId);
  }

  public static Matcher<View> emptyDrawable() {
    return new DrawableMatcher(-1);
  }
}
