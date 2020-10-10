/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Base64OutputStream
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.util.ProgressOutputStream
import com.flowcrypt.email.util.cache.DiskLruCache
import com.flowcrypt.email.util.exception.SyncTaskTerminatedException
import okhttp3.internal.io.FileSystem
import okio.buffer
import java.io.File
import java.io.IOException
import javax.crypto.CipherOutputStream
import javax.mail.internet.MimeMessage

/**
 * This class describes a logic of caching massages. Here we use [DiskLruCache] to store and retrieve raw MIME messages.
 *
 * @author Denis Bondarenko
 *         Date: 8/12/19
 *         Time: 11:40 AM
 *         E-mail: DenBond7@gmail.com
 */
object MsgsCacheManager {
  private const val CACHE_VERSION = BuildConfig.VERSION_CODE
  private const val CACHE_SIZE: Long = 1024 * 1000 * 50 //50Mb
  const val CACHE_DIR_NAME = "emails"
  lateinit var diskLruCache: DiskLruCache

  fun init(context: Context) {
    diskLruCache = DiskLruCache(FileSystem.SYSTEM, File(context.filesDir, CACHE_DIR_NAME), CACHE_VERSION, CACHE_SIZE)
  }

  fun storeMsg(key: String, msg: MimeMessage) {
    val editor = diskLruCache.edit(key) ?: return

    val bufferedSink = editor.newSink().buffer()
    val outputStreamOfBufferedSink = ProgressOutputStream(bufferedSink.outputStream())
    val cipherForEncryption = KeyStoreCryptoManager.getCipherForEncryption()
    val base64OutputStream = Base64OutputStream(outputStreamOfBufferedSink, KeyStoreCryptoManager.BASE64_FLAGS)
    val outputStream = CipherOutputStream(base64OutputStream, cipherForEncryption)

    try {
      outputStream.use {
        outputStreamOfBufferedSink.write(Base64.encodeToString(cipherForEncryption.iv, KeyStoreCryptoManager.BASE64_FLAGS).toByteArray())
        outputStreamOfBufferedSink.write("\n".toByteArray())
        msg.writeTo(it)
        bufferedSink.flush()
        editor.commit()
      }

      diskLruCache[key] ?: throw IOException("No space left on device")
    } catch (e: SyncTaskTerminatedException) {
      e.printStackTrace()
      editor.abort()
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

  fun isMsgExist(key: String): Boolean {
    val snapshot = diskLruCache[key] ?: return false
    return snapshot.getLength(0) > 0
  }

  fun evictAll(context: Context?) {
    context ?: return
    diskLruCache.evictAll()
  }
}