/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   DenBond7
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.SearchBackupsUtil
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.com.sun.mail.imap.canBeUsedToSearchBackups
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.auth.GoogleAuthException
import org.eclipse.angus.mail.imap.IMAPFolder
import jakarta.mail.Folder
import jakarta.mail.MessagingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * This loader finds and returns a user backup of private keys from the mail.
 *
 * @author Denys Bondarenko
 */
class LoadPrivateKeysViewModel(application: Application) : BaseAndroidViewModel(application) {
  val privateKeysLiveData = MutableLiveData<Result<ArrayList<PgpKeyRingDetails>?>>()

  fun fetchAvailableKeys(accountEntity: AccountEntity?) {
    viewModelScope.launch {
      val context: Context = getApplication()
      privateKeysLiveData.value =
        Result.loading(progressMsg = context.getString(R.string.searching_backups))
      if (accountEntity != null) {
        privateKeysLiveData.value = fetchKeys(accountEntity)
      } else {
        privateKeysLiveData.value = Result.exception(NullPointerException("AccountEntity is null!"))
      }
    }
  }

  private suspend fun fetchKeys(accountEntity: AccountEntity): Result<ArrayList<PgpKeyRingDetails>> =
    withContext(Dispatchers.IO) {
      try {
        when (accountEntity.accountType) {
          AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
            GmailApiHelper.executeWithResult {
              Result.success(
                ArrayList(
                  GmailApiHelper.getPrivateKeyBackups(
                    getApplication(),
                    accountEntity
                  )
                )
              )
            }
          }

          else -> Result.success(ArrayList(getPrivateKeyBackupsUsingJavaMailAPI(accountEntity)))
        }
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        Result.exception(e)
      }
    }

  /**
   * Get a list of [PgpKeyRingDetails] using the standard JavaMail API
   *
   * @return A list of [PgpKeyRingDetails]
   * @throws MessagingException
   * @throws IOException
   * @throws GoogleAuthException
   */
  private suspend fun getPrivateKeyBackupsUsingJavaMailAPI(accountEntity: AccountEntity): Collection<PgpKeyRingDetails> =
    withContext(Dispatchers.IO) {
      val details = ArrayList<PgpKeyRingDetails>()
      OpenStoreHelper.openStore(
        getApplication(),
        accountEntity,
        OpenStoreHelper.getAccountSess(getApplication(), accountEntity)
      ).use { store ->
        try {
          val context: Context = getApplication()
          val folders = store.defaultFolder.list("*")

          privateKeysLiveData.postValue(
            Result.loading(
              progressMsg = context.resources
                .getQuantityString(R.plurals.found_folder, folders.size, folders.size),
              progress = 100.0
            )
          )

          for ((index, folder) in folders.withIndex()) {
            if ((folder as IMAPFolder).canBeUsedToSearchBackups()) {
              folder.open(Folder.READ_ONLY)

              val foundMsgs = folder.search(SearchBackupsUtil.genSearchTerms(accountEntity.email))

              for (message in foundMsgs) {
                val backup = EmailUtil.getKeyFromMimeMsg(message)

                if (TextUtils.isEmpty(backup)) {
                  continue
                }

                details.addAll(PgpKey.parseKeys(source = backup).pgpKeyDetailsList.map {
                  it.copy(
                    importInfo = (it.importInfo ?: PgpKeyRingDetails.ImportInfo()).copy(
                      importSourceType = KeyImportDetails.SourceType.EMAIL
                    ),
                  )
                })
              }

              folder.close(false)
            }

            privateKeysLiveData.postValue(
              Result.loading(
                progressMsg = context.getString(
                  R.string.searching_in_folders,
                  index,
                  folders.size
                ),
                progress = (index / folders.size).toDouble()
              )
            )
          }
        } catch (e: Exception) {
          e.printStackTrace()
          throw e
        }
      }
      return@withContext details
    }
}
