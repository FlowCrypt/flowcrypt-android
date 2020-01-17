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
 * @author Denis Bondarenko
 * Date: 21.05.2018
 * Time: 15:43
 * E-mail: DenBond7@gmail.com
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
