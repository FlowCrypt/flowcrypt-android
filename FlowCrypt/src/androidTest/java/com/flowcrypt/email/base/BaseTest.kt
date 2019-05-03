/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.base;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Html;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.base.BaseActivity;
import com.google.android.material.snackbar.Snackbar;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * The base test implementation.
 *
 * @author Denis Bondarenko
 * Date: 26.12.2017
 * Time: 16:37
 * E-mail: DenBond7@gmail.com
 */
@RunWith(AndroidJUnit4.class)
public abstract class BaseTest {

  public abstract ActivityTestRule getActivityTestRule();

  public static Context getTargetContext() {
    return InstrumentationRegistry.getInstrumentation().getTargetContext();
  }

  public static Context getContext() {
    return InstrumentationRegistry.getInstrumentation().getContext();
  }

  @Before
  public void registerNodeIdling() {
    ActivityTestRule activityTestRule = getActivityTestRule();
    if (activityTestRule != null) {
      Activity activity = activityTestRule.getActivity();
      if (activity instanceof BaseActivity) {
        IdlingRegistry.getInstance().register(((BaseActivity) activity).getNodeIdlingResource());
      }
    }
  }

  @After
  public void unregisterNodeIdling() {
    ActivityTestRule activityTestRule = getActivityTestRule();
    if (activityTestRule != null) {
      Activity activity = activityTestRule.getActivity();
      if (activity instanceof BaseActivity) {
        IdlingRegistry.getInstance().unregister(((BaseActivity) activity).getNodeIdlingResource());
      }
    }
  }

  /**
   * Check is {@link Toast} displaying.
   *
   * @param activity A root {@link Activity}
   * @param message  A message which was displayed.
   */
  protected void checkIsToastDisplayed(Activity activity, String message) {
    onView(withText(message)).inRoot(withDecorView(not(is(activity.getWindow().getDecorView())))).check(matches(isDisplayed()));
  }

  /**
   * Test the app help screen.
   */
  protected void testHelpScreen() {
    onView(withId(R.id.menuActionHelp)).check(matches(isDisplayed())).perform(click());
    onView(withId(R.id.textViewAuthorHint)).check(matches(isDisplayed()))
        .check(matches(withText(R.string.i_will_usually_reply_within_an_hour_except_when_i_sleep_tom)));
    onView(withText(R.string.help_feedback_or_question)).check(matches(isDisplayed()));
  }

  /**
   * Test is a {@link Snackbar} with an input message displayed.
   *
   * @param message An input message.
   */
  protected void checkIsSnackbarDisplayedAndClick(String message) {
    onView(withText(message)).check(matches(isDisplayed()));
    onView(withId(com.google.android.material.R.id.snackbar_action)).check(matches(isDisplayed()))
        .perform(click());
  }

  /**
   * Test is a {@link Snackbar} displayed.
   */
  protected void checkIsSnackBarDisplayed() {
    onView(withId(com.google.android.material.R.id.snackbar_action)).check(matches(isDisplayed()));
  }

  /**
   * Test is a {@link Snackbar} not displayed.
   */
  protected void checkIsSnackBarNotDisplayed() {
    onView(withId(com.google.android.material.R.id.snackbar_action)).check(doesNotExist());
  }

  /**
   * Add some text to the {@link ClipboardManager}
   *
   * @param label The clipboard data label.
   * @param text  The text which will be added to the clipboard.
   * @throws Throwable
   */
  protected void addTextToClipboard(final String label, final String text) throws Throwable {
    runOnUiThread(new Runnable() {
      public void run() {
        ClipboardManager clipboard = (ClipboardManager) getTargetContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        if (clipboard != null) {
          clipboard.setPrimaryClip(clip);
        }
      }
    });
  }

  protected void checkClipboardText(final CharSequence text) {
    ClipboardManager clipboardManager =
        (ClipboardManager) getTargetContext().getSystemService(Context.CLIPBOARD_SERVICE);
    CharSequence clipboardText = null;
    if (clipboardManager.getPrimaryClip() != null && clipboardManager.getPrimaryClip().getItemCount() > 0) {
      ClipData.Item item = clipboardManager.getPrimaryClip().getItemAt(0);
      clipboardText = item.getText();
    }
    assertThat(clipboardText, Matchers.<CharSequence>hasToString(text.toString()));
  }

  protected String getResString(int resId) {
    return getTargetContext().getString(resId);
  }

  protected String getResString(int resId, Object... formatArgs) {
    return getTargetContext().getString(resId, formatArgs);
  }

  protected String getHtmlString(String html) {
    return Html.fromHtml(html).toString();
  }
}
