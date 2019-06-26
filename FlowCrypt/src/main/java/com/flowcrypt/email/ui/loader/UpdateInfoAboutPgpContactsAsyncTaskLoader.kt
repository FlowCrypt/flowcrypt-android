/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader

import android.content.Context
import android.text.TextUtils
import androidx.loader.content.AsyncTaskLoader
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.ApiService
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor
import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailModel
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailResponse
import com.flowcrypt.email.database.dao.source.ContactsDaoSource
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.model.UpdateInfoAboutPgpContactsResult
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.common.util.CollectionUtils
import java.io.IOException
import java.util.*

/**
 * This loader do next job for all input emails:
 *
 *  * a) look up the email in the database;
 *  * b) if there is a record for that email and has_pgp==true, we can use the `pubkey` instead of
 * querying Attester;
 *  * c) if there is a record but `has_pgp==false`, do `flowcrypt.com/attester/lookup/email` API call
 * to see if you can now get the pubkey. If a pubkey is available, save it back to the database.
 *  * e) no record in the db found:
 *  1. save an empty record eg `new PgpContact(email, null);` - this means we don't know if they have PGP yet
 *  1. look up the email on `flowcrypt.com/attester/lookup/email`
 *  1. if pubkey comes back, create something like `new PgpContact(js, email, null, pubkey,
 * client);`. The PgpContact constructor will define has_pgp, longid, fingerprint, etc
 * for you. Then save that object into database.
 *  1. if no pubkey found, create `new PgpContact(js, email, null, null, null, null);` - this
 * means we know they don't currently have PGP
 *
 * @author DenBond7
 * Date: 19.05.2017
 * Time: 10:50
 * E-mail: DenBond7@gmail.com
 */

class UpdateInfoAboutPgpContactsAsyncTaskLoader(context: Context,
                                                private val emails: List<String>) : AsyncTaskLoader<LoaderResult>(context) {

  init {
    onContentChanged()
  }

  override fun loadInBackground(): LoaderResult? {
    val contactsDaoSource = ContactsDaoSource()
    return getLoaderResult(contactsDaoSource)
  }

  public override fun onStopLoading() {
    cancelLoad()
  }

  public override fun onStartLoading() {
    if (takeContentChanged()) {
      forceLoad()
    }
  }

  private fun getLoaderResult(contactsDaoSource: ContactsDaoSource): LoaderResult {
    var isAllInfoReceived = true
    val pgpContacts = ArrayList<PgpContact>()
    try {
      for (email in emails) {
        if (GeneralUtil.isEmailValid(email)) {
          val emailLowerCase = email.toLowerCase()

          var localPgpContact = contactsDaoSource.getPgpContact(context, emailLowerCase)

          if (localPgpContact == null) {
            localPgpContact = PgpContact(emailLowerCase, null)
            contactsDaoSource.addRow(context, localPgpContact)
          }

          try {
            if (!localPgpContact.hasPgp) {
              val remotePgpContact = getPgpContactInfoFromServer(emailLowerCase)
              if (remotePgpContact != null) {
                contactsDaoSource.updatePgpContact(context, localPgpContact, remotePgpContact)
                localPgpContact = contactsDaoSource.getPgpContact(context, emailLowerCase)
              }
            }
          } catch (e: Exception) {
            isAllInfoReceived = false
            e.printStackTrace()
            ExceptionUtil.handleError(e)
          }

          localPgpContact?.let { pgpContacts.add(it) }
        }
      }
      return LoaderResult(UpdateInfoAboutPgpContactsResult(emails, isAllInfoReceived, pgpContacts), null)
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
      return LoaderResult(null, e)
    }

  }

  /**
   * Get information about [PgpContact] from the remote server.
   *
   * @param email Used to generate a request to the server.
   * @return [PgpContact]
   * @throws IOException
   */
  private fun getPgpContactInfoFromServer(email: String): PgpContact? {
    val response = getLookUpEmailResponse(email)

    if (response != null) {
      if (!TextUtils.isEmpty(response.pubKey)) {
        val client = if (response.hasCryptup()) {
          ContactsDaoSource.CLIENT_FLOWCRYPT
        } else {
          ContactsDaoSource.CLIENT_PGP
        }
        val details = NodeCallsExecutor.parseKeys(response.pubKey!!)
        if (!CollectionUtils.isEmpty(details)) {
          val pgpContact = details[0].primaryPgpContact
          pgpContact.client = client
          return pgpContact
        }
      }
    }

    return null
  }

  /**
   * Get [LookUpEmailResponse] object which contain a remote information about
   * [PgpContact].
   *
   * @param email Used to generate a request to the server.
   * @return [LookUpEmailResponse]
   * @throws IOException
   */
  private fun getLookUpEmailResponse(email: String): LookUpEmailResponse? {
    val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
    val response = apiService.postLookUpEmail(PostLookUpEmailModel(email)).execute()
    return response.body()
  }
}
