/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.viewaction

import android.view.View
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.util.HumanReadables
import org.hamcrest.Matcher
import org.hamcrest.Matchers.any
import java.util.concurrent.TimeoutException

/**
 * A [ViewAction] that waits up to [timeout] milliseconds for a [View]'s visibility value to change to [View.GONE].
 * ref https://stackoverflow.com/questions/22358325/how-to-wait-till-a-view-has-gone-in-espresso-tests
 *
 * @author Denys Bondarenko
 */
class WaitUntilGoneAction(private val timeout: Long) : ViewAction {

  override fun getConstraints(): Matcher<View> {
    return any(View::class.java)
  }

  override fun getDescription(): String {
    return "wait up to $timeout milliseconds for the view to be gone"
  }

  override fun perform(uiController: UiController, view: View) {

    val endTime = System.currentTimeMillis() + timeout

    do {
      if (view.visibility == View.GONE) return
      uiController.loopMainThreadForAtLeast(50)
    } while (System.currentTimeMillis() < endTime)

    throw PerformException.Builder()
      .withActionDescription(description)
      .withCause(TimeoutException("Waited $timeout milliseconds"))
      .withViewDescription(HumanReadables.describe(view))
      .build()
  }
}