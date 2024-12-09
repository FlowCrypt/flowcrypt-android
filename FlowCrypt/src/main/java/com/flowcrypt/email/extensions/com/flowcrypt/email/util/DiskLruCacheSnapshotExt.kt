/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.com.flowcrypt.email.util

import android.content.Context
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.kotlin.processing
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.flowcrypt.email.security.pgp.PgpMsg
import com.flowcrypt.email.util.cache.DiskLruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.pgpainless.PGPainless
import org.pgpainless.key.protection.PasswordBasedSecretKeyRingProtector
import org.pgpainless.util.Passphrase

/**
 * @author Denys Bondarenko
 */
suspend fun DiskLruCache.Snapshot.processing(
  context: Context,
  accountEntity: AccountEntity,
  skipAttachmentsRawData: Boolean = false,
  preResultAction: suspend (blocks: List<MsgBlock>) -> Unit = {}
): Result<PgpMsg.ProcessedMimeMessageResult?> = withContext(Dispatchers.IO) {
  val uri = getUri(0)
  if (uri != null) {
    try {
      val inputStream =
        context.contentResolver.openInputStream(uri) ?: throw java.lang.IllegalStateException()

      val keys = PGPainless.readKeyRing()
        .secretKeyRingCollection(accountEntity.servicePgpPrivateKey)

      val decryptionStream = PgpDecryptAndOrVerify.genDecryptionStream(
        srcInputStream = inputStream,
        secretKeys = keys,
        protector = PasswordBasedSecretKeyRingProtector.forKey(
          keys.first(),
          Passphrase.fromPassword(accountEntity.servicePgpPassphrase)
        )
      )

      val processedMimeMessage = PgpMsg.processMimeMessage(
        context = context,
        inputStream = decryptionStream,
        skipAttachmentsRawData = skipAttachmentsRawData
      )

      preResultAction.invoke(processedMimeMessage.blocks)
      return@withContext Result.success(processedMimeMessage)
    } catch (e: Exception) {
      return@withContext Result.exception(e)
    }
  } else {
    return@withContext getByteArray(0).processing(
      context = context,
      skipAttachmentsRawData = skipAttachmentsRawData
    ) { blocks ->
      preResultAction.invoke(blocks)
    }
  }
}