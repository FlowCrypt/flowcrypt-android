/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.matchers

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.BoundedMatcher
import com.flowcrypt.email.ui.adapter.MessageHeadersListAdapter
import org.hamcrest.Description

/**
 * @author Denys Bondarenko
 */
class MessageHeadersListAdapterViewHolderMatcher(
  private val header: MessageHeadersListAdapter.Header
) : BoundedMatcher<RecyclerView.ViewHolder,
    MessageHeadersListAdapter.ViewHolder>(MessageHeadersListAdapter.ViewHolder::class.java) {
  override fun matchesSafely(holder: MessageHeadersListAdapter.ViewHolder): Boolean {
    return holder.binding.tVHeaderName.text.toString() == header.name
        && holder.binding.tVHeaderValue.text.toString() == header.value
  }

  override fun describeTo(description: Description) {
    description.appendText("with: $header")
  }
}