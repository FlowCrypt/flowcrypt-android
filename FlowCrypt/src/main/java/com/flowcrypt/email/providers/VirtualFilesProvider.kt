/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.providers

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import com.flowcrypt.email.R
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException


/**
 * @author Denys Bondarenko
 */
class VirtualFilesProvider : DocumentsProvider() {

  private val TAG = "MyCloudProvider"

  private val DEFAULT_ROOT_PROJECTION = arrayOf(
    Root.COLUMN_ROOT_ID,
    Root.COLUMN_MIME_TYPES,
    Root.COLUMN_FLAGS,
    Root.COLUMN_ICON,
    Root.COLUMN_TITLE,
    Root.COLUMN_SUMMARY,
    Root.COLUMN_DOCUMENT_ID,
    Root.COLUMN_AVAILABLE_BYTES
  )

  private val DEFAULT_DOCUMENT_PROJECTION = arrayOf<String>(
    Document.COLUMN_DOCUMENT_ID,
    Document.COLUMN_MIME_TYPE,
    Document.COLUMN_DISPLAY_NAME,
    Document.COLUMN_LAST_MODIFIED,
    Document.COLUMN_FLAGS,
    Document.COLUMN_SIZE
  )

  private val ROOT = "root"

  private var mBaseDir: File? = null

  override fun onCreate(): Boolean {
    mBaseDir = context!!.filesDir
    return true
  }

  @Throws(FileNotFoundException::class)
  override fun queryRoots(projection: Array<String>?): Cursor {

    // Create a cursor with either the requested fields, or the default projection.  This
    // cursor is returned to the Android system picker UI and used to display all roots from
    // this provider.
    val result = MatrixCursor(resolveRootProjection(projection))

    // It's possible to have multiple roots (e.g. for multiple accounts in the same app) -
    // just add multiple cursor rows.
    // Construct one row for a root called "MyCloud".
    val row = result.newRow()
    row.add(Root.COLUMN_ROOT_ID, ROOT)
    row.add(Root.COLUMN_SUMMARY, context!!.getString(R.string.app_name))

    // FLAG_SUPPORTS_CREATE means at least one directory under the root supports creating
    // documents.  FLAG_SUPPORTS_RECENTS means your application's most recently used
    // documents will show up in the "Recents" category.  FLAG_SUPPORTS_SEARCH allows users
    // to search all documents the application shares. FLAG_SUPPORTS_IS_CHILD allows
    // testing parent child relationships, available after SDK 21 (Lollipop).
    row.add(
      Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE or
          Root.FLAG_SUPPORTS_RECENTS or
          Root.FLAG_SUPPORTS_SEARCH or
          Root.FLAG_SUPPORTS_IS_CHILD
    )

    // COLUMN_TITLE is the root title (e.g. what will be displayed to identify your provider).
    row.add(Root.COLUMN_TITLE, context!!.getString(R.string.app_name))

    // This document id must be unique within this provider and consistent across time.  The
    // system picker UI may save it and refer to it later.
    row.add(Root.COLUMN_DOCUMENT_ID, getDocIdForFile(mBaseDir))

    // The child MIME types are used to filter the roots and only present to the user roots
    // that contain the desired type somewhere in their file hierarchy.
    row.add(Root.COLUMN_MIME_TYPES, getChildMimeTypes(mBaseDir))
    row.add(Root.COLUMN_AVAILABLE_BYTES, mBaseDir!!.freeSpace)
    row.add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
    return result
  }

  @Throws(FileNotFoundException::class)
  override fun queryDocument(documentId: String?, projection: Array<String>?): Cursor? {
    // Create a cursor with the requested projection, or the default projection.
    val result = MatrixCursor(resolveDocumentProjection(projection))
    includeFile(result, documentId, null)
    return result
  }

  @Throws(FileNotFoundException::class)
  override fun queryChildDocuments(
    parentDocumentId: String, projection: Array<String>?,
    sortOrder: String
  ): Cursor {
    val result = MatrixCursor(resolveDocumentProjection(projection))
    val parent = getFileForDocId(parentDocumentId)
    for (file: File? in parent!!.listFiles()) {
      includeFile(result, null, file)
    }
    return result
  }
  // END_INCLUDE(query_child_documents)


  // END_INCLUDE(query_child_documents)
  // BEGIN_INCLUDE(open_document)
  @Throws(FileNotFoundException::class)
  override fun openDocument(
    documentId: String, mode: String,
    signal: CancellationSignal?
  ): ParcelFileDescriptor? {
    // It's OK to do network operations in this method to download the document, as long as you
    // periodically check the CancellationSignal.  If you have an extremely large file to
    // transfer from the network, a better solution may be pipes or sockets
    // (see ParcelFileDescriptor for helper methods).
    val file = getFileForDocId(documentId)
    val accessMode = ParcelFileDescriptor.parseMode(mode)
    val isWrite = mode.indexOf('w') != -1
    return if (isWrite) {
      // Attach a close listener if the document is opened in write mode.
      try {
        val handler = Handler(context!!.mainLooper)
        ParcelFileDescriptor.open(file, accessMode, handler,
          object : ParcelFileDescriptor.OnCloseListener {
            override fun onClose(e: IOException) {

            }
          })
      } catch (e: IOException) {
        throw FileNotFoundException(
          ("Failed to open document with id " + documentId +
              " and mode " + mode)
        )
      }
    } else {
      ParcelFileDescriptor.open(file, accessMode)
    }
  }

  fun isChildFile(parentFile: File?, childFile: File?): Boolean {
    val realFileParent = childFile!!.parentFile
    return realFileParent == null || realFileParent == parentFile
  }

  override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
    try {
      val parentFile = getFileForDocId(parentDocumentId)
      val childFile = getFileForDocId(documentId)
      return isChildFile(parentFile, childFile)
    } catch (e: FileNotFoundException) {
      e.printStackTrace()
    }
    return false
  }

  @Throws(FileNotFoundException::class)
  override fun getDocumentType(documentId: String): String? {
    val file = getFileForDocId(documentId)
    return getTypeForFile(file)
  }

  /**
   * @param projection the requested root column projection
   * @return either the requested root column projection, or the default projection if the
   * requested projection is null.
   */
  private fun resolveRootProjection(projection: Array<String>?): Array<String>? {
    return projection ?: DEFAULT_ROOT_PROJECTION
  }

  private fun resolveDocumentProjection(projection: Array<String>?): Array<String>? {
    return projection ?: DEFAULT_DOCUMENT_PROJECTION
  }

  /**
   * Get a file's MIME type
   *
   * @param file the File object whose type we want
   * @return the MIME type of the file
   */
  private fun getTypeForFile(file: File?): String {
    return if (file!!.isDirectory) {
      Document.MIME_TYPE_DIR
    } else {
      getTypeForName(file.name)
    }
  }

  /**
   * Get the MIME data type of a document, given its filename.
   *
   * @param name the filename of the document
   * @return the MIME data type of a document
   */
  private fun getTypeForName(name: String): String {
    val lastDot = name.lastIndexOf('.')
    if (lastDot >= 0) {
      val extension = name.substring(lastDot + 1)
      val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
      if (mime != null) {
        return mime
      }
    }
    return "application/octet-stream"
  }

  /**
   * Gets a string of unique MIME data types a directory supports, separated by newlines.  This
   * should not change.
   *
   * @param parent the File for the parent directory
   * @return a string of the unique MIME data types the parent directory supports
   */
  private fun getChildMimeTypes(parent: File?): String? {
    val mimeTypes: MutableSet<String> = HashSet()
    mimeTypes.add("image/*")
    mimeTypes.add("text/*")
    mimeTypes.add("application/vnd.openxmlformats-officedocument.wordprocessingml.document")

    // Flatten the list into a string and insert newlines between the MIME type strings.
    val mimeTypesString = StringBuilder()
    for (mimeType: String in mimeTypes) {
      mimeTypesString.append(mimeType).append("\n")
    }
    return mimeTypesString.toString()
  }

  /**
   * Get the document ID given a File.  The document id must be consistent across time.  Other
   * applications may save the ID and use it to reference documents later.
   *
   *
   * This implementation is specific to this demo.  It assumes only one root and is built
   * directly from the file structure.  However, it is possible for a document to be a child of
   * multiple directories (for example "android" and "images"), in which case the file must have
   * the same consistent, unique document ID in both cases.
   *
   * @param file the File whose document ID you want
   * @return the corresponding document ID
   */
  private fun getDocIdForFile(file: File?): String? {
    var path = file!!.absolutePath

    // Start at first char of path under root
    val rootPath = mBaseDir!!.path
    if ((rootPath == path)) {
      path = ""
    } else if (rootPath.endsWith("/")) {
      path = path.substring(rootPath.length)
    } else {
      path = path.substring(rootPath.length + 1)
    }
    return "root:$path"
  }

  /**
   * Add a representation of a file to a cursor.
   *
   * @param result the cursor to modify
   * @param docId  the document ID representing the desired file (may be null if given file)
   * @param file   the File object representing the desired file (may be null if given docID)
   * @throws FileNotFoundException
   */
  @Throws(FileNotFoundException::class)
  private fun includeFile(result: MatrixCursor, docId: String?, file: File?) {
    var docId = docId
    var file = file
    if (docId == null) {
      docId = getDocIdForFile(file)
    } else {
      file = getFileForDocId(docId)
    }
    var flags = 0
    val displayName = file!!.name
    val mimeType = getTypeForFile(file)
    val row = result.newRow()
    row.add(Document.COLUMN_DOCUMENT_ID, docId)
    row.add(Document.COLUMN_DISPLAY_NAME, displayName)
    row.add(Document.COLUMN_SIZE, file.length())
    row.add(Document.COLUMN_MIME_TYPE, mimeType)
    row.add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
    row.add(Document.COLUMN_FLAGS, flags)

    // Add a custom icon
    row.add(Document.COLUMN_ICON, R.mipmap.ic_launcher)
  }

  /**
   * Translate your custom URI scheme into a File object.
   *
   * @param docId the document ID representing the desired file
   * @return a File represented by the given document ID
   * @throws java.io.FileNotFoundException
   */
  @Throws(FileNotFoundException::class)
  private fun getFileForDocId(docId: String): File? {
    var target = mBaseDir
    if ((docId == ROOT)) {
      return target
    }
    val splitIndex = docId.indexOf(':', 1)
    if (splitIndex < 0) {
      throw FileNotFoundException("Missing root for $docId")
    } else {
      val path = docId.substring(splitIndex + 1)
      target = File(target, path)
      if (!target.exists()) {
        throw FileNotFoundException("Missing file for $docId at $target")
      }
      return target
    }
  }

  private fun getResourceIdArray(arrayResId: Int): IntArray {
    val ar = context!!.resources.obtainTypedArray(arrayResId)
    val len = ar.length()
    val resIds = IntArray(len)
    for (i in 0 until len) {
      resIds[i] = ar.getResourceId(i, 0)
    }
    ar.recycle()
    return resIds
  }
}