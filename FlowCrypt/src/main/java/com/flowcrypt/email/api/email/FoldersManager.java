/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource;
import com.sun.mail.imap.IMAPFolder;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;

/**
 * The {@link FoldersManager} describes a logic of work with remote folders. This class helps as
 * resolve problems with localized names of Gmail labels.
 *
 * @author DenBond7
 * Date: 07.06.2017
 * Time: 14:37
 * E-mail: DenBond7@gmail.com
 */

public class FoldersManager {
  private LinkedHashMap<String, LocalFolder> folders;

  public FoldersManager() {
    this.folders = new LinkedHashMap<>();
  }

  /**
   * Generate a new {@link FoldersManager} using information from the local database.
   *
   * @param context     Interface to global information about an application environment.
   * @param accountName The name of an account.
   * @return The new {@link FoldersManager}.
   */
  public static FoldersManager fromDatabase(Context context, String accountName) {
    FoldersManager foldersManager = new FoldersManager();

    Cursor cursor = context.getContentResolver().query(new ImapLabelsDaoSource().
        getBaseContentUri(), null, ImapLabelsDaoSource.COL_EMAIL +
        " = ?", new String[]{accountName}, null);

    if (cursor != null) {
      ImapLabelsDaoSource imapLabelsDaoSource = new ImapLabelsDaoSource();

      while (cursor.moveToNext()) {
        foldersManager.addFolder(imapLabelsDaoSource.getFolder(cursor));
      }

      cursor.close();
    }

    return foldersManager;
  }

  /**
   * Generate a new {@link LocalFolder}
   *
   * @param imapFolder  The {@link IMAPFolder} object which contains information about a
   *                    remote folder.
   * @param folderAlias The folder alias.
   * @return
   * @throws MessagingException
   */
  public static LocalFolder generateFolder(IMAPFolder imapFolder, String folderAlias) throws
      MessagingException {
    return new LocalFolder(imapFolder.getFullName(), folderAlias, 0, imapFolder.getAttributes(),
        isCustom(imapFolder));
  }

  /**
   * Check if current folder is a custom label.
   *
   * @param folder The {@link IMAPFolder} object which contains information about a
   *               remote folder.
   * @return true if this label is a custom, false otherwise.
   * @throws MessagingException
   */
  public static boolean isCustom(IMAPFolder folder) throws MessagingException {
    String[] attr = folder.getAttributes();
    FolderType[] folderTypes = FolderType.values();

    for (String attribute : attr) {
      for (FolderType folderType : folderTypes) {
        if (folderType.getValue().equals(attribute)) {
          return false;
        }
      }
    }

    return !FolderType.INBOX.getValue().equalsIgnoreCase(folder.getFullName());
  }

  /**
   * Get a {@link FolderType} using folder attributes.
   *
   * @param localFolder Some {@link javax.mail.Folder}.
   * @return {@link FolderType}.
   */
  public static FolderType getFolderTypeForImapFolder(LocalFolder localFolder) {
    FolderType[] folderTypes = FolderType.values();

    if (localFolder != null) {
      String[] attributes = localFolder.getAttributes();

      if (attributes != null) {
        for (String attribute : attributes) {
          for (FolderType folderType : folderTypes) {
            if (folderType.getValue().equals(attribute)) {
              return folderType;
            }
          }
        }
      }

      if (!TextUtils.isEmpty(localFolder.getFullName())) {
        if (JavaEmailConstants.FOLDER_INBOX.equalsIgnoreCase(localFolder.getFullName())) {
          return FolderType.INBOX;
        }
      }

      if (!TextUtils.isEmpty(localFolder.getFullName())) {
        if (JavaEmailConstants.FOLDER_OUTBOX.equalsIgnoreCase(localFolder.getFullName())) {
          return FolderType.OUTBOX;
        }
      }
    }
    return null;
  }

  public LocalFolder getFolderInbox() {
    return folders.get(FolderType.INBOX.getValue());
  }

  public LocalFolder getFolderArchive() {
    return folders.get(FolderType.All.getValue());
  }

  public LocalFolder getFolderDrafts() {
    return folders.get(FolderType.DRAFTS.getValue());
  }

  public LocalFolder getFolderStarred() {
    return folders.get(FolderType.STARRED.getValue());
  }

  public LocalFolder getFolderSpam() {
    LocalFolder spam = folders.get(FolderType.JUNK.getValue());
    return spam != null ? spam : folders.get(FolderType.SPAM.getValue());
  }

  public LocalFolder getFolderSent() {
    return folders.get(FolderType.SENT.getValue());
  }

  public LocalFolder getFolderTrash() {
    return folders.get(FolderType.TRASH.getValue());
  }

  public LocalFolder getFolderAll() {
    return folders.get(FolderType.All.getValue());
  }

  public LocalFolder getFolderImportant() {
    return folders.get(FolderType.IMPORTANT.getValue());
  }

  /**
   * Clear the folders list.
   */
  public void clear() {
    if (this.folders != null) {
      this.folders.clear();
    }
  }

  /**
   * Add a new folder to {@link FoldersManager} to manage it.
   *
   * @param imapFolder  The {@link IMAPFolder} object which contains information about a
   *                    remote folder.
   * @param folderAlias The folder alias.
   * @throws MessagingException
   */
  public void addFolder(IMAPFolder imapFolder, String folderAlias) throws MessagingException {
    if (imapFolder != null
        && !EmailUtil.containsNoSelectAttribute(imapFolder)
        && !TextUtils.isEmpty(imapFolder.getFullName())
        && !folders.containsKey(imapFolder.getFullName())) {
      this.folders.put(prepareFolderKey(imapFolder), generateFolder(imapFolder, folderAlias));
    }
  }

  /**
   * Add a new folder to {@link FoldersManager} to manage it.
   *
   * @param localFolder The {@link LocalFolder} object which contains information about a
   *               remote folder.
   */
  public void addFolder(LocalFolder localFolder) {
    if (localFolder != null && !TextUtils.isEmpty(localFolder.getFullName())
        && !folders.containsKey(localFolder.getFullName())) {
      this.folders.put(prepareFolderKey(localFolder), localFolder);
    }
  }

  /**
   * Get {@link LocalFolder} by the alias name.
   *
   * @param folderAlias The folder alias name.
   * @return {@link LocalFolder}.
   */
  public LocalFolder getFolderByAlias(String folderAlias) {
    for (Map.Entry<String, LocalFolder> entry : folders.entrySet()) {
      if (entry.getValue() != null && entry.getValue().getFolderAlias().equals(folderAlias)) {
        return entry.getValue();
      }
    }

    return null;
  }

  public Collection<LocalFolder> getAllFolders() {
    return folders.values();
  }

  /**
   * Get a list of all available custom labels.
   *
   * @return List of custom labels({@link LocalFolder}).
   */
  public List<LocalFolder> getCustomLabels() {
    List<LocalFolder> customLocalFolders = new LinkedList<>();

    for (Map.Entry<String, LocalFolder> entry : folders.entrySet()) {
      if (entry.getValue() != null && entry.getValue().isCustom()) {
        customLocalFolders.add(entry.getValue());
      }
    }

    return customLocalFolders;
  }

  /**
   * Get a list of original server {@link LocalFolder} objects.
   *
   * @return a list of original server {@link LocalFolder} objects.
   */
  public List<LocalFolder> getServerFolders() {
    List<LocalFolder> serverLocalFolders = new LinkedList<>();

    for (Map.Entry<String, LocalFolder> entry : folders.entrySet()) {
      if (entry.getValue() != null && !entry.getValue().isCustom()) {
        serverLocalFolders.add(entry.getValue());
      }
    }

    return serverLocalFolders;
  }

  public LocalFolder findInboxFolder() {
    for (LocalFolder localFolder : getAllFolders()) {
      if (localFolder.getFullName().equalsIgnoreCase(JavaEmailConstants.FOLDER_INBOX)) {
        return localFolder;
      }
    }

    return null;
  }

  public LocalFolder getFolderOutbox() {
    return folders.get(FolderType.OUTBOX.getValue());
  }

  private String prepareFolderKey(IMAPFolder imapFolder) throws MessagingException {
    FolderType folderType = getFolderTypeForImapFolder(generateFolder(imapFolder, null));
    if (folderType == null) {
      return imapFolder.getFullName();
    } else {
      return folderType.value;
    }
  }

  private String prepareFolderKey(LocalFolder localFolder) {
    FolderType folderType = getFolderTypeForImapFolder(localFolder);
    if (folderType == null) {
      return localFolder.getFullName();
    } else {
      return folderType.value;
    }
  }

  /**
   * This class contains information about all servers folders types.
   */
  public enum FolderType {
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
    OUTBOX("\\Outbox");


    private String value;

    FolderType(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }
}
