/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.providers

import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.util.Log
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.api.email.model.AttachmentInfo
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap


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
    val finalProjection = projection ?: DEFAULT_DOCUMENT_PROJECTION
    return MatrixCursor(finalProjection).apply {
      documentId?.let { id ->
        getAttachmentByDocumentId(id)?.let { attachmentInfo ->
          newRow().apply {
            if (DocumentsContract.Document.COLUMN_DOCUMENT_ID in finalProjection) {
              add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, id)
            }
            if (DocumentsContract.Document.COLUMN_DISPLAY_NAME in finalProjection) {
              add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, attachmentInfo.getSafeName())
            }
            if (DocumentsContract.Document.COLUMN_MIME_TYPE in finalProjection) {
              add(DocumentsContract.Document.COLUMN_MIME_TYPE, attachmentInfo.type)
            }
            if (DocumentsContract.Document.COLUMN_FLAGS in finalProjection) {
              add(DocumentsContract.Document.COLUMN_FLAGS, 0)
            }
            if (DocumentsContract.Document.COLUMN_SIZE in finalProjection) {
              add(DocumentsContract.Document.COLUMN_SIZE, attachmentInfo.rawData?.size ?: 0)
            }
            if (DocumentsContract.Document.COLUMN_LAST_MODIFIED in finalProjection) {
              add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, null)
            }
          }
        }
      }
    }
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
    return getFileDescriptor(getBytesForDocumentId(documentId))
  }

  override fun openTypedDocument(
    documentId: String?,
    mimeTypeFilter: String?,
    opts: Bundle?,
    signal: CancellationSignal?
  ): AssetFileDescriptor {
    return AssetFileDescriptor(
      openDocument(documentId = documentId!!, mode = "r", signal),
      0,
      AssetFileDescriptor.UNKNOWN_LENGTH
    )
  }

  private fun getBytesForDocumentId(documentId: String): ByteArray {
    val attachmentInfo = Cache.getInstance().get(documentId)
    return requireNotNull(attachmentInfo?.rawData)
  }

  private fun getFileDescriptor(bytes: ByteArray): ParcelFileDescriptor? {
    val pipe = ParcelFileDescriptor.createPipe()
    val readParcelFileDescriptor = pipe[0]
    val writeParcelFileDescriptor = pipe[1]

    ByteArrayInputStream(bytes).use { inputStream ->
      ParcelFileDescriptor.AutoCloseOutputStream(writeParcelFileDescriptor).use { outputStream ->
        inputStream.copyTo(outputStream)
      }
    }

    return readParcelFileDescriptor
  }

  private fun getAttachmentByDocumentId(documentId: String): AttachmentInfo? {
    return Cache.getInstance().get(documentId)
  }

  internal class TransferThread(var bytes: ByteArray, var outputStream: OutputStream) : Thread() {
    override fun run() {
      try {
        outputStream.write(bytes)
        outputStream.flush()
        outputStream.close()
      } catch (e: IOException) {
        Log.e(
          javaClass.getSimpleName(),
          "Exception transferring file", e
        )
      }
    }
  }

  class Cache private constructor() {
    private val map: ConcurrentHashMap<String, AttachmentInfo> = ConcurrentHashMap()

    fun get(documentId: String): AttachmentInfo? {
      return map[documentId]
    }

    fun getUriVersion(documentId: String): AttachmentInfo? {
      return map[documentId]?.copy(
        rawData = null,
        uri = getUriByDocumentId(documentId)
      )
    }

    fun getUriByDocumentId(documentId: String): Uri {
      return DocumentsContract.buildDocumentUri(AUTHORITY, documentId)
    }

    fun getDocumentId(attachmentInfo: AttachmentInfo): String? {
      return map.filter { entry ->
        entry.value.uniqueStringId == attachmentInfo.uniqueStringId
            && entry.value.name == attachmentInfo.name
      }.map { it.key }.firstOrNull()
    }

    fun addAndGet(attachmentInfo: AttachmentInfo): AttachmentInfo {
      val uri = addOrReplace(attachmentInfo)
      return attachmentInfo.copy(rawData = null, uri = uri)
    }

    fun clear() {
      map.clear()
    }

    private fun addOrReplace(attachmentInfo: AttachmentInfo): Uri? {
      val existingAttachmentInfoKey = getDocumentId(attachmentInfo)

      return if (attachmentInfo.rawData != null) {
        val documentId = existingAttachmentInfoKey ?: UUID.randomUUID().toString()
        map[documentId] = attachmentInfo
        getUriByDocumentId(documentId)
      } else {
        if (existingAttachmentInfoKey != null) {
          map.remove(existingAttachmentInfoKey)
        }
        null
      }
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

  companion object {
    private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
      DocumentsContract.Document.COLUMN_DOCUMENT_ID,
      DocumentsContract.Document.COLUMN_MIME_TYPE,
      DocumentsContract.Document.COLUMN_DISPLAY_NAME,
      DocumentsContract.Document.COLUMN_LAST_MODIFIED,
      DocumentsContract.Document.COLUMN_FLAGS,
      DocumentsContract.Document.COLUMN_SIZE
    )
  }
}