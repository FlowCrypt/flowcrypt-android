/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.providers

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsProvider
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException

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

  @Throws(FileNotFoundException::class)
  override fun openDocument(
    documentId: String,
    mode: String,
    signal: CancellationSignal?
  ): ParcelFileDescriptor? {
    return getFileDescriptor(getBytesForDocumentId(documentId))
  }

  private fun getBytesForDocumentId(documentId: String): ByteArray {
    return "SOME DATA".toByteArray()
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
}