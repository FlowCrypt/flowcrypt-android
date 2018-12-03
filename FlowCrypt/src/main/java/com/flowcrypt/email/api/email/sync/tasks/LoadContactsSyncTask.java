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
  public void runIMAPAction(AccountDao account, Session session, Store store, SyncListener listener) throws Exception {
    if (listener != null) {
      FoldersManager foldersManager = FoldersManager.fromDatabase(listener.getContext(), account.getEmail());

      if (foldersManager.getFolderSent() != null) {
        IMAPFolder imapFolder = (IMAPFolder) store.getFolder(foldersManager.getFolderSent().getFullName());
        imapFolder.open(Folder.READ_ONLY);

        Message[] msgs = imapFolder.getMessages();

        if (msgs.length > 0) {
          FetchProfile fetchProfile = new FetchProfile();
          fetchProfile.add(Message.RecipientType.TO.toString().toUpperCase());
          fetchProfile.add(Message.RecipientType.CC.toString().toUpperCase());
          fetchProfile.add(Message.RecipientType.BCC.toString().toUpperCase());
          imapFolder.fetch(msgs, fetchProfile);

          ArrayList<EmailAndNamePair> emailAndNamePairs = new ArrayList<>();
          for (Message msg : msgs) {
            emailAndNamePairs.addAll(Arrays.asList(parseRecipients(msg, Message.RecipientType.TO)));
            emailAndNamePairs.addAll(Arrays.asList(parseRecipients(msg, Message.RecipientType.CC)));
            emailAndNamePairs.addAll(Arrays.asList(parseRecipients(msg, Message.RecipientType.BCC)));
          }

          ContactsDaoSource contactsDaoSource = new ContactsDaoSource();
          List<PgpContact> availablePgpContacts = contactsDaoSource.getAllPgpContacts(listener.getContext());

          Set<String> contactsInDatabase = new HashSet<>();
          Set<String> contactsWhichWillBeUpdated = new HashSet<>();
          Set<String> contactsWhichWillBeCreated = new HashSet<>();
          Map<String, String> emailNamePairsMap = new HashMap<>();

          ArrayList<EmailAndNamePair> newCandidates = new ArrayList<>();
          ArrayList<EmailAndNamePair> updateCandidates = new ArrayList<>();

          for (PgpContact pgpContact : availablePgpContacts) {
            contactsInDatabase.add(pgpContact.getEmail().toLowerCase());
            emailNamePairsMap.put(pgpContact.getEmail().toLowerCase(), pgpContact.getName());
          }

          for (EmailAndNamePair emailAndNamePair : emailAndNamePairs) {
            if (contactsInDatabase.contains(emailAndNamePair.getEmail())) {
              if (TextUtils.isEmpty(emailNamePairsMap.get(emailAndNamePair.getEmail()))) {
                if (!contactsWhichWillBeUpdated.contains(emailAndNamePair.getEmail())) {
                  contactsWhichWillBeUpdated.add(emailAndNamePair.getEmail());
                  updateCandidates.add(emailAndNamePair);
                }
              }
            } else {
              if (!contactsWhichWillBeCreated.contains(emailAndNamePair.getEmail())) {
                contactsWhichWillBeCreated.add(emailAndNamePair.getEmail());
                newCandidates.add(emailAndNamePair);
              }
            }
          }

          contactsDaoSource.updatePgpContacts(listener.getContext(), updateCandidates);
          contactsDaoSource.addRows(listener.getContext(), newCandidates);

          ContentValues contentValues = new ContentValues();
          contentValues.put(AccountDaoSource.COL_IS_CONTACTS_LOADED, true);

          new AccountDaoSource().updateAccountInformation(listener.getContext(), account.getAccount(), contentValues);
        }

        imapFolder.close(false);
      }
    }
  }

  /**
   * Generate an array of {@link EmailAndNamePair} objects from the input message.
   * This information will be retrieved from "to" , "cc" or "bcc" headers.
   *
   * @param msg           The input {@link Message}.
   * @param recipientType The input {@link Message.RecipientType}.
   * @return An array of EmailAndNamePair objects, which contains information about emails and names.
   */
  private EmailAndNamePair[] parseRecipients(Message msg, Message.RecipientType recipientType) {
    if (msg != null && recipientType != null) {
      try {
        String[] header = msg.getHeader(recipientType.toString());
        if (header != null && header.length > 0) {
          if (!TextUtils.isEmpty(header[0])) {
            InternetAddress[] addresses = InternetAddress.parse(header[0]);
            EmailAndNamePair[] emailAndNamePairs = new EmailAndNamePair[addresses.length];
            for (int i = 0; i < addresses.length; i++) {
              InternetAddress address = addresses[i];
              emailAndNamePairs[i] = new EmailAndNamePair(address.getAddress().toLowerCase(), address.getPersonal());
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
