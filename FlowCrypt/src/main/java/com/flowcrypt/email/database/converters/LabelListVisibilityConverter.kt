/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.database.converters

import androidx.room.TypeConverter
import com.flowcrypt.email.database.entity.LabelEntity

/**
 * @author Denys Bondarenko
 */
class LabelListVisibilityConverter {
  @TypeConverter
  fun from(labelListVisibility: LabelEntity.LabelListVisibility): String {
    return labelListVisibility.value
  }

  @TypeConverter
  fun to(value: String): LabelEntity.LabelListVisibility {
    return LabelEntity.LabelListVisibility.findByValue(value)
  }
}