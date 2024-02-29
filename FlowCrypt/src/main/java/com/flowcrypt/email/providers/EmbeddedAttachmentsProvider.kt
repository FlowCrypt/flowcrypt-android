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
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.api.email.model.AttachmentInfo
import java.io.ByteArrayInputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Denys Bondarenko
 */
class EmbeddedAttachmentsProvider : DocumentsProvider() {

  override fun onCreate(): Boolean {
    return true
  }

  override fun queryRoots(projection: Array<String>?): Cursor = MatrixCursor(emptyArray())

  override fun queryDocument(documentId: String?, projection: Array<String>?): Cursor =
    MatrixCursor(emptyArray())

  override fun queryChildDocuments(
    parentDocumentId: String, projection: Array<String>?,
    sortOrder: String
  ): Cursor = MatrixCursor(emptyArray())

  override fun openDocument(
    documentId: String,
    mode: String,
    signal: CancellationSignal?
  ): ParcelFileDescriptor? {
    return getParcelFileDescriptorForDocument(documentId)
  }

  private fun getParcelFileDescriptorForDocument(
    documentId: String
  ): ParcelFileDescriptor? {
    return getFileDescriptor(getBytesForDocumentId(documentId))
  }

  private fun getBytesForDocumentId(documentId: String): ByteArray {
    val attachmentInfo = Cache.getInstance().get(documentId)
    return requireNotNull(attachmentInfo?.rawData)
  }

  private fun getFileDescriptor(fileData: ByteArray): ParcelFileDescriptor? {
    val pipe = ParcelFileDescriptor.createPipe()
    val readParcelFileDescriptor = pipe[0]
    val writeParcelFileDescriptor = pipe[1]

    ByteArrayInputStream(fileData).use { inputStream ->
      ParcelFileDescriptor.AutoCloseOutputStream(writeParcelFileDescriptor).use { outputStream ->
        inputStream.copyTo(outputStream)
      }
    }

    return readParcelFileDescriptor
  }

  class Cache private constructor() {
    private val map: ConcurrentHashMap<String, AttachmentInfo> = ConcurrentHashMap()

    fun get(documentId: String): AttachmentInfo? {
      return map[documentId]
    }

    fun add(attachmentInfo: AttachmentInfo): Uri? {
      return if (attachmentInfo.rawData != null) {
        val documentId = UUID.randomUUID().toString()
        map[documentId] = attachmentInfo
        DocumentsContract.buildDocumentUri(AUTHORITY, documentId)
      } else null
    }

    fun addAndGet(attachmentInfo: AttachmentInfo): AttachmentInfo {
      val uri = add(attachmentInfo)
      return attachmentInfo.copy(rawData = null, uri = uri)
    }

    fun clear() {
      map.clear()
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