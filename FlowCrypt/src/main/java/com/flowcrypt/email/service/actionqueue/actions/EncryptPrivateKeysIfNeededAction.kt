/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   DenBond7
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.service.actionqueue.actions

import android.content.Context
import androidx.preference.PreferenceManager
import com.flowcrypt.email.Constants
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.security.pgp.PgpPwd
import com.flowcrypt.email.ui.notifications.SystemNotificationManager
import com.flowcrypt.email.util.SharedPreferencesHelper
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.PrivateKeyStrengthException
import com.google.android.gms.common.util.CollectionUtils
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * This [Action] checks all available private keys are they encrypted. If not we will try to encrypt a key and
 * save to the local database.
 *
 * @author Denis Bondarenko
 * Date: 2/25/19
 * Time: 4:03 PM
 * E-mail: DenBond7@gmail.com
 */
@Parcelize
data class EncryptPrivateKeysIfNeededAction @JvmOverloads constructor(
  override var id: Long = 0,
  override var email: String? = null,
  override val version: Int = 0
) : Action {
  @IgnoredOnParcel
  @SerializedName(Action.TAG_NAME_ACTION_TYPE)
  override val type: Action.Type = Action.Type.ENCRYPT_PRIVATE_KEYS

  override suspend fun run(context: Context) {
    val keyEntities = KeysStorageImpl.getInstance(context).getRawKeys().map { it.copy() }
    val modifiedKeyEntities = mutableListOf<KeyEntity>()
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)

    if (keyEntities.isEmpty()) {
      return
    }

    for (keyEntity in keyEntities) {
      val passphrase = keyEntity.passphrase

      val keyDetailsList =
        PgpKey.parseKeys(keyEntity.privateKeyAsString.toByteArray(), false).pgpKeyDetailsList
      if (keyDetailsList.isEmpty() || keyDetailsList.size != 1) {
        ExceptionUtil.handleError(
          IllegalArgumentException(
            "An error occurred during the key parsing| 1: "
                + if (CollectionUtils.isEmpty(keyDetailsList)) "Empty results" else "Size = " + keyDetailsList.size
          )
        )
        continue
      }

      val keyDetails = keyDetailsList.first()

      if (keyDetails.isFullyEncrypted) {
        continue
      }

      try {
        PgpPwd.checkForWeakPassphrase(passphrase)
        val encryptedKey = PgpKey.encryptKey(keyDetails.privateKey!!, passphrase)

        val encryptedKeyDetailsList =
          PgpKey.parseKeys(encryptedKey.toByteArray(), false).pgpKeyDetailsList
        if (encryptedKeyDetailsList.isEmpty() || encryptedKeyDetailsList.size != 1) {
          ExceptionUtil.handleError(IllegalArgumentException("An error occurred during the key parsing| 2"))
          continue
        }

        val keyDetailsWithPgpEncryptedInfo = encryptedKeyDetailsList.first()
        val modifiedKeyEntity = keyEntity.copy(
          privateKey = KeyStoreCryptoManager.encrypt(keyDetailsWithPgpEncryptedInfo.privateKey)
            .toByteArray(),
          publicKey = keyDetailsWithPgpEncryptedInfo.publicKey.toByteArray()
        )
        modifiedKeyEntities.add(modifiedKeyEntity)
      } catch (e: PrivateKeyStrengthException) {
        val account = roomDatabase.accountDao().getActiveAccount() ?: return
        SystemNotificationManager(context).showPassphraseTooLowNotification(account)
        ExceptionUtil.handleError(e)
      }
    }

    roomDatabase.keysDao().update(modifiedKeyEntities)

    SharedPreferencesHelper.setBoolean(
      PreferenceManager
        .getDefaultSharedPreferences(context), Constants.PREF_KEY_IS_CHECK_KEYS_NEEDED, false
    )
  }
}
