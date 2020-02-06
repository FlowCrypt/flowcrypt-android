/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import android.content.Context
import android.net.Uri
import com.flowcrypt.email.util.cache.DiskLruCache
import okhttp3.internal.io.FileSystem
import okio.buffer
import okio.source
import java.io.File
import java.io.InputStream


/**
 * This class describes a logic of caching massages. Here we use [DiskLruCache] to store and retrieve raw MIME messages.
 *
 * @author Denis Bondarenko
 *         Date: 8/12/19
 *         Time: 11:40 AM
 *         E-mail: DenBond7@gmail.com
 */
object MsgsCacheManager {
  const val CACHE_VERSION = 1
  const val CACHE_SIZE: Long = 1024 * 1000 * 50 //50Mb
  const val CACHE_DIR_NAME = "emails"
  lateinit var diskLruCache: DiskLruCache

  fun init(context: Context) {
    diskLruCache = DiskLruCache(FileSystem.SYSTEM, File(context.filesDir, CACHE_DIR_NAME), CACHE_VERSION, 1, CACHE_SIZE)
  }

  fun addMsg(key: String, inputStream: InputStream) {
    val editor = diskLruCache.edit(key) ?: return

    val bufferedSink = editor.newSink(0).buffer()
    bufferedSink.writeAll(inputStream.source())
    bufferedSink.flush()
    editor.commit()
  }

  fun getMsgAsByteArray(key: String): ByteArray {
    return diskLruCache[key]?.getByteArray(0) ?: return byteArrayOf()
  }

  fun getMsgAsUri(key: String): Uri? {
    return diskLruCache[key]?.getUri(0) ?: return null
  }

  fun isMsgExist(key: String): Boolean {
    val snapshot = diskLruCache[key] ?: return false
    return snapshot.getLength(0) > 0
  }
}