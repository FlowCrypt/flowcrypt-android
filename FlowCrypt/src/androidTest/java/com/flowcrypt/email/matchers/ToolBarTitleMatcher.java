/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers;

import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

import androidx.appcompat.widget.Toolbar;
import androidx.test.espresso.matcher.BoundedMatcher;

import static com.google.android.gms.common.internal.Preconditions.checkNotNull;
import static org.hamcrest.CoreMatchers.is;

/**
 * @author Denis Bondarenko
 * Date: 16.08.2018
 * Time: 11:14
 * E-mail: DenBond7@gmail.com
 */
public class ToolBarTitleMatcher extends BoundedMatcher<View, Toolbar> {

  private final Matcher<String> textMatcher;

  public ToolBarTitleMatcher(Matcher<String> textMatcher) {
    super(Toolbar.class);
    this.textMatcher = textMatcher;
  }

  public static Matcher<View> withText(String textMatcher) {
    return new ToolBarTitleMatcher(checkNotNull(is(textMatcher)));
  }

  @Override
  protected boolean matchesSafely(Toolbar toolbar) {
    System.out.println("toolbar = [" + toolbar.getTitle() + "]");
    return textMatcher.matches(toolbar.getTitle());
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("with toolbar title: ");
    textMatcher.describeTo(description);
  }
}
