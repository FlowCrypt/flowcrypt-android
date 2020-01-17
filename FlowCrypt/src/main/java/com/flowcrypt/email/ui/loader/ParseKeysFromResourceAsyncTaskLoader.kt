/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import androidx.loader.content.AsyncTaskLoader
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.model.KeyImportModel
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import java.util.*

/**
 * This loader parses keys from the given resource (string or file).
 *
 * @author Denis Bondarenko
 * Date: 13.08.2018
 * Time: 13:00
 * E-mail: DenBond7@gmail.com
 */

class ParseKeysFromResourceAsyncTaskLoader(context: Context,
                                           private val keyImportModel: KeyImportModel?,
                                           private val isCheckSizeEnabled: Boolean) :
    AsyncTaskLoader<LoaderResult>(context) {

  init {
    onContentChanged()
  }

  public override fun onStartLoading() {
    if (takeContentChanged()) {
      forceLoad()
    }
  }

  override fun loadInBackground(): LoaderResult? {
    val list = ArrayList<NodeKeyDetails>()
    try {
      if (keyImportModel != null) {
        val armoredKey: String?
        when (keyImportModel.type) {
          KeyDetails.Type.FILE -> {
            if (isCheckSizeEnabled && isKeyTooBig(keyImportModel.fileUri)) {
              return LoaderResult(null, IllegalArgumentException("The file is too big"))
            }

            if (keyImportModel.fileUri == null) {
              throw NullPointerException("Uri is null!")
            }

            armoredKey = GeneralUtil.readFileFromUriToString(context, keyImportModel.fileUri)
          }

          KeyDetails.Type.CLIPBOARD, KeyDetails.Type.EMAIL -> armoredKey = keyImportModel.keyString
          else -> throw IllegalStateException("Unsupported : ${keyImportModel.type}")
        }

        if (!TextUtils.isEmpty(armoredKey)) {
          list.addAll(NodeCallsExecutor.parseKeys(armoredKey!!))
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
      return LoaderResult(null, e)
    }

    return LoaderResult(list, null)
  }

  public override fun onStopLoading() {
    cancelLoad()
  }

  /**
   * Check that the key size not bigger then [.MAX_SIZE_IN_BYTES].
   *
   * @param fileUri The [Uri] of the selected file.
   * @return true if the key size not bigger then [.MAX_SIZE_IN_BYTES], otherwise false
   */
  private fun isKeyTooBig(fileUri: Uri?): Boolean {
    return GeneralUtil.getFileSizeFromUri(context, fileUri) > MAX_SIZE_IN_BYTES
  }

  companion object {
    /**
     * Max size of a key is 256k.
     */
    private const val MAX_SIZE_IN_BYTES = 256 * 1024
  }
}
