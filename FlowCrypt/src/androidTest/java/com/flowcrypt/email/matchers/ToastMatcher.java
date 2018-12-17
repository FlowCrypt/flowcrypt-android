/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers;

import android.os.IBinder;
import android.view.WindowManager;
import android.widget.Toast;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import androidx.test.espresso.Root;

/**
 * An implementation of {@link TypeSafeMatcher} to test {@link Toast} using Espresso.
 * <p>
 * See <a href ="http://www.qaautomated.com/2016/01/how-to-test-toast-message-using-espresso.html">http://www
 * .qaautomated.com/2016/01/how-to-test-toast-message-using-espresso.html</a>
 * <p>
 * 1. Test if the Toast Message is Displayed :
 * <p>
 * <code>onView(withText(R.string.mssage)).inRoot(new ToastMatcher()).check(matches(isDisplayed()));</code>
 * <p>
 * 2. Test if the Toast Message is not Displayed
 * <p>
 * <code>onView(withText(R.string.mssage)).inRoot(new ToastMatcher()).check(matches(not(isDisplayed())));</code>
 * <p>
 * 3. Test id the Toast contains specific Text Message
 * <p>
 * onView(withText(R.string.mssage)).inRoot(new ToastMatcher()).check(matches(withText("Invalid Name"));
 * <p>
 *
 * @author Denis Bondarenko
 * Date: 09.03.2018
 * Time: 13:09
 * E-mail: DenBond7@gmail.com
 */

public class ToastMatcher extends TypeSafeMatcher<Root> {

  @Override
  public void describeTo(Description description) {
    description.appendText("is toast");
  }

  @Override
  public boolean matchesSafely(Root root) {
    int type = root.getWindowLayoutParams().get().type;
    if (type == WindowManager.LayoutParams.TYPE_TOAST) {
      IBinder windowToken = root.getDecorView().getWindowToken();
      IBinder appToken = root.getDecorView().getApplicationWindowToken();
      if (windowToken == appToken) {
        // windowToken == appToken means this window isn't contained by any other windows.
        // if it was a window for an activity, it would have TYPE_BASE_APPLICATION.
        return true;
      }
    }
    return false;
  }
}
