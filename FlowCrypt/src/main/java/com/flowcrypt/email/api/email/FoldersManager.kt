/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import android.content.Context
import android.text.TextUtils
import androidx.annotation.WorkerThread
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.LabelEntity
import com.google.api.services.gmail.model.Label
import com.sun.mail.imap.IMAPFolder
import jakarta.mail.MessagingException
import java.util.*

/**
 * The [FoldersManager] describes a logic of work with remote folders. This class helps as
 * resolve problems with localized names of Gmail labels.
 *
 * @author DenBond7
 * Date: 07.06.2017
 * Time: 14:37
 * E-mail: DenBond7@gmail.com
 */
class FoldersManager constructor(val account: String) {
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
    get() {
      val spam = folders[FolderType.JUNK.value]
      return spam ?: folders[FolderType.SPAM.value]
    }

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
   * @param folderAlias The folder alias.
   * @throws MessagingException
   */
  fun addFolder(imapFolder: IMAPFolder?) {
    imapFolder?.let {
      if (!EmailUtil.containsNoSelectAttr(it) && !TextUtils.isEmpty(it.fullName) && !folders.containsKey(
          it.fullName
        )
      ) {
        this.folders[prepareFolderKey(it)] = generateFolder(account, it, imapFolder.name)
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
        this.folders[prepareFolderKey(it)] = generateFolder(account, it)
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
   * @return The sorted labels list.
   */
  fun getSortedNames(): Collection<String> {
    val localFolders = serverFolders.toMutableList()
    val sortedList = arrayOfNulls<String>(localFolders.size)

    val inbox = folderInbox?.let {
      sortedList[0] = it.folderAlias
      localFolders.remove(it)
      it
    }

    val moveFolder = fun(localFolder: LocalFolder) {
      sortedList[localFolders.size] = localFolder.folderAlias
      localFolders.remove(localFolder)
    }

    folderTrash?.let { moveFolder(it) }
    folderSpam?.let { moveFolder(it) }
    folderOutbox?.let { moveFolder(it) }

    for (i in localFolders.indices) {
      val localFolder = localFolders[i]
      if (inbox == null) {
        sortedList[i] = localFolder.folderAlias
      } else {
        sortedList[i + 1] = localFolder.folderAlias
      }
    }

    return sortedList.filterNotNull()
  }

  private fun prepareFolderKey(imapFolder: IMAPFolder): String {
    val folderType = getFolderType(generateFolder(account, imapFolder, null))
    return folderType?.value ?: imapFolder.fullName
  }

  private fun prepareFolderKey(label: Label): String {
    val folderType = getFolderType(generateFolder(account, label))
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
        accountEntity.email,
        FlowCryptRoomDatabase.getDatabase(appContext).labelDao()
          .getLabels(accountEntity.email, accountEntity.accountType)
      )
    }

    /**
     * Generate a new [FoldersManager] using information from the local database. Should be
     * called from a background thread only.
     *
     * @param context     Interface to global information about an application environment.
     * @param accountName The name of an account.
     * @return The new [FoldersManager].
     */
    @WorkerThread
    suspend fun fromDatabaseSuspend(
      context: Context,
      accountEntity: AccountEntity
    ): FoldersManager {
      val appContext = context.applicationContext
      return build(
        accountEntity.email, FlowCryptRoomDatabase.getDatabase(appContext).labelDao()
          .getLabelsSuspend(accountEntity.email, accountEntity.accountType)
      )
    }

    /**
     * Generate a new [LocalFolder]
     *
     * @param imapFolder  The [IMAPFolder] object which contains information about a
     * remote folder.
     * @param folderAlias The folder alias.
     * @return
     * @throws MessagingException
     */
    fun generateFolder(account: String, imapFolder: IMAPFolder, folderAlias: String?): LocalFolder {
      return LocalFolder(
        account, imapFolder.fullName, folderAlias, Arrays.asList(
          *imapFolder
            .attributes
        ), isCustom(imapFolder), 0, ""
      )
    }

    /**
     * Generate a new [LocalFolder]
     *
     * @param label  The [Label] object which contains information about a remote folder.
     * @param folderAlias The folder alias.
     */
    fun generateFolder(account: String, label: Label): LocalFolder {
      return LocalFolder(
        account = account,
        fullName = label.id,
        folderAlias = label.name,
        attributes = emptyList(),
        isCustom = label.type == GmailApiHelper.FOLDER_TYPE_USER,
        msgCount = 0,
        searchQuery = ""
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
    fun isCustom(folder: IMAPFolder): Boolean {
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

    /**
     * Generate a new [FoldersManager] using information from the local database.
     *
     * @param accountName The name of an account.
     * @return a new [FoldersManager].
     */
    fun build(accountName: String, labels: List<LabelEntity>): FoldersManager {
      val foldersManager = FoldersManager(accountName)

      labels.forEach { label ->
        foldersManager.addFolder(LocalFolder(label))
      }

      return foldersManager
    }
  }
}
