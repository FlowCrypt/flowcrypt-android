/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.database.Cursor
import com.flowcrypt.email.R
import com.flowcrypt.email.util.UIUtil
import com.hootsuite.nachos.ChipConfiguration
import com.hootsuite.nachos.chip.Chip
import com.hootsuite.nachos.chip.ChipCreator
import com.hootsuite.nachos.chip.ChipSpan
import com.hootsuite.nachos.chip.ChipSpanChipCreator

/**
 * This [ChipSpanChipCreator] responsible for displaying [Chip].
 *
 * @author Denis Bondarenko
 * Date: 31.07.2017
 * Time: 13:09
 * E-mail: DenBond7@gmail.com
 */
class CustomChipSpanChipCreator(context: Context) : ChipCreator<PGPContactChipSpan> {
  private val bGColorHasUsablePubKey =
    UIUtil.getColor(context, CHIP_COLOR_RES_ID_HAS_USABLE_PUB_KEY)
  private val bgColorHasPubKeyButExpired =
    UIUtil.getColor(context, CHIP_COLOR_RES_ID_HAS_PUB_KEY_BUT_EXPIRED)
  private val bgColorHasPubKeyButRevoked =
    UIUtil.getColor(context, CHIP_COLOR_RES_ID_HAS_PUB_KEY_BUT_REVOKED)
  private val bgColorNoPubKey =
    UIUtil.getColor(context, CHIP_COLOR_RES_ID_NO_PUB_KEY)
  private val bgColorNoUsablePubKey =
    UIUtil.getColor(context, CHIP_COLOR_RES_ID_NO_USABLE_PUB_KEY)
  private val textColorHasPubKey = UIUtil.getColor(context, android.R.color.white)
  private val textColorNoPubKey = UIUtil.getColor(context, R.color.dark)

  override fun createChip(context: Context, text: CharSequence, data: Any?): PGPContactChipSpan {
    return PGPContactChipSpan(context, text.toString().lowercase(), null, data)
  }

  override fun createChip(
    context: Context,
    pgpContactChipSpan: PGPContactChipSpan
  ): PGPContactChipSpan {
    return PGPContactChipSpan(context, pgpContactChipSpan)
  }

  override fun configureChip(span: PGPContactChipSpan, chipConfiguration: ChipConfiguration) {
    val chipSpacing = chipConfiguration.chipHorizontalSpacing
    if (chipSpacing != -1) {
      span.setLeftMargin(chipSpacing / 2)
      span.setRightMargin(chipSpacing / 2)
    }

    val chipTextColor = chipConfiguration.chipTextColor
    if (chipTextColor != -1) {
      span.setTextColor(chipTextColor)
    }

    val chipTextSize = chipConfiguration.chipTextSize
    if (chipTextSize != -1) {
      span.setTextSize(chipTextSize)
    }

    val chipHeight = chipConfiguration.chipHeight
    if (chipHeight != -1) {
      span.setChipHeight(chipHeight)
    }

    val chipVerticalSpacing = chipConfiguration.chipVerticalSpacing
    if (chipVerticalSpacing != -1) {
      span.setChipVerticalSpacing(chipVerticalSpacing)
    }

    val maxAvailableWidth = chipConfiguration.maxAvailableWidth
    if (maxAvailableWidth != -1) {
      span.setMaxAvailableWidth(maxAvailableWidth)
    }

    if (span.hasAtLeastOnePubKey != null) {
      span.hasAtLeastOnePubKey?.let { updateChipSpanBackground(span) }
    } else if (span.data != null && span.data is Cursor) {
      val cursor = span.data as? Cursor ?: return
      if (!cursor.isClosed) {
        val columnIndex = cursor.getColumnIndex("has_pgp")
        if (columnIndex != -1) {
          val hasPgp = cursor.getInt(columnIndex) == 1
          span.hasAtLeastOnePubKey = hasPgp
          updateChipSpanBackground(span)
        }
      }
    } else {
      val chipBackground = chipConfiguration.chipBackground
      if (chipBackground != null) {
        span.setBackgroundColor(chipBackground)
      }
    }
  }

  /**
   * Update the [ChipSpan] background.
   *
   * @param span   The [ChipSpan] object.
   */
  private fun updateChipSpanBackground(span: PGPContactChipSpan) {
    if (span.hasAtLeastOnePubKey == true) {
      when {
        span.hasUsablePubKey == false -> {
          span.setBackgroundColor(ColorStateList.valueOf(bgColorNoUsablePubKey))
        }

        span.hasNotRevokedPubKey == false -> {
          span.setBackgroundColor(ColorStateList.valueOf(bgColorHasPubKeyButRevoked))
        }

        span.hasNotExpiredPubKey == false -> {
          span.setBackgroundColor(ColorStateList.valueOf(bgColorHasPubKeyButExpired))
        }

        else -> {
          span.setBackgroundColor(ColorStateList.valueOf(bGColorHasUsablePubKey))
        }
      }
      span.setTextColor(textColorHasPubKey)
    } else {
      span.setBackgroundColor(ColorStateList.valueOf(bgColorNoPubKey))
      span.setTextColor(textColorNoPubKey)
    }
  }

  companion object {
    const val CHIP_COLOR_RES_ID_HAS_USABLE_PUB_KEY = R.color.colorPrimary
    const val CHIP_COLOR_RES_ID_HAS_PUB_KEY_BUT_EXPIRED = R.color.orange
    const val CHIP_COLOR_RES_ID_HAS_PUB_KEY_BUT_REVOKED = R.color.red
    const val CHIP_COLOR_RES_ID_NO_PUB_KEY = R.color.aluminum
    const val CHIP_COLOR_RES_ID_NO_USABLE_PUB_KEY = R.color.red
  }
}
