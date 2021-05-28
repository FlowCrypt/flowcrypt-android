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
import java.util.*

/**
 * This [ChipSpanChipCreator] responsible for displaying [Chip].
 *
 * @author Denis Bondarenko
 * Date: 31.07.2017
 * Time: 13:09
 * E-mail: DenBond7@gmail.com
 */
class CustomChipSpanChipCreator(context: Context) : ChipCreator<PGPContactChipSpan> {
  private val backgroundColorPgpExists: Int = UIUtil.getColor(context, R.color.colorPrimary)
  private val backgroundColorPgpExistsButKeyExpired: Int = UIUtil.getColor(context, R.color.orange)
  private val backgroundColorPgpNotExists: Int = UIUtil.getColor(context, R.color.aluminum)
  private val textColorPgpExists: Int = UIUtil.getColor(context, android.R.color.white)
  private val textColorNoPgpNoExists: Int = UIUtil.getColor(context, R.color.dark)

  override fun createChip(context: Context, text: CharSequence, data: Any?): PGPContactChipSpan {
    return PGPContactChipSpan(context, text.toString().toLowerCase(Locale.getDefault()), null, data)
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

    if (span.hasPgp != null) {
      span.hasPgp?.let { updateChipSpanBackground(span) }
    } else if (span.data != null && span.data is Cursor) {
      val cursor = span.data as? Cursor ?: return
      if (!cursor.isClosed) {
        val columnIndex = cursor.getColumnIndex("has_pgp")
        if (columnIndex != -1) {
          val hasPgp = cursor.getInt(columnIndex) == 1
          span.hasPgp = hasPgp
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
    if (span.hasPgp == true) {
      if (span.isExpired == true) {
        span.setBackgroundColor(ColorStateList.valueOf(backgroundColorPgpExistsButKeyExpired))
      } else {
        span.setBackgroundColor(ColorStateList.valueOf(backgroundColorPgpExists))
      }
      span.setTextColor(textColorPgpExists)
    } else {
      span.setBackgroundColor(ColorStateList.valueOf(backgroundColorPgpNotExists))
      span.setTextColor(textColorNoPgpNoExists)
    }
  }
}

