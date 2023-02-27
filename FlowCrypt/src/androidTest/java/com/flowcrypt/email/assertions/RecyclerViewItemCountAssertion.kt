/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.assertions

import android.view.View

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`

/**
 * This [ViewAssertion] implementation asserts the [RecyclerView] size.
 *
 * @author Denys Bondarenko
 */
class RecyclerViewItemCountAssertion(private val expectedCount: Int) : ViewAssertion {

  override fun check(view: View, noViewFoundException: NoMatchingViewException?) {
    if (noViewFoundException != null) {
      throw noViewFoundException
    }

    val recyclerView = view as RecyclerView
    val adapter = recyclerView.adapter
    assertThat(adapter?.itemCount, `is`(expectedCount))
  }
}
