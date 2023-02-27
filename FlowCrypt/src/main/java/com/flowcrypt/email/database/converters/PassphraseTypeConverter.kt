/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.converters

import androidx.room.TypeConverter
import com.flowcrypt.email.database.entity.KeyEntity

/**
 * @author Denys Bondarenko
 */
class PassphraseTypeConverter {
  @TypeConverter
  fun fromKeyType(passphraseType: KeyEntity.PassphraseType): Int {
    return passphraseType.id
  }

  @TypeConverter
  fun toKeyType(id: Int): KeyEntity.PassphraseType {
    return KeyEntity.PassphraseType.findValueById(id)
  }
}
