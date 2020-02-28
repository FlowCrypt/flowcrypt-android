/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import com.flowcrypt.email.api.retrofit.node.gson.NodeGson
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.model.node.BaseMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.DecryptedAttMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.util.CacheManager
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import org.apache.commons.io.FileUtils
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.StringReader

/**
 * It's a result for "parseDecryptMsg" requests.
 *
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 3:48 PM
 * E-mail: DenBond7@gmail.com
 */
data class ParseDecryptedMsgResult constructor(
    @Expose val text: String?,
    @Expose val replyType: String,
    @Expose val subject: String?,
    @SerializedName("error")
    @Expose override val apiError: ApiError?,
    var msgBlocks: MutableList<MsgBlock>?) :
    BaseNodeResponse {
  override fun handleRawData(bufferedInputStream: BufferedInputStream) {
    var isEnabled = true
    val gson = NodeGson.gson

    if (msgBlocks == null) {
      msgBlocks = mutableListOf()
    }

    val tempDir = CacheManager.getCurrentMsgTempDir()
    FileAndDirectoryUtils.cleanDir(tempDir)

    while (isEnabled) {
      ByteArrayOutputStream().use { outputStream ->
        BufferedOutputStream(outputStream).use { bufferedOutputStream ->
          var c: Int

          //find the end of the next part of data
          while (true) {
            c = bufferedInputStream.read()
            if (c == -1 || c == '\n'.toInt()) {
              break
            }
            bufferedOutputStream.write(c.toByte().toInt())
          }

          bufferedOutputStream.flush()
          val jsonReader = gson.newJsonReader(StringReader(outputStream.toString()))
          val block = NodeGson.gson.fromJson<MsgBlock>(jsonReader, MsgBlock::class.java)

          if (block != null) {
            when (block.type) {
              MsgBlock.Type.DECRYPTED_ATT -> {
                if (tempDir != null) {
                  try {
                    val decryptedAtt: DecryptedAttMsgBlock = block as DecryptedAttMsgBlock
                    val fileName = FileAndDirectoryUtils.normalizeFileName(decryptedAtt.attMeta.name)
                    val file = if (fileName.isNullOrEmpty()) {
                      createTempFile(directory = tempDir)
                    } else {
                      val file = File(tempDir, fileName)
                      if (file.exists()) {
                        FileAndDirectoryUtils.createFileWithIncreasedIndex(tempDir, fileName)
                      } else {
                        file
                      }
                    }
                    FileUtils.writeByteArrayToFile(file, Base64.decode(decryptedAtt.attMeta.data, Base64.DEFAULT))
                    decryptedAtt.attMeta.data = null //clear raw info to prevent Binder exception
                    decryptedAtt.fileUri = Uri.fromFile(file)
                    msgBlocks?.add(decryptedAtt)
                  } catch (e: Exception) {
                    e.printStackTrace()
                    ExceptionUtil.handleError(e)
                  }
                } else {
                  ExceptionUtil.handleError(IOException("The temp cache dir == null"))
                }
              }
              else -> msgBlocks?.add(block)
            }
          }

          if (c == -1) {
            isEnabled = false
          }
        }
      }
    }
  }

  fun getMsgEncryptionType(): MessageEncryptionType {
    return if (replyType == "encrypted") MessageEncryptionType.ENCRYPTED else MessageEncryptionType.STANDARD
  }

  constructor(source: Parcel) : this(
      source.readString(),
      source.readString()!!,
      source.readString(),
      source.readParcelable<ApiError>(ApiError::class.java.classLoader),
      mutableListOf<MsgBlock>().apply { source.readTypedList(this, BaseMsgBlock.CREATOR) }
  )

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
    writeString(text)
    writeString(replyType)
    writeString(subject)
    writeParcelable(apiError, flags)
    writeTypedList(msgBlocks)
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<ParseDecryptedMsgResult> = object : Parcelable.Creator<ParseDecryptedMsgResult> {
      override fun createFromParcel(source: Parcel): ParseDecryptedMsgResult = ParseDecryptedMsgResult(source)
      override fun newArray(size: Int): Array<ParseDecryptedMsgResult?> = arrayOfNulls(size)
    }
  }
}
