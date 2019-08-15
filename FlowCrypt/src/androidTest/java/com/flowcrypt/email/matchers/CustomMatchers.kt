/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers

import android.view.View
import android.widget.ListView
import androidx.test.espresso.matcher.BoundedMatcher
import com.flowcrypt.email.api.email.model.SecurityType
import com.google.android.material.appbar.AppBarLayout
import org.hamcrest.BaseMatcher
import org.hamcrest.Matcher

/**
 * @author Denis Bondarenko
 * Date: 3/15/19
 * Time: 5:20 PM
 * E-mail: DenBond7@gmail.com
 */

class CustomMatchers {
  companion object {
    @JvmStatic
    fun withDrawable(resourceId: Int): Matcher<View> {
      return DrawableMatcher(resourceId)
    }

    @JvmStatic
    fun emptyDrawable(): Matcher<View> {
      return DrawableMatcher(DrawableMatcher.EMPTY)
    }

    @JvmStatic
    fun withToolBarText(textMatcher: String): Matcher<View> {
      return ToolBarTitleMatcher.withText(textMatcher)
    }

    /**
     * Match the {@link SecurityType.Option}.
     *
     * @param option An input {@link SecurityType.Option}.
     */
    @JvmStatic
    fun withSecurityTypeOption(option: SecurityType.Option): Matcher<SecurityType.Option> {
      return SecurityTypeOptionMatcher(option)
    }

    /**
     * Match a color in the {@link AppBarLayout}.
     *
     * @param color An input color value.
     * @return true if matched, otherwise false
     */
    @JvmStatic
    fun withAppBarLayoutBackgroundColor(color: Int): BoundedMatcher<View, AppBarLayout> {
      return AppBarLayoutBackgroundColorMatcher(color)
    }

    /**
     * Match is [ListView] empty.
     */
    @JvmStatic
    fun withEmptyListView(): BaseMatcher<View> {
      return EmptyListViewMather()
    }

    /**
     * Match is [androidx.recyclerview.widget.RecyclerView] empty.
     */
    @JvmStatic
    fun withEmptyRecyclerView(): BaseMatcher<View> {
      return EmptyRecyclerViewMatcher()
    }

    /**
     * Match is an items count of [ListView] empty.
     */
    @JvmStatic
    fun withListViewItemCount(itemCount: Int): BaseMatcher<View> {
      return ListViewItemCountMatcher(itemCount)
    }

    /**
     * Match is an items count of [RecyclerView] empty.
     */
    @JvmStatic
    fun withRecyclerViewItemCount(itemCount: Int): BaseMatcher<View> {
      return RecyclerViewItemCountMatcher(itemCount)
    }
  }
}
