/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.BoundedMatcher
import com.flowcrypt.email.ui.adapter.PgpBadgeListAdapter
import org.hamcrest.Description

/**
 * @author Denys Bondarenko
 */
class PgpBadgeMatcher(private val pgpBadge: PgpBadgeListAdapter.PgpBadge) :
  BoundedMatcher<RecyclerView.ViewHolder, PgpBadgeListAdapter.ViewHolder>(
    PgpBadgeListAdapter.ViewHolder::class.java
  ) {
  override fun matchesSafely(holder: PgpBadgeListAdapter.ViewHolder): Boolean {
    return holder.badgeType == pgpBadge.type
  }

  override fun describeTo(description: Description) {
    description.appendText("with: $pgpBadge")
  }
}
