/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager.sync

import android.content.Context
import android.text.TextUtils
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.gmail.api.GmaiAPIMimeMessage
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.model.EmailAndNamePair
import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.*
import javax.mail.FetchProfile
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.Store
import javax.mail.internet.InternetAddress

/**
 * This [CoroutineWorker] loads information about contacts from the SENT folder.
 *
 * @author Denis Bondarenko
 * Date: 23.04.2018
 * Time: 14:53
 * E-mail: DenBond7@gmail.com
 */
class LoadContactsWorker(context: Context, params: WorkerParameters) :
  BaseSyncWorker(context, params) {
  override suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store) {
    fetchContacts(accountEntity, store)
  }

  override suspend fun runAPIAction(accountEntity: AccountEntity) {
    fetchContacts(accountEntity)
  }

  private suspend fun fetchContacts(account: AccountEntity, store: Store) =
    withContext(Dispatchers.IO) {
      fetchContactsInternal(account) {
        val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, account)
        val folderSent =
          foldersManager.findSentFolder() ?: return@fetchContactsInternal emptyArray()

        store.getFolder(folderSent.fullName).use { folder ->
          val imapFolder = (folder as IMAPFolder).apply { open(Folder.READ_ONLY) }
          val msgs = imapFolder.messages

          if (msgs.isNotEmpty()) {
            val fetchProfile = FetchProfile()
            fetchProfile.add(Message.RecipientType.TO.toString().uppercase(Locale.getDefault()))
            fetchProfile.add(Message.RecipientType.CC.toString().uppercase(Locale.getDefault()))
            fetchProfile.add(Message.RecipientType.BCC.toString().uppercase(Locale.getDefault()))
            imapFolder.fetch(msgs, fetchProfile)

            return@fetchContactsInternal msgs
          }
        }

        return@fetchContactsInternal emptyArray()
      }
    }

  private suspend fun fetchContacts(account: AccountEntity) = withContext(Dispatchers.IO) {
    fetchContactsInternal(account) {
      val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, account)
      val folderSent = foldersManager.findSentFolder() ?: return@fetchContactsInternal emptyArray()

      executeGMailAPICall(applicationContext) {
        val gmailApiService = GmailApiHelper.generateGmailApiService(applicationContext, account)

        var response = gmailApiService
          .users()
          .messages()
          .list(GmailApiHelper.DEFAULT_USER_ID)
          .setLabelIds(listOf(GmailApiHelper.LABEL_SENT))
          .execute()

        val msgsBaseInfo = mutableListOf<com.google.api.services.gmail.model.Message>()

        //Try to load the last 200 messages. Only base info
        while (response.messages != null) {
          //to prevent limit exception we can do it once per 1 seconds
          delay(1000)
          msgsBaseInfo.addAll(response.messages)
          if (msgsBaseInfo.size < MAX_MSGS_COUNT && response.nextPageToken != null) {
            response = gmailApiService
              .users()
              .messages()
              .list(GmailApiHelper.DEFAULT_USER_ID)
              .setPageToken(response.nextPageToken)
              .execute()
          } else {
            break
          }
        }

        val list = mutableListOf<com.google.api.services.gmail.model.Message>()
        list.addAll(
          GmailApiHelper.doOperationViaStepsSuspend(
            stepValue = 50,
            list = msgsBaseInfo
          ) { subList ->
            //to prevent limit exception we can do it once per 1 seconds
            delay(1000)
            GmailApiHelper.loadMsgs(
              context = applicationContext,
              accountEntity = account,
              messages = subList,
              localFolder = folderSent,
              format = GmailApiHelper.MESSAGE_RESPONSE_FORMAT_METADATA,
              metadataHeaders = listOf("To", "Cc"),
              fields = listOf("payload")
            )
          })

        val session = Session.getInstance(Properties())
        list.map { GmaiAPIMimeMessage(session, it) }.toTypedArray()
      }
    }
  }

  private suspend fun fetchContactsInternal(
    account: AccountEntity,
    action: suspend () -> Array<Message>
  ) = withContext(Dispatchers.IO) {
    if (account.contactsLoaded == true) return@withContext
    val msgs = action.invoke()

    if (msgs.isNotEmpty()) {
      updateContacts(msgs)
      FlowCryptRoomDatabase.getDatabase(applicationContext).accountDao()
        .updateAccountSuspend(account.copy(contactsLoaded = true))
    }
  }

  private suspend fun updateContacts(msgs: Array<Message>) = withContext(Dispatchers.IO) {
    val emailAndNamePairs = mutableListOf<EmailAndNamePair>()
    for (msg in msgs) {
      emailAndNamePairs.addAll(parseRecipients(msg, Message.RecipientType.TO))
      emailAndNamePairs.addAll(parseRecipients(msg, Message.RecipientType.CC))
      emailAndNamePairs.addAll(parseRecipients(msg, Message.RecipientType.BCC))
    }

    val contactsDao = FlowCryptRoomDatabase.getDatabase(applicationContext).recipientDao()
    val availableContacts = contactsDao.getAllContacts()

    val contactsInDatabase = HashSet<String>()
    val contactsWhichWillBeUpdated = HashSet<String>()
    val contactsWhichWillBeCreated = HashSet<String>()
    val contactsByEmailMap = HashMap<String, RecipientEntity?>()

    val newCandidates = mutableListOf<RecipientEntity>()
    val updateCandidates = mutableListOf<RecipientEntity>()

    for (contact in availableContacts) {
      contactsInDatabase.add(contact.email.lowercase())
      contactsByEmailMap[contact.email.lowercase()] = contact
    }

    for (emailAndNamePair in emailAndNamePairs) {
      if (contactsInDatabase.contains(emailAndNamePair.email)) {
        val contactEntity = contactsByEmailMap[emailAndNamePair.email]
        if (contactEntity?.email.isNullOrEmpty()) {
          if (!contactsWhichWillBeUpdated.contains(emailAndNamePair.email)) {
            emailAndNamePair.email?.let {
              contactsWhichWillBeUpdated.add(it)
            }
            contactEntity?.copy(name = emailAndNamePair.name)?.let { updateCandidates.add(it) }
          }
        }
      } else {
        if (!contactsWhichWillBeCreated.contains(emailAndNamePair.email)) {
          emailAndNamePair.email?.let {
            contactsWhichWillBeCreated.add(it)
            newCandidates.add(
              RecipientEntity(
                email = it,
                name = emailAndNamePair.name
              )
            )
          }
        }
      }
    }

    contactsDao.updateSuspend(updateCandidates)
    contactsDao.insertSuspend(newCandidates)
  }

  /**
   * Generate an array of [EmailAndNamePair] objects from the input message.
   * This information will be retrieved from "to", "cc" or "bcc" headers.
   *
   * @param msg           The input [Message].
   * @param recipientType The input [Message.RecipientType].
   * @return An array of EmailAndNamePair objects, which contains information about emails and names.
   */
  private suspend fun parseRecipients(
    msg: Message?,
    recipientType: Message.RecipientType?
  ): List<EmailAndNamePair> = withContext(Dispatchers.IO) {
    if (msg != null && recipientType != null) {
      try {
        val header = msg.getHeader(recipientType.toString()) ?: return@withContext emptyList()
        if (header.isNotEmpty()) {
          if (!TextUtils.isEmpty(header[0])) {
            val addresses = InternetAddress.parse(header[0])
            val emailAndNamePairs = mutableListOf<EmailAndNamePair>()
            for (address in addresses) {
              emailAndNamePairs.add(
                EmailAndNamePair(
                  address.address.lowercase(Locale.getDefault()), address.personal
                )
              )
            }

            return@withContext emailAndNamePairs
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }

      return@withContext emptyList()
    } else {
      return@withContext emptyList()
    }
  }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".FETCH_CONTACTS"
    private const val MAX_MSGS_COUNT = 200

    fun enqueue(context: Context) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniqueWork(
          GROUP_UNIQUE_TAG,
          ExistingWorkPolicy.KEEP,
          OneTimeWorkRequestBuilder<LoadContactsWorker>()
            .addTag(TAG_SYNC)
            .setConstraints(constraints)
            .build()
        )
    }
  }
}
