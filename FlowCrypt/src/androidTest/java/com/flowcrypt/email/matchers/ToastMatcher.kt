/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers

import android.view.WindowManager
import android.widget.Toast
import androidx.test.espresso.Root
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

/**
 * An implementation of [TypeSafeMatcher] to test [Toast] using Espresso.
 *
 * See [http://www.qaautomated.com/2016/01/how-to-test-toast-message-using-espresso.html]
 * (http://www.qaautomated.com/2016/01/how-to-test-toast-message-using-espresso.html)
 *
 * 1. Test if the Toast Message is Displayed :
 *
 * `onView(withText(R.string.mssage)).inRoot(new ToastMatcher()).check(matches(isDisplayed()));`
 *
 * 2. Test if the Toast Message is not Displayed
 *
 * `onView(withText(R.string.mssage)).inRoot(new ToastMatcher()).check(matches(not(isDisplayed())));`
 *
 * 3. Test id the Toast contains specific Text Message
 *
 * onView(withText(R.string.mssage)).inRoot(new ToastMatcher()).check(matches(withText("Invalid Name"));
 *
 *
 * @author Denis Bondarenko
 * Date: 09.03.2018
 * Time: 13:09
 * E-mail: DenBond7@gmail.com
 */

class ToastMatcher : TypeSafeMatcher<Root>() {

  override fun describeTo(description: Description) {
    description.appendText("is toast")
  }

  public override fun matchesSafely(root: Root): Boolean {
    val type = root.windowLayoutParams.get().type
    if (type == WindowManager.LayoutParams.TYPE_TOAST) {
      val windowToken = root.decorView.windowToken
      val appToken = root.decorView.applicationWindowToken
      if (windowToken === appToken) {
        // windowToken == appToken means this window isn't contained by any other windows.
        // if it was a window for an activity, it would have TYPE_BASE_APPLICATION.
        return true
      }
    }
    return false
  }
}
