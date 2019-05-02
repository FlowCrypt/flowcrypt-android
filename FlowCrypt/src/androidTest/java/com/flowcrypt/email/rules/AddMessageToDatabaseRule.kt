/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules;

import com.flowcrypt.email.api.email.LocalFolder;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.google.android.gms.auth.GoogleAuthException;
import com.sun.mail.imap.IMAPFolder;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;

import javax.mail.FetchProfile;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;

import androidx.test.platform.app.InstrumentationRegistry;

/**
 * @author Denis Bondarenko
 * Date: 17.08.2018
 * Time: 09:48
 * E-mail: DenBond7@gmail.com
 */
public class AddMessageToDatabaseRule implements TestRule {
  private AccountDao account;
  private LocalFolder localFolder;
  private long uid;
  private Message message;

  public AddMessageToDatabaseRule(AccountDao account, LocalFolder localFolder, long uid, Message message) {
    this.account = account;
    this.localFolder = localFolder;
    this.uid = uid;
    this.message = message;
  }

  public AddMessageToDatabaseRule(AccountDao account, LocalFolder localFolder) {
    this.account = account;
    this.localFolder = localFolder;

    try {
      Session session = OpenStoreHelper.getAccountSess(InstrumentationRegistry.getInstrumentation()
              .getTargetContext(),
          account);
      Store store = OpenStoreHelper.openStore(InstrumentationRegistry.getInstrumentation()
              .getTargetContext(), account,
          session);

      IMAPFolder imapFolder = (IMAPFolder) store.getFolder(localFolder.getFullName());
      imapFolder.open(javax.mail.Folder.READ_ONLY);

      Message[] messages;

      messages = new Message[]{imapFolder.getMessage(imapFolder.getMessageCount())};

      FetchProfile fetchProfile = new FetchProfile();
      fetchProfile.add(FetchProfile.Item.ENVELOPE);
      fetchProfile.add(FetchProfile.Item.FLAGS);
      fetchProfile.add(FetchProfile.Item.CONTENT_INFO);
      fetchProfile.add(UIDFolder.FetchProfileItem.UID);

      this.message = messages[0];

      imapFolder.fetch(messages, fetchProfile);
    } catch (MessagingException | IOException | GoogleAuthException e) {
      e.printStackTrace();
    }
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        saveMsgToDatabase();
        base.evaluate();
      }
    };
  }

  private void saveMsgToDatabase() throws MessagingException {
    MessageDaoSource messageDaoSource = new MessageDaoSource();
    messageDaoSource.addRow(InstrumentationRegistry.getInstrumentation().getTargetContext(),
        account.getEmail(),
        localFolder.getFolderAlias(),
        uid,
        message, false);
  }
}
