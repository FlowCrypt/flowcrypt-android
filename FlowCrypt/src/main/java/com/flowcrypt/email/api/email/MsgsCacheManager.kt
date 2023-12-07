/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import android.content.Context
import android.net.Uri
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.kotlin.toPGPPublicKeyRingCollection
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.armor
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPublicKeyRing
import com.flowcrypt.email.security.pgp.PgpEncryptAndOrSign
import com.flowcrypt.email.util.cache.DiskLruCache
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.internal.io.FileSystem
import okio.buffer
import org.pgpainless.PGPainless
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * This class describes a logic of caching massages. Here we use [DiskLruCache] to store and retrieve raw MIME messages.
 *
 * @author Denys Bondarenko
 */
object MsgsCacheManager {
  private const val CACHE_VERSION = BuildConfig.VERSION_CODE
  private const val CACHE_SIZE: Long = 1024 * 1000 * 50 //50Mb
  const val CACHE_DIR_NAME = "emails"
  lateinit var diskLruCache: DiskLruCache

  fun init(context: Context) {
    diskLruCache = DiskLruCache(
      FileSystem.SYSTEM,
      File(context.filesDir, CACHE_DIR_NAME),
      CACHE_VERSION,
      CACHE_SIZE
    )
  }

  suspend fun storeMsg(key: String, msg: MimeMessage, accountEntity: AccountEntity) =
    withContext(Dispatchers.IO) {
      storeMsgInternal(key, accountEntity) {
        msg.writeTo(it)
      }
    }

  suspend fun storeMsg(key: String, inputStream: InputStream, accountEntity: AccountEntity) =
    withContext(Dispatchers.IO) {
      storeMsgInternal(key, accountEntity) {
        inputStream.copyTo(it)
      }
    }

  fun getMsgAsByteArray(key: String): ByteArray {
    return diskLruCache[key]?.getByteArray(0) ?: return byteArrayOf()
  }

  fun getMsgAsUri(key: String): Uri? {
    return diskLruCache[key]?.getUri(0)
  }

  fun getMsgSnapshot(key: String): DiskLruCache.Snapshot? {
    return diskLruCache[key]
  }

  /**
   * Due to the realization of [DiskLruCache] we need to add a little delay
   * before fetching [DiskLruCache.Snapshot]
   */
  suspend fun getMsgSnapshotWithRetryStrategy(key: String): DiskLruCache.Snapshot? =
    withContext(Dispatchers.IO) {
      var attemptsCount = 0
      while (diskLruCache[key] == null && attemptsCount <= 50) {
        delay(50)
        attemptsCount++
      }
      return@withContext diskLruCache[key]
    }

  fun isMsgExist(key: String): Boolean {
    val snapshot = diskLruCache[key] ?: return false
    return snapshot.getLength(0) > 0
  }

  fun evictAll(context: Context?) {
    context ?: return
    diskLruCache.evictAll()
  }

  private fun storeMsgInternal(
    key: String,
    accountEntity: AccountEntity,
    action: (outputStream: OutputStream) -> Unit
  ) {
    val editor = diskLruCache.edit(key) ?: return

    try {
      val bufferedSink = editor.newSink().buffer()

      PgpEncryptAndOrSign.encryptAndOrSign(
        destOutputStream = bufferedSink.outputStream(),
        pgpPublicKeyRingCollection = PGPainless.readKeyRing()
          .secretKeyRingCollection(requireNotNull(accountEntity.pgpPrivateKey))
          .map { it.toPublicKeyRing().armor() }.toPGPPublicKeyRingCollection()
      ) { out ->
        out.use { outputStream ->
          action.invoke(outputStream)
        }

        bufferedSink.flush()
      }

      editor.commit()

      diskLruCache[key] ?: throw IOException("No space left on device")
    } catch (e: Exception) {
      editor.abort()
      throw e
    }
  }
}
