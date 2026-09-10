/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.providers

import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import androidx.collection.LruCache
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.extensions.java.lang.printStackTraceIfDebugOnly
import org.apache.commons.io.FileUtils
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.math.max

/**
 * @author Denys Bondarenko
 */
class EmbeddedAttachmentsProvider : DocumentsProvider() {

  override fun onCreate(): Boolean {
    return true
  }

  override fun queryRoots(projection: Array<String>?): Cursor {
    return MatrixCursor(emptyArray())
  }

  override fun queryDocument(documentId: String?, projection: Array<String>?): Cursor {
    return MatrixCursor(emptyArray())
  }

  override fun queryChildDocuments(
    parentDocumentId: String, projection: Array<String>?,
    sortOrder: String
  ): Cursor {
    return MatrixCursor(emptyArray())
  }

  override fun openDocument(
    documentId: String,
    mode: String,
    signal: CancellationSignal?
  ): ParcelFileDescriptor? {
    if (mode.contains("w")) {
      throw IllegalStateException("Modification is not allowed")
    }

    return getFileDescriptor(getBytesForDocumentId(documentId))
  }

  override fun getDocumentType(documentId: String?): String {
    return documentId?.let {
      Cache.getInstance().get(documentId)?.type ?: super.getDocumentType(documentId)
    } ?: super.getDocumentType(documentId)
  }

  private fun getBytesForDocumentId(documentId: String): ByteArray {
    val attachmentInfo = Cache.getInstance().get(documentId)
    return requireNotNull(attachmentInfo?.rawData)
  }

  private fun getFileDescriptor(bytes: ByteArray): ParcelFileDescriptor? {
    val pipe = ParcelFileDescriptor.createPipe()
    val readParcelFileDescriptor = pipe[0]
    val writeParcelFileDescriptor = pipe[1]
    TransferThread(
      ByteArrayInputStream(bytes),
      ParcelFileDescriptor.AutoCloseOutputStream(writeParcelFileDescriptor)
    ).start()
    return readParcelFileDescriptor
  }

  private fun getAttachmentByDocumentId(documentId: String): AttachmentInfo? {
    return Cache.getInstance().get(documentId)
  }

  internal class TransferThread(
    val inputStream: InputStream,
    val outputStream: OutputStream
  ) : Thread() {
    override fun run() {
      try {
        inputStream.use {
          outputStream.use {
            inputStream.copyTo(outputStream)
            outputStream.flush()
          }
        }
      } catch (e: Exception) {
        e.printStackTraceIfDebugOnly()
      }
    }
  }


  class Cache private constructor() {
    private val lruCache: LruCache<String, AttachmentInfo>

    init {
      val maxMemory = Runtime.getRuntime().maxMemory()
      val cacheSize = max(maxMemory / 4, FileUtils.ONE_MB * 25).toInt()
      lruCache = object : LruCache<String, AttachmentInfo>(cacheSize) {
        override fun sizeOf(key: String, attachmentInfo: AttachmentInfo): Int {
          return attachmentInfo.rawData?.size ?: attachmentInfo.encodedSize.toInt()
        }
      }
    }

    fun get(documentId: String): AttachmentInfo? {
      return lruCache.get(documentId)
    }

    fun getUriVersion(documentId: String): AttachmentInfo? {
      return lruCache.get(documentId)?.copy(
        rawData = null,
        uri = getUriByDocumentId(documentId)
      )
    }

    fun getDocumentId(attachmentInfo: AttachmentInfo): String? {
      return lruCache.snapshot().filter { entry ->
        entry.value.uniqueStringId == attachmentInfo.uniqueStringId
            && entry.value.name == attachmentInfo.name
      }.map { it.key }.firstOrNull()
    }

    fun addAndGet(attachmentInfo: AttachmentInfo): AttachmentInfo {
      val uri = addOrReplace(attachmentInfo)
      return attachmentInfo.copy(rawData = null, uri = uri)
    }

    fun clear() {
      lruCache.evictAll()
    }

    private fun addOrReplace(attachmentInfo: AttachmentInfo): Uri? {
      val existingAttachmentInfoKey = getDocumentId(attachmentInfo)

      return if (attachmentInfo.rawData != null) {
        val documentId = existingAttachmentInfoKey ?: UUID.randomUUID().toString()
        lruCache.put(documentId, attachmentInfo)
        getUriByDocumentId(documentId)
      } else {
        if (existingAttachmentInfoKey != null) {
          lruCache.remove(existingAttachmentInfoKey)
        }
        null
      }
    }

    private fun getUriByDocumentId(documentId: String): Uri {
      return DocumentsContract.buildDocumentUri(AUTHORITY, documentId)
    }

    companion object {
      const val AUTHORITY = BuildConfig.APPLICATION_ID + ".embedded.attachments"

      @Volatile
      private var INSTANCE: Cache? = null

      @JvmStatic
      fun getInstance(): Cache {
        return INSTANCE ?: synchronized(this) {
          INSTANCE ?: Cache().also { INSTANCE = it }
        }
      }
    }
  }
}