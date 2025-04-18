/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import android.content.Context
import android.text.TextUtils
import androidx.annotation.WorkerThread
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.LabelEntity
import com.google.api.services.gmail.model.Label
import org.eclipse.angus.mail.imap.IMAPFolder
import jakarta.mail.MessagingException
import java.util.LinkedList

/**
 * The [FoldersManager] describes a logic of work with remote folders. This class helps as
 * resolve problems with localized names of Gmail labels.
 *
 * @author Denys Bondarenko
 */
class FoldersManager constructor(val accountEntity: AccountEntity) {
  private var folders: LinkedHashMap<String, LocalFolder> = LinkedHashMap()

  val folderInbox: LocalFolder?
    get() = folders[FolderType.INBOX.value]

  val folderArchive: LocalFolder?
    get() = folders[FolderType.All.value]

  val folderDrafts: LocalFolder?
    get() = folders[FolderType.DRAFTS.value]

  val folderStarred: LocalFolder?
    get() = folders[FolderType.STARRED.value]

  val folderSpam: LocalFolder?
    get() = folders[FolderType.JUNK.value] ?: folders[FolderType.SPAM.value]

  val folderSent: LocalFolder?
    get() = folders[FolderType.SENT.value]

  val folderTrash: LocalFolder?
    get() = folders[FolderType.TRASH.value]

  val folderAll: LocalFolder?
    get() = folders[FolderType.All.value]

  val folderImportant: LocalFolder?
    get() = folders[FolderType.IMPORTANT.value]

  val allFolders: Collection<LocalFolder>
    get() = folders.values

  /**
   * Get a list of all available custom labels.
   *
   * @return List of custom labels([LocalFolder]).
   */
  val customLabels: List<LocalFolder>
    get() {
      val customLocalFolders = LinkedList<LocalFolder>()

      for (folder in folders) {
        if (folder.value.isCustom) {
          customLocalFolders.add(folder.value)
        }
      }

      return customLocalFolders
    }

  /**
   * Get a list of original server [LocalFolder] objects.
   *
   * @return a list of original server [LocalFolder] objects.
   */
  val serverFolders: List<LocalFolder>
    get() {
      val serverLocalFolders = LinkedList<LocalFolder>()

      for (folder in folders) {
        if (!folder.value.isCustom) {
          serverLocalFolders.add(folder.value)
        }
      }

      return serverLocalFolders
    }

  val folderOutbox: LocalFolder?
    get() = folders[FolderType.OUTBOX.value]

  /**
   * Clear the folders list.
   */
  fun clear() {
    folders.clear()
  }

  /**
   * Add a new folder to [FoldersManager] to manage it.
   *
   * @param imapFolder  The [IMAPFolder] object which contains information about a
   * remote folder.
   * @throws MessagingException
   */
  fun addFolder(imapFolder: IMAPFolder?) {
    imapFolder?.let {
      if (!EmailUtil.containsNoSelectAttr(it) && !TextUtils.isEmpty(it.fullName) && !folders.containsKey(
          it.fullName
        )
      ) {
        this.folders[prepareFolderKey(it)] = generateFolder(accountEntity.email, it)
      }
    }
  }

  /**
   * Add a new folder to [FoldersManager] to manage it.
   *
   * @param gMailLabel  The [Label] object which contains information about a remote folder.
   */
  fun addFolder(gMailLabel: Label?) {
    gMailLabel?.let {
      if (it.id.isNotEmpty() && !folders.containsKey(it.id)) {
        this.folders[prepareFolderKey(it)] = generateFolder(accountEntity.email, it)
      }
    }
  }

  /**
   * Add a new folder to [FoldersManager] to manage it.
   *
   * @param localFolder The [LocalFolder] object which contains information about a
   * remote folder.
   */
  fun addFolder(localFolder: LocalFolder?) {
    if (localFolder != null && !TextUtils.isEmpty(localFolder.fullName) && !folders.containsKey(
        localFolder.fullName
      )
    ) {
      this.folders[prepareFolderKey(localFolder)] = localFolder
    }
  }

  /**
   * Update [FoldersManager] to use fresh data.
   */
  fun swapFolders(list: Collection<LabelEntity>) {
    clear()
    list.forEach {
      addFolder(LocalFolder(it))
    }
  }

  /**
   * Get [LocalFolder] by the alias name.
   *
   * @param folderAlias The folder alias name.
   * @return [LocalFolder].
   */
  fun getFolderByAlias(folderAlias: String?): LocalFolder? {
    folderAlias ?: return null

    for (folder in folders) {
      if (folder.value.folderAlias?.equals(folderAlias) == true) {
        return folder.value
      }
    }

    return null
  }

  /**
   * Get [LocalFolder] by the full name.
   *
   * @param fullName The folder full name.
   * @return [LocalFolder].
   */
  fun getFolderByFullName(fullName: String?): LocalFolder? {
    fullName ?: return null

    for (folder in folders) {
      if (folder.value.fullName == fullName) {
        return folder.value
      }
    }

    return null
  }

  fun findInboxFolder(): LocalFolder? {
    for (localFolder in allFolders) {
      if (localFolder.fullName.equals(JavaEmailConstants.FOLDER_INBOX, ignoreCase = true)) {
        return localFolder
      }
    }

    return null
  }

  /**
   * We use this method instead of use [folderSent] because some email providers don't have the
   * Sent folder with right attributes.
   */
  fun findSentFolder(): LocalFolder? {
    var sentFolder = folderSent

    sentFolder?.let { return it }

    for (localFolder in allFolders) {
      if (localFolder.fullName.uppercase() in arrayOf("INBOX/SENT", "SENT")) {
        sentFolder = localFolder
      }
    }

    return sentFolder
  }

  /**
   * Sort the server folders for a better user experience.
   *
   * @return The server folders.
   */
  fun getSortedServerFolders(): Collection<LocalFolder> {
    val localFolders = serverFolders.toMutableList()
    val sortedList = arrayOfNulls<LocalFolder>(localFolders.size)

    val inbox = folderInbox?.let {
      sortedList[0] = it
      localFolders.remove(it)
      it
    }

    val moveFolder = fun(localFolder: LocalFolder) {
      sortedList[localFolders.size] = localFolder
      localFolders.remove(localFolder)
    }

    folderTrash?.let { moveFolder(it) }
    folderSpam?.let { moveFolder(it) }
    folderOutbox?.let { moveFolder(it) }

    for (i in localFolders.indices) {
      val localFolder = localFolders[i]
      if (inbox == null) {
        sortedList[i] = localFolder
      } else {
        sortedList[i + 1] = localFolder
      }
    }

    return sortedList.filterNotNull().toList()
  }

  private fun prepareFolderKey(imapFolder: IMAPFolder): String {
    val folderType = getFolderType(generateFolder(accountEntity.email, imapFolder))
    return folderType?.value ?: imapFolder.fullName
  }

  private fun prepareFolderKey(label: Label): String {
    val folderType = getFolderType(generateFolder(accountEntity.email, label))
    return folderType?.value ?: label.id
  }

  private fun prepareFolderKey(localFolder: LocalFolder): String {
    val folderType = getFolderType(localFolder)
    return folderType?.value ?: localFolder.fullName
  }

  /**
   * This class contains information about all servers folders types.
   */
  enum class FolderType constructor(val value: String) {
    INBOX("INBOX"),
    All("\\All"),
    ARCHIVE("\\Archive"),
    DRAFTS("\\Drafts"),
    STARRED("\\Flagged"),
    JUNK("\\Junk"),
    SPAM("\\Spam"),
    SENT("\\Sent"),
    TRASH("\\Trash"),
    IMPORTANT("\\Important"),
    OUTBOX("\\Outbox")
  }

  companion object {

    /**
     * Generate a new [FoldersManager] using information from the local database. Should be
     * called from a background thread only.
     *
     * @param context     Interface to global information about an application environment.
     * @param accountEntity [AccountEntity].
     * @return The new [FoldersManager].
     */
    @WorkerThread
    fun fromDatabase(context: Context, accountEntity: AccountEntity): FoldersManager {
      val appContext = context.applicationContext
      return build(
        accountEntity,
        FlowCryptRoomDatabase.getDatabase(appContext).labelDao()
          .getLabels(accountEntity.email, accountEntity.accountType)
      )
    }

    /**
     * Generate a new [FoldersManager] using information from the local database. Should be
     * called from a background thread only.
     *
     * @param context     Interface to global information about an application environment.
     * @param accountEntity An account.
     * @return The new [FoldersManager].
     */
    @WorkerThread
    suspend fun fromDatabaseSuspend(
      context: Context,
      accountEntity: AccountEntity
    ): FoldersManager {
      val appContext = context.applicationContext
      return build(
        accountEntity, FlowCryptRoomDatabase.getDatabase(appContext).labelDao()
          .getLabelsSuspend(accountEntity.email, accountEntity.accountType)
      )
    }

    /**
     * Generate a new [LocalFolder]
     *
     * @param imapFolder  The [IMAPFolder] object which contains information about a
     * remote folder.
     * @return
     * @throws MessagingException
     */
    fun generateFolder(account: String, imapFolder: IMAPFolder): LocalFolder {
      val isCustom = isCustom(imapFolder)
      return LocalFolder(
        account = account,
        fullName = imapFolder.fullName,
        folderAlias = if (isCustom) imapFolder.fullName else imapFolder.name,
        attributes = listOf(*imapFolder.attributes),
        isCustom = isCustom,
        msgCount = 0,
        searchQuery = ""
      )
    }

    /**
     * Generate a new [LocalFolder]
     *
     * @param label  The [Label] object which contains information about a remote folder.
     */
    fun generateFolder(account: String, label: Label): LocalFolder {
      return LocalFolder(
        account = account,
        fullName = label.id,
        folderAlias = label.name,
        attributes = emptyList(),
        isCustom = label.type.equals(GmailApiHelper.FOLDER_TYPE_USER, true),
        msgCount = 0,
        searchQuery = "",
        labelColor = label.color?.backgroundColor,
        textColor = label.color?.textColor,
        labelListVisibility = runCatching {
          LabelEntity.LabelListVisibility.findByValue(label.labelListVisibility)
        }.getOrDefault(LabelEntity.LabelListVisibility.SHOW)
      )
    }

    /**
     * Check if current folder is a custom label.
     *
     * @param folder The [IMAPFolder] object which contains information about a
     * remote folder.
     * @return true if this label is a custom, false otherwise.
     * @throws MessagingException
     */
    private fun isCustom(folder: IMAPFolder): Boolean {
      val attr = folder.attributes
      val folderTypes = FolderType.values()

      for (attribute in attr) {
        for (folderType in folderTypes) {
          if (folderType.value == attribute) {
            return false
          }
        }
      }

      return !FolderType.INBOX.value.equals(folder.fullName, ignoreCase = true)
    }

    /**
     * Get a [FolderType] using folder attributes.
     *
     * @param localFolder Some [jakarta.mail.Folder].
     * @return [FolderType].
     */
    fun getFolderType(localFolder: LocalFolder?): FolderType? {
      val folderTypes = FolderType.values()
      val attributes = localFolder?.attributes ?: emptyList()

      for (attribute in attributes) {
        for (folderType in folderTypes) {
          if (folderType.value == attribute) {
            return folderType
          }
        }
      }

      return when {
        JavaEmailConstants.FOLDER_INBOX.equals(
          localFolder?.fullName,
          ignoreCase = true
        ) -> FolderType.INBOX

        JavaEmailConstants.FOLDER_ALL_MAIL.equals(
          localFolder?.fullName,
          ignoreCase = true
        ) -> FolderType.All

        JavaEmailConstants.FOLDER_OUTBOX.equals(
          localFolder?.fullName,
          ignoreCase = true
        ) -> FolderType.OUTBOX

        JavaEmailConstants.FOLDER_SENT.equals(
          localFolder?.fullName,
          ignoreCase = true
        ) -> FolderType.SENT

        JavaEmailConstants.FOLDER_TRASH.equals(
          localFolder?.fullName,
          ignoreCase = true
        ) -> FolderType.TRASH

        JavaEmailConstants.FOLDER_DRAFT.equals(
          localFolder?.fullName,
          ignoreCase = true
        ) -> FolderType.DRAFTS

        JavaEmailConstants.FOLDER_STARRED.equals(
          localFolder?.fullName,
          ignoreCase = true
        ) -> FolderType.STARRED

        JavaEmailConstants.FOLDER_IMPORTANT.equals(
          localFolder?.fullName,
          ignoreCase = true
        ) -> FolderType.IMPORTANT

        JavaEmailConstants.FOLDER_SPAM.equals(
          localFolder?.fullName,
          ignoreCase = true
        ) -> FolderType.SPAM

        else -> null
      }
    }

    fun getFolderIconResourceId(localFolder: LocalFolder?, isGoogleSignInAccount: Boolean): Int {
      return when (localFolder?.getFolderType()) {
        FolderType.INBOX -> R.drawable.ic_mail_24dp
        FolderType.All -> R.drawable.ic_mails_24dp
        FolderType.ARCHIVE -> R.drawable.ic_mails_24dp
        FolderType.SENT -> R.drawable.ic_send_24dp
        FolderType.IMPORTANT -> R.drawable.ic_important_24dp
        FolderType.TRASH -> R.drawable.ic_trash_24dp
        FolderType.DRAFTS -> R.drawable.ic_drafts_24dp
        FolderType.SPAM, FolderType.JUNK -> R.drawable.ic_spam_24dp
        FolderType.STARRED -> R.drawable.ic_star_24dp
        FolderType.OUTBOX -> R.drawable.ic_outgoing_24dp

        else -> when {
          //handle Gmail variants which IMAP doesn't have
          isGoogleSignInAccount && "UNREAD".equals(
            localFolder?.fullName,
            ignoreCase = true
          ) -> R.drawable.ic_email_unread_24dp

          else -> R.drawable.ic_label_24dp
        }
      }
    }

    /**
     * Generate a new [FoldersManager] using information from the local database.
     *
     * @param accountEntity An account.
     * @return a new [FoldersManager].
     */
    fun build(accountEntity: AccountEntity, labels: List<LabelEntity>): FoldersManager {
      val foldersManager = FoldersManager(accountEntity)

      labels.forEach { label ->
        foldersManager.addFolder(LocalFolder(label))
      }

      return foldersManager
    }
  }
}
