/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.converters

import androidx.room.TypeConverter
import com.flowcrypt.email.database.entity.KeyEntity

/**
 * @author Denis Bondarenko
 *         Date: 5/17/21
 *         Time: 6:13 PM
 *         E-mail: DenBond7@gmail.com
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
