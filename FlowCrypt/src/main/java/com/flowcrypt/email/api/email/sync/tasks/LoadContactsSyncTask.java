/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.content.ContentValues;
import android.os.Messenger;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.EmailAndNamePair;
import com.sun.mail.imap.IMAPFolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;

/**
 * This {@link SyncTask} loads information about contacts from the SENT folder.
 *
 * @author Denis Bondarenko
 * Date: 23.04.2018
 * Time: 14:53
 * E-mail: DenBond7@gmail.com
 */
public class LoadContactsSyncTask extends BaseSyncTask {

    /**
     * The base constructor.
     *
     * @param ownerKey    The name of the reply to {@link Messenger}.
     * @param requestCode The unique request code for the reply to {@link Messenger}.
     */
    public LoadContactsSyncTask(String ownerKey, int requestCode) {
        super(ownerKey, requestCode);
    }

    public LoadContactsSyncTask() {
        super(null, 0);
    }

    @Override
    public void runIMAPAction(AccountDao accountDao, Session session, Store store, SyncListener syncListener)
            throws Exception {
        if (syncListener != null) {
            FoldersManager foldersManager
                    = FoldersManager.fromDatabase(syncListener.getContext(), accountDao.getEmail());

            if (foldersManager.getFolderSent() != null) {
                IMAPFolder imapFolder =
                        (IMAPFolder) store.getFolder(foldersManager.getFolderSent().getServerFullFolderName());
                imapFolder.open(Folder.READ_ONLY);

                Message[] messages = imapFolder.getMessages();

                if (messages.length > 0) {
                    FetchProfile fetchProfile = new FetchProfile();
                    fetchProfile.add(Message.RecipientType.TO.toString().toUpperCase());
                    fetchProfile.add(Message.RecipientType.CC.toString().toUpperCase());
                    fetchProfile.add(Message.RecipientType.BCC.toString().toUpperCase());
                    imapFolder.fetch(messages, fetchProfile);

                    ArrayList<EmailAndNamePair> emailAndNamePairs = new ArrayList<>();
                    for (Message message : messages) {
                        emailAndNamePairs.addAll(Arrays.asList(parseRecipients(message, Message.RecipientType.TO)));
                        emailAndNamePairs.addAll(Arrays.asList(parseRecipients(message, Message.RecipientType.CC)));
                        emailAndNamePairs.addAll(Arrays.asList(parseRecipients(message, Message.RecipientType.BCC)));
                    }

                    ContactsDaoSource contactsDaoSource = new ContactsDaoSource();
                    List<PgpContact> availablePgpContacts = contactsDaoSource.getAllPgpContacts(syncListener
                            .getContext());

                    Set<String> contactsInDatabaseSet = new HashSet<>();
                    Set<String> contactsWhichWillBeUpdatedSet = new HashSet<>();
                    Set<String> contactsWhichWillBeCreatedSet = new HashSet<>();
                    Map<String, String> emailNamePairsMap = new HashMap<>();

                    ArrayList<EmailAndNamePair> newCandidate = new ArrayList<>();
                    ArrayList<EmailAndNamePair> updateCandidate = new ArrayList<>();

                    for (PgpContact pgpContact : availablePgpContacts) {
                        contactsInDatabaseSet.add(pgpContact.getEmail().toLowerCase());
                        emailNamePairsMap.put(pgpContact.getEmail().toLowerCase(), pgpContact.getName());
                    }

                    for (EmailAndNamePair emailAndNamePair : emailAndNamePairs) {
                        if (contactsInDatabaseSet.contains(emailAndNamePair.getEmail())) {
                            if (TextUtils.isEmpty(emailNamePairsMap.get(emailAndNamePair.getEmail()))) {
                                if (!contactsWhichWillBeUpdatedSet.contains(emailAndNamePair.getEmail())) {
                                    contactsWhichWillBeUpdatedSet.add(emailAndNamePair.getEmail());
                                    updateCandidate.add(emailAndNamePair);
                                }
                            }
                        } else {
                            if (!contactsWhichWillBeCreatedSet.contains(emailAndNamePair.getEmail())) {
                                contactsWhichWillBeCreatedSet.add(emailAndNamePair.getEmail());
                                newCandidate.add(emailAndNamePair);
                            }
                        }
                    }

                    contactsDaoSource.updatePgpContacts(syncListener.getContext(), updateCandidate);
                    contactsDaoSource.addRows(syncListener.getContext(), newCandidate);

                    ContentValues contentValues = new ContentValues();
                    contentValues.put(AccountDaoSource.COL_IS_CONTACTS_LOADED, true);

                    new AccountDaoSource().updateAccountInformation(syncListener.getContext(),
                            accountDao.getAccount(), contentValues);
                }

                imapFolder.close(false);
            }
        }
    }

    /**
     * Remove duplicates from the input list.
     *
     * @param emailAndNamePairs The input list.
     * @return A list with unique elements.
     */
    private ArrayList<EmailAndNamePair> removeDuplicates(ArrayList<EmailAndNamePair> emailAndNamePairs) {
        Set<String> savedEmails = new HashSet<>();
        ArrayList<EmailAndNamePair> cleanedEmailAndNamePairs = new ArrayList<>();

        for (EmailAndNamePair emailAndNamePair : emailAndNamePairs) {
            if (!savedEmails.contains(emailAndNamePair.getEmail())) {
                savedEmails.add(emailAndNamePair.getEmail());
                cleanedEmailAndNamePairs.add(emailAndNamePair);
            }
        }

        return cleanedEmailAndNamePairs;
    }

    /**
     * Generate an array of {@link EmailAndNamePair} objects from the input message.
     * This information will be retrieved from "to" , "cc" or "bcc" headers.
     *
     * @param message       The input {@link Message}.
     * @param recipientType The input {@link Message.RecipientType}.
     * @return An array of EmailAndNamePair objects, which contains information about emails and names.
     */
    private EmailAndNamePair[] parseRecipients(Message message, Message.RecipientType recipientType) {
        if (message != null && recipientType != null) {
            try {
                String[] header = message.getHeader(recipientType.toString());
                if (header != null && header.length > 0) {
                    if (!TextUtils.isEmpty(header[0])) {
                        InternetAddress[] internetAddresses = InternetAddress.parse(header[0]);
                        EmailAndNamePair[] emailAndNamePairs = new EmailAndNamePair[internetAddresses.length];
                        for (int i = 0; i < internetAddresses.length; i++) {
                            InternetAddress internetAddress = internetAddresses[i];
                            emailAndNamePairs[i] = new EmailAndNamePair(internetAddress.getAddress().toLowerCase(),
                                    internetAddress.getPersonal());
                        }

                        return emailAndNamePairs;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return new EmailAndNamePair[0];
        } else {
            return new EmailAndNamePair[0];
        }
    }
}
