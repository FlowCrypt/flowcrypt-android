/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue.actions

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import androidx.preference.PreferenceManager
import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor
import com.flowcrypt.email.broadcastreceivers.UpdateStorageConnectorBroadcastReceiver
import com.flowcrypt.email.database.dao.KeysDao
import com.flowcrypt.email.database.dao.source.KeysDaoSource
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.util.SharedPreferencesHelper
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.common.util.CollectionUtils
import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * This [Action] checks all available private keys are they encrypted. If not we will try to encrypt a key and
 * save to the local database.
 *
 * @author Denis Bondarenko
 * Date: 2/25/19
 * Time: 4:03 PM
 * E-mail: DenBond7@gmail.com
 */
data class EncryptPrivateKeysIfNeededAction @JvmOverloads constructor(override var id: Long = 0,
                                                                      override var email: String? = null,
                                                                      override val version: Int = 0) : Action {
  @SerializedName(Action.TAG_NAME_ACTION_TYPE)
  override val type: Action.Type = Action.Type.ENCRYPT_PRIVATE_KEYS

  override fun run(context: Context) {
    val keysStore = KeysStorageImpl.getInstance(context)
    val pgpKeyInfoList = keysStore.getAllPgpPrivateKeys()

    if (CollectionUtils.isEmpty(pgpKeyInfoList)) {
      return
    }

    val keyStoreCryptoManager = KeyStoreCryptoManager.getInstance(context)
    val keysDaoList = ArrayList<KeysDao>()

    for ((longid, private) in pgpKeyInfoList) {
      val passphrase = keysStore.getPassphrase(longid)

      if (TextUtils.isEmpty(passphrase)) {
        continue
      }

      val keyDetailsList = NodeCallsExecutor.parseKeys(private!!)
      if (CollectionUtils.isEmpty(keyDetailsList) || keyDetailsList.size != 1) {
        ExceptionUtil.handleError(IllegalArgumentException("An error occurred during the key parsing| 1: " + if (CollectionUtils.isEmpty(keyDetailsList)) "Empty results" else "Size = " + keyDetailsList.size))
        continue
      }

      val (isDecrypted, privateKey) = keyDetailsList[0]

      if ((!isDecrypted!!)) {
        continue
      }

      val (encryptedKey) = NodeCallsExecutor.encryptKey(privateKey!!, passphrase!!)

      if (TextUtils.isEmpty(encryptedKey)) {
        ExceptionUtil.handleError(IllegalArgumentException("An error occurred during the key encryption"))
        continue
      }

      val modifiedKeyDetailsList = NodeCallsExecutor.parseKeys(encryptedKey!!)
      if (CollectionUtils.isEmpty(modifiedKeyDetailsList) || modifiedKeyDetailsList.size != 1) {
        ExceptionUtil.handleError(IllegalArgumentException("An error occurred during the key parsing| 2"))
        continue
      }

      keysDaoList.add(KeysDao.generateKeysDao(keyStoreCryptoManager, modifiedKeyDetailsList[0], passphrase))
    }

    if (keysDaoList.size > 0) {
      val contentProviderResults = KeysDaoSource().updateKeys(context, keysDaoList)

      if (contentProviderResults.isEmpty()) {
        throw IllegalArgumentException("An error occurred during saving changes")
      }

      for (contentProviderResult in contentProviderResults) {
        if (contentProviderResult.count < 1) {
          throw IllegalArgumentException("An error occurred when we tried update " + contentProviderResult.uri)
        }
      }

      context.sendBroadcast(UpdateStorageConnectorBroadcastReceiver.newIntent(context))
    }

    SharedPreferencesHelper.setBoolean(PreferenceManager
        .getDefaultSharedPreferences(context), Constants.PREFERENCES_KEY_IS_CHECK_KEYS_NEEDED, false)
  }

  constructor(source: Parcel) : this(
      source.readLong(),
      source.readString(),
      source.readInt()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeLong(id)
        writeString(email)
        writeInt(version)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<EncryptPrivateKeysIfNeededAction> = object : Parcelable.Creator<EncryptPrivateKeysIfNeededAction> {
      override fun createFromParcel(source: Parcel): EncryptPrivateKeysIfNeededAction = EncryptPrivateKeysIfNeededAction(source)
      override fun newArray(size: Int): Array<EncryptPrivateKeysIfNeededAction?> = arrayOfNulls(size)
    }
  }
}
