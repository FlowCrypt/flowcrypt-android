/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.retrofit.node.NodeRepository
import com.flowcrypt.email.api.retrofit.node.PgpApiRepository
import com.flowcrypt.email.api.retrofit.request.node.ParseDecryptMsgRequest
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.node.ParseDecryptedMsgResult
import com.flowcrypt.email.security.KeysStorageImpl
import com.sun.mail.util.ASCIIUtility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.IOUtils
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * This [ViewModel] implementation can be used to parse and decrypt (if needed) an incoming message.
 *
 * @author Denis Bondarenko
 * Date: 3/21/19
 * Time: 11:47 AM
 * E-mail: DenBond7@gmail.com
 */
class DecryptMessageViewModel(application: Application) : BaseNodeApiViewModel(application),
    KeysStorageImpl.OnRefreshListener {
  val headersLiveData: MutableLiveData<String> = MutableLiveData()
  val decryptLiveData: MutableLiveData<Result<ParseDecryptedMsgResult?>> = MutableLiveData()
  private val keysStorage: KeysStorageImpl = KeysStorageImpl.getInstance(application)
  private val apiRepository: PgpApiRepository = NodeRepository()

  init {
    this.keysStorage.attachOnRefreshListener(this)
  }

  override fun onRefresh() {}

  fun decryptMessage(rawMimeBytes: ByteArray) {
    decryptLiveData.value = Result.loading()
    viewModelScope.launch {
      headersLiveData.value = getHeaders(ByteArrayInputStream(rawMimeBytes))
      val pgpKeyInfoList = keysStorage.getAllPgpPrivateKeys()
      val result = apiRepository.parseDecryptMsg(
          request = ParseDecryptMsgRequest(data = rawMimeBytes, pgpKeyInfos = pgpKeyInfoList, isEmail = true))
      decryptLiveData.value = result
    }
  }

  fun decryptMessage(context: Context, uri: Uri) {
    decryptLiveData.value = Result.loading()
    viewModelScope.launch {
      headersLiveData.value = getHeaders(context.contentResolver.openInputStream(uri))
      val pgpKeyInfoList = keysStorage.getAllPgpPrivateKeys()
      val result = apiRepository.parseDecryptMsg(
          request = ParseDecryptMsgRequest(context = context, uri = uri, pgpKeyInfos = pgpKeyInfoList, isEmail = true))
      decryptLiveData.value = result
    }
  }

  /**
   * We fetch the first 50Kb from the given input stream and extract headers.
   */
  private suspend fun getHeaders(inputStream: InputStream?): String =
      withContext(Dispatchers.IO) {
        inputStream ?: return@withContext ""
        val d = ByteArray(50000)
        IOUtils.read(inputStream, d)
        EmailUtil.getHeadersFromRawMIME(ASCIIUtility.toString(d))
      }

}
