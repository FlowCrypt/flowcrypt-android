/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue.actions

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.api.email.protocol.SmtpProtocolUtil
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.security.KeysStorageImpl
import com.google.gson.annotations.SerializedName

/**
 * This action describes a task which backups a private key to INBOX.
 *
 * @author Denis Bondarenko
 * Date: 29.01.2018
 * Time: 16:58
 * E-mail: DenBond7@gmail.com
 */
data class BackupPrivateKeyToInboxAction @JvmOverloads constructor(override var id: Long = 0,
                                                                   override var email: String,
                                                                   override val version: Int = 0,
                                                                   private val privateKeyLongId: String) : Action {
  @SerializedName(Action.TAG_NAME_ACTION_TYPE)
  override val type: Action.Type = Action.Type.BACKUP_PRIVATE_KEY_TO_INBOX

  override fun run(context: Context) {
    val account = AccountDaoSource().getAccountInformation(context, email)
    val keysStorage = KeysStorageImpl.getInstance(context)
    val pgpKeyInfo = keysStorage.getPgpPrivateKey(privateKeyLongId)
    if (account != null && pgpKeyInfo != null && !TextUtils.isEmpty(pgpKeyInfo.private)) {
      val session = OpenStoreHelper.getAccountSess(context, account)
      val transport = SmtpProtocolUtil.prepareSmtpTransport(context, session, account)

      val (encryptedKey) = NodeCallsExecutor.encryptKey(pgpKeyInfo.private!!,
          keysStorage.getPassphrase(privateKeyLongId)!!)

      if (TextUtils.isEmpty(encryptedKey)) {
        throw IllegalStateException("An error occurred during encrypting some key")
      }

      val mimeBodyPart = EmailUtil.genBodyPartWithPrivateKey(account, encryptedKey!!)
      val message = EmailUtil.genMsgWithPrivateKeys(context, account, session, mimeBodyPart)
      transport.sendMessage(message, message.allRecipients)
    }
  }

  constructor(source: Parcel) : this(
      source.readLong(),
      source.readString()!!,
      source.readInt(),
      source.readString()!!
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeLong(id)
        writeString(email)
        writeInt(version)
        writeString(privateKeyLongId)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<BackupPrivateKeyToInboxAction> =
        object : Parcelable.Creator<BackupPrivateKeyToInboxAction> {
          override fun createFromParcel(source: Parcel): BackupPrivateKeyToInboxAction =
              BackupPrivateKeyToInboxAction(source)
      override fun newArray(size: Int): Array<BackupPrivateKeyToInboxAction?> = arrayOfNulls(size)
    }
  }
}
