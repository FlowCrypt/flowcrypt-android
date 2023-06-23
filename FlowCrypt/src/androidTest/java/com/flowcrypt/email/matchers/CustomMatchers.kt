/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Root
import androidx.test.espresso.matcher.BoundedMatcher
import com.flowcrypt.email.api.email.model.SecurityType
import com.flowcrypt.email.ui.adapter.PgpBadgeListAdapter
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.chip.Chip
import org.hamcrest.BaseMatcher
import org.hamcrest.Matcher

/**
 * @author Denys Bondarenko
 */

class CustomMatchers {
  companion object {
    fun withDrawable(resourceId: Int): Matcher<View> {
      return DrawableMatcher(resourceId)
    }

    fun withBitmap(bitmap: Bitmap): Matcher<View> {
      return BitmapMatcher(bitmap)
    }

    fun emptyDrawable(): Matcher<View> {
      return DrawableMatcher(DrawableMatcher.EMPTY)
    }

    fun withToolBarText(textMatcher: String): Matcher<View> {
      return ToolBarTitleMatcher.withText(textMatcher)
    }

    /**
     * Match the {@link SecurityType.Option}.
     *
     * @param option An input {@link SecurityType.Option}.
     */
    fun withSecurityTypeOption(option: SecurityType.Option): Matcher<SecurityType.Option> {
      return SecurityTypeOptionMatcher(option)
    }

    /**
     * Match a color in the {@link AppBarLayout}.
     *
     * @param color An input color value.
     * @return true if matched, otherwise false
     */
    fun withAppBarLayoutBackgroundColor(color: Int): BoundedMatcher<View, AppBarLayout> {
      return AppBarLayoutBackgroundColorMatcher(color)
    }

    /**
     * Match is [androidx.recyclerview.widget.RecyclerView] empty.
     */
    fun withEmptyRecyclerView(): BaseMatcher<View> {
      return EmptyRecyclerViewMatcher()
    }

    /**
     * Match is an items count of [RecyclerView] empty.
     */
    fun withRecyclerViewItemCount(itemCount: Int): BaseMatcher<View> {
      return RecyclerViewItemCountMatcher(itemCount)
    }

    fun withChipsBackgroundColor(context: Context, resourceId: Int): BoundedMatcher<View, Chip> {
      return ChipBackgroundColorMatcher(ContextCompat.getColor(context, resourceId))
    }

    fun withChipCloseIconAvailability(isCloseIconVisible: Boolean): BoundedMatcher<View, Chip> {
      return ChipCloseIconAvailabilityMatcher(isCloseIconVisible)
    }

    fun withPgpBadge(pgpBadge: PgpBadgeListAdapter.PgpBadge): PgpBadgeMatcher {
      return PgpBadgeMatcher(pgpBadge)
    }

    fun isToast(): BaseMatcher<Root?> {
      return ToastMatcher()
    }

    fun withTextViewDrawable(
      @DrawableRes resourceId: Int,
      @TextViewDrawableMatcher.DrawablePosition drawablePosition: Int
    ): Matcher<View> {
      return TextViewDrawableMatcher(resourceId, drawablePosition)
    }

    fun hasItem(matcher: Matcher<View>): Matcher<View> {
      return RecyclerViewItemMatcher(matcher)
    }

    fun withViewBackgroundTint(
      context: Context,
      @ColorRes resourceId: Int
    ): BackgroundTintMatcher {
      return BackgroundTintMatcher(
        requireNotNull(ContextCompat.getColorStateList(context, resourceId))
      )
    }

    fun withTextInputLayoutError(expectedHint: String): TextInputLayoutErrorMatcher {
      return TextInputLayoutErrorMatcher(expectedHint)
    }
  }
}
