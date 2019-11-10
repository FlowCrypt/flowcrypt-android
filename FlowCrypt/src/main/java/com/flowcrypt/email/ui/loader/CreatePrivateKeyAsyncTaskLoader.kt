/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader

import android.content.Context
import androidx.loader.content.AsyncTaskLoader
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.api.email.protocol.SmtpProtocolUtil
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.ApiService
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel
import com.flowcrypt.email.api.retrofit.request.model.TestWelcomeModel
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.dao.KeysDao
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.ActionQueueDaoSource
import com.flowcrypt.email.database.dao.source.KeysDaoSource
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.service.actionqueue.actions.BackupPrivateKeyToInboxAction
import com.flowcrypt.email.service.actionqueue.actions.RegisterUserPublicKeyAction
import com.flowcrypt.email.service.actionqueue.actions.SendWelcomeTestEmailAction
import com.flowcrypt.email.util.exception.ApiException
import com.flowcrypt.email.util.exception.ExceptionUtil
import java.io.IOException
import java.util.*

/**
 * This loader does job of creating a private key and returns the private key long id as result.
 *
 * @author DenBond7
 * Date: 12.01.2018.
 * Time: 12:36.
 * E-mail: DenBond7@gmail.com
 */
class CreatePrivateKeyAsyncTaskLoader(context: Context,
                                      private val account: AccountDao,
                                      private val passphrase: String) : AsyncTaskLoader<LoaderResult>(context) {
  private var isActionStarted: Boolean = false
  private var data: LoaderResult? = null

  public override fun onStartLoading() {
    if (data != null) {
      deliverResult(data)
    } else {
      if (!isActionStarted) {
        forceLoad()
      }
    }
  }

  override fun loadInBackground(): LoaderResult? {
    val email = account.email
    isActionStarted = true
    var nodeKeyDetails: NodeKeyDetails? = null
    try {
      val manager = KeyStoreCryptoManager.getInstance(context)

      val (key) = NodeCallsExecutor.genKey(passphrase, genContacts())
      nodeKeyDetails = key

      val keysDao = KeysDao.generateKeysDao(manager, KeyDetails.Type.NEW, nodeKeyDetails!!, passphrase)

      KeysDaoSource().addRow(context, keysDao)
          ?: return LoaderResult(null, NullPointerException("Cannot save the generated private key"))

      UserIdEmailsKeysDaoSource().addRow(context, nodeKeyDetails.longId!!,
          nodeKeyDetails.primaryPgpContact.email)

      val daoSource = ActionQueueDaoSource()

      if (account.isRuleExist(AccountDao.DomainRule.ENFORCE_ATTESTER_SUBMIT)) {
        val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
        val model = InitialLegacySubmitModel(account.email, nodeKeyDetails.publicKey!!)
        val response = apiService.postInitialLegacySubmit(model).execute()
        val body = response.body()
        body?.apiError?.let { throw ApiException(body.apiError) }

        if (!account.isRuleExist(AccountDao.DomainRule.NO_PRV_BACKUP)) {
          if (!saveCreatedPrivateKeyAsBackupToInbox(nodeKeyDetails)) {
            daoSource.addAction(context, BackupPrivateKeyToInboxAction(0, email, 0, nodeKeyDetails.longId!!))
          }
        }
      } else {
        if (!account.isRuleExist(AccountDao.DomainRule.NO_PRV_BACKUP)) {
          if (!saveCreatedPrivateKeyAsBackupToInbox(nodeKeyDetails)) {
            daoSource.addAction(context, BackupPrivateKeyToInboxAction(0, email, 0, nodeKeyDetails.longId!!))
          }
        }

        if (!registerUserPublicKey(nodeKeyDetails)) {
          daoSource.addAction(context, RegisterUserPublicKeyAction(0, email, 0, nodeKeyDetails.publicKey!!))
        }
      }

      if (!requestingTestMsgWithNewPublicKey(nodeKeyDetails)) {
        daoSource.addAction(context, SendWelcomeTestEmailAction(0, email, 0, nodeKeyDetails.publicKey!!))
      }

      return LoaderResult(nodeKeyDetails.longId, null)
    } catch (e: Exception) {
      e.printStackTrace()
      if (nodeKeyDetails != null) {
        KeysDaoSource().removeKey(context, nodeKeyDetails.longId!!)
        UserIdEmailsKeysDaoSource().removeKey(context, nodeKeyDetails.longId!!)
      }
      ExceptionUtil.handleError(e)
      return LoaderResult(null, e)
    }
  }

  override fun deliverResult(data: LoaderResult?) {
    this.data = data
    super.deliverResult(data)
  }

  /**
   * Perform a backup of the armored key in INBOX.
   *
   * @return true if message was send.
   */
  private fun saveCreatedPrivateKeyAsBackupToInbox(keyDetails: NodeKeyDetails): Boolean {
    try {
      val session = OpenStoreHelper.getAccountSess(context, account)
      val transport = SmtpProtocolUtil.prepareSmtpTransport(context, session, account)
      val msg = EmailUtil.genMsgWithPrivateKeys(context, account, session,
          EmailUtil.genBodyPartWithPrivateKey(account, keyDetails.privateKey!!))
      transport.sendMessage(msg, msg.allRecipients)
    } catch (e: Exception) {
      e.printStackTrace()
      return false
    }

    return true
  }

  private fun genContacts(): List<PgpContact> {
    val pgpContactMain = PgpContact(account.email, account.displayName)
    val contacts = ArrayList<PgpContact>()

    when (account.accountType) {
      AccountDao.ACCOUNT_TYPE_GOOGLE -> {
        contacts.add(pgpContactMain)
        val gmail = GmailApiHelper.generateGmailApiService(context, account)
        val aliases = gmail.users().settings().sendAs().list(GmailApiHelper.DEFAULT_USER_ID).execute()
        for (alias in aliases.sendAs) {
          if (alias.verificationStatus != null) {
            contacts.add(PgpContact(alias.sendAsEmail, alias.displayName))
          }
        }
      }

      else -> contacts.add(pgpContactMain)
    }

    return contacts
  }

  /**
   * Registering a key with attester API.
   * Note: this will only be successful if it's the first time submitting a key for this email address, or if the
   * key being submitted has the same fingerprint as the one already recorded. If it's an error due to key
   * conflict, ignore the error.
   *
   * @param keyDetails Details of the created key.
   * @return true if no errors.
   */
  private fun registerUserPublicKey(keyDetails: NodeKeyDetails): Boolean {
    return try {
      val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
      val model = InitialLegacySubmitModel(account.email, keyDetails.publicKey!!)
      val response = apiService.postInitialLegacySubmit(model).execute()
      val body = response.body()
      body != null && (body.apiError == null || body.apiError.code !in 400..499)
    } catch (e: IOException) {
      e.printStackTrace()
      false
    }
  }

  /**
   * Request a test email from FlowCrypt.
   *
   * @param keyDetails Details of the created key.
   * @return true if no errors.
   */
  private fun requestingTestMsgWithNewPublicKey(keyDetails: NodeKeyDetails): Boolean {
    return try {
      val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
      val model = TestWelcomeModel(account.email, keyDetails.publicKey!!)
      val response = apiService.postTestWelcome(model).execute()

      val testWelcomeResponse = response.body()
      testWelcomeResponse != null && testWelcomeResponse.isSent
    } catch (e: IOException) {
      e.printStackTrace()
      false
    }
  }
}
