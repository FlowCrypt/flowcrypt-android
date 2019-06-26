/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.api.retrofit.node.gson.NodeGson
import com.flowcrypt.email.api.retrofit.response.model.node.BaseMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.Error
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.google.gson.annotations.Expose
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.StringReader

/**
 * It's a result for "parseDecryptMsg" requests.
 *
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 3:48 PM
 * E-mail: DenBond7@gmail.com
 */
data class ParseDecryptedMsgResult constructor(@Expose override val error: Error?,
                                               var msgBlocks: MutableList<MsgBlock>?) :
    BaseNodeResponse {

  override fun handleRawData(bufferedInputStream: BufferedInputStream) {
    var isEnabled = true
    val gson = NodeGson.gson

    if (msgBlocks == null) {
      msgBlocks = mutableListOf()
    }

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
            msgBlocks?.add(block)
          }

          if (c == -1) {
            isEnabled = false
          }
        }
      }
    }
  }

  constructor(source: Parcel) : this(
      source.readParcelable<Error>(Error::class.java.classLoader),
      mutableListOf<MsgBlock>().apply { source.readTypedList(this, BaseMsgBlock.CREATOR) }
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeParcelable(error, 0)
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
