/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.kotlin

import android.content.Context
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import com.flowcrypt.email.security.pgp.PgpMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author Denys Bondarenko
 */
suspend fun ByteArray.processing(
  context: Context,
  skipAttachmentsRawData: Boolean = false,
  preResultAction: suspend (blocks: List<MsgBlock>) -> Unit = {}
): Result<PgpMsg.ProcessedMimeMessageResult?> = withContext(Dispatchers.IO) {
  return@withContext if (isEmpty()) {
    Result.exception(throwable = IllegalArgumentException("empty byte array"))
  } else {
    try {
      val processedMimeMessageResult = PgpMsg.processMimeMessage(
        context = context,
        inputStream = inputStream(),
        skipAttachmentsRawData = skipAttachmentsRawData
      )
      preResultAction.invoke(processedMimeMessageResult.blocks)
      return@withContext Result.success(processedMimeMessageResult)
    } catch (e: Exception) {
      return@withContext Result.exception(throwable = e)
    }
  }
}