/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import android.content.Context
import android.text.TextUtils
import androidx.annotation.WorkerThread
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.LabelEntity
import com.sun.mail.imap.IMAPFolder
import java.util.*
import javax.mail.MessagingException

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
  fun addFolder(imapFolder: IMAPFolder?, folderAlias: String) {
    imapFolder?.let {
      if (!EmailUtil.containsNoSelectAttr(it) && !TextUtils.isEmpty(it.fullName) && !folders.containsKey(it.fullName)) {
        this.folders[prepareFolderKey(it)] = generateFolder(account, it, folderAlias)
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
    if (localFolder != null && !TextUtils.isEmpty(localFolder.fullName) && !folders.containsKey(localFolder.fullName)) {
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

  fun findInboxFolder(): LocalFolder? {
    for (localFolder in allFolders) {
      if (localFolder.fullName.equals(JavaEmailConstants.FOLDER_INBOX, ignoreCase = true)) {
        return localFolder
      }
    }

    return null
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
      localFolders.remove(it)
      sortedList[0] = it.folderAlias
      it
    }

    folderTrash?.let {
      localFolders.remove(it)
      sortedList[localFolders.size + 1] = it.folderAlias
    }

    folderSpam?.let {
      localFolders.remove(it)
      sortedList[localFolders.size + 1] = it.folderAlias
    }

    folderOutbox?.let {
      localFolders.remove(it)
      sortedList[localFolders.size + 1] = it.folderAlias
    }

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
     * @param accountName The name of an account.
     * @return The new [FoldersManager].
     */
    @WorkerThread
    fun fromDatabase(context: Context, accountName: String): FoldersManager {
      val appContext = context.applicationContext
      return build(accountName, FlowCryptRoomDatabase.getDatabase(appContext).labelDao().getLabels(accountName))
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
      return LocalFolder(account, imapFolder.fullName, folderAlias, Arrays.asList(*imapFolder
          .attributes), isCustom(imapFolder), 0, "")
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
     * @param localFolder Some [javax.mail.Folder].
     * @return [FolderType].
     */
    fun getFolderType(localFolder: LocalFolder?): FolderType? {
      val folderTypes = FolderType.values()

      if (localFolder != null) {
        val attributes = localFolder.attributes

        if (attributes != null) {
          for (attribute in attributes) {
            for (folderType in folderTypes) {
              if (folderType.value == attribute) {
                return folderType
              }
            }
          }
        }

        if (!TextUtils.isEmpty(localFolder.fullName)) {
          if (JavaEmailConstants.FOLDER_INBOX.equals(localFolder.fullName, ignoreCase = true)) {
            return FolderType.INBOX
          }
        }

        if (!TextUtils.isEmpty(localFolder.fullName)) {
          if (JavaEmailConstants.FOLDER_OUTBOX.equals(localFolder.fullName, ignoreCase = true)) {
            return FolderType.OUTBOX
          }
        }
      }
      return null
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
