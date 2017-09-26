/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource;
import com.sun.mail.imap.IMAPFolder;

import java.util.Arrays;
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
 *         Date: 07.06.2017
 *         Time: 14:37
 *         E-mail: DenBond7@gmail.com
 */

public class FoldersManager {
    private LinkedHashMap<String, Folder> folders;

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
     * Generate a new {@link Folder}
     *
     * @param imapFolder  The {@link IMAPFolder} object which contains information about a
     *                    remote folder.
     * @param folderAlias The folder alias.
     * @return
     * @throws MessagingException
     */
    public static Folder generateFolder(IMAPFolder imapFolder, String folderAlias) throws
            MessagingException {
        return new Folder(imapFolder.getFullName(),
                folderAlias, imapFolder.getMessageCount(),
                imapFolder.getAttributes(),
                isCustomLabels(imapFolder));
    }

    /**
     * Check if current folder is a custom label.
     *
     * @param folder The {@link IMAPFolder} object which contains information about a
     *               remote folder.
     * @return true if this label is a custom, false otherwise.
     * @throws MessagingException
     */
    public static boolean isCustomLabels(IMAPFolder folder) throws MessagingException {
        String[] attr = folder.getAttributes();
        FolderType[] folderTypes = FolderType.values();

        for (String attribute : attr) {
            for (FolderType folderType : folderTypes) {
                if (folderType.getValue().equals(attribute)) {
                    return false;
                }
            }
        }

        return !FolderType.INBOX.getValue().equals(folder.getFullName());

    }

    /**
     * Get a {@link FolderType} using folder attributes.
     *
     * @param attributes The folder attributes.
     * @return {@link FolderType}.
     */
    public static FolderType getFolderTypeForImapFodler(String[] attributes) {
        FolderType[] folderTypes = FolderType.values();

        if (attributes != null) {
            for (String attribute : attributes) {
                for (FolderType folderType : folderTypes) {
                    if (folderType.getValue().equals(attribute)) {
                        return folderType;
                    }
                }
            }
        }
        return null;
    }

    public Folder getFolderInbox() {
        return folders.get(FolderType.INBOX.getValue());
    }

    public Folder getFolderArchive() {
        return folders.get(FolderType.All.getValue());
    }

    public Folder getFolderDrafts() {
        return folders.get(FolderType.DRAFTS.getValue());
    }

    public Folder getFolderStarred() {
        return folders.get(FolderType.STARRED.getValue());
    }

    public Folder getFolderSpam() {
        return folders.get(FolderType.SPAM.getValue());
    }

    public Folder getFolderSent() {
        return folders.get(FolderType.SENT.getValue());
    }

    public Folder getFolderTrash() {
        return folders.get(FolderType.TRASH.getValue());
    }

    public Folder getFolderAll() {
        return folders.get(FolderType.All.getValue());
    }

    public Folder getFolderImportant() {
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
                && !isFolderHasNoSelectAttribute(imapFolder)
                && !TextUtils.isEmpty(imapFolder.getFullName())
                && !folders.containsKey(imapFolder.getFullName())) {
            this.folders.put(prepareFolderKey(imapFolder), generateFolder(imapFolder, folderAlias));
        }
    }

    /**
     * Add a new folder to {@link FoldersManager} to manage it.
     *
     * @param folder The {@link Folder} object which contains information about a
     *               remote folder.
     */
    public void addFolder(Folder folder) {
        if (folder != null && !TextUtils.isEmpty(folder.getServerFullFolderName())
                && !folders.containsKey(folder.getServerFullFolderName())) {
            this.folders.put(prepareFolderKey(folder), folder);
        }
    }

    /**
     * Get {@link Folder} by the alias name.
     *
     * @param folderAlias The folder alias name.
     * @return {@link Folder}.
     */
    public Folder getFolderByAlias(String folderAlias) {
        for (Map.Entry<String, Folder> entry : folders.entrySet()) {
            if (entry.getValue() != null && entry.getValue().getFolderAlias().equals(folderAlias)) {
                return entry.getValue();
            }
        }

        return null;
    }

    public Collection<Folder> getAllFolders() {
        return folders.values();
    }

    /**
     * Get a list of all available custom labels.
     *
     * @return List of custom labels({@link Folder}).
     */
    public List<Folder> getCustomLabels() {
        List<Folder> customFolders = new LinkedList<>();

        for (Map.Entry<String, Folder> entry : folders.entrySet()) {
            if (entry.getValue() != null && entry.getValue().isCustomLabel()) {
                customFolders.add(entry.getValue());
            }
        }

        return customFolders;
    }

    /**
     * Get a list of original server {@link Folder} objects.
     *
     * @return a list of original server {@link Folder} objects.
     */
    public List<Folder> getServerFolders() {
        List<Folder> serverFolders = new LinkedList<>();

        for (Map.Entry<String, Folder> entry : folders.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isCustomLabel()) {
                serverFolders.add(entry.getValue());
            }
        }

        return serverFolders;
    }

    public Folder findInboxFolder() {
        for (Folder folder : getAllFolders()) {
            if (folder.getServerFullFolderName().equalsIgnoreCase(JavaEmailConstants.FOLDER_INBOX)) {
                return folder;
            }
        }

        return null;
    }

    private String prepareFolderKey(IMAPFolder imapFolder) throws MessagingException {
        FolderType folderType = getFolderTypeForImapFodler(imapFolder.getAttributes());
        if (folderType == null) {
            return imapFolder.getFullName();
        } else {
            return folderType.value;
        }
    }

    private String prepareFolderKey(Folder folder) {
        FolderType folderType = getFolderTypeForImapFodler(folder.getAttributes());
        if (folderType == null) {
            return folder.getServerFullFolderName();
        } else {
            return folderType.value;
        }
    }

    /**
     * Check if current folder has {@link JavaEmailConstants#FOLDER_ATTRIBUTE_NO_SELECT}. If the
     * folder contains it attribute we will not show this folder in the list.
     *
     * @param imapFolder The {@link IMAPFolder} object.
     * @return true if current folder contains attribute
     * {@link JavaEmailConstants#FOLDER_ATTRIBUTE_NO_SELECT}, false otherwise.
     * @throws MessagingException
     */
    private boolean isFolderHasNoSelectAttribute(IMAPFolder imapFolder) throws MessagingException {
        List<String> attributes = Arrays.asList(imapFolder.getAttributes());
        return attributes.contains(JavaEmailConstants.FOLDER_ATTRIBUTE_NO_SELECT);
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
        SPAM("\\Junk"),
        SENT("\\Sent"),
        TRASH("\\Trash"),
        IMPORTANT("\\Important");


        private String value;

        FolderType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
