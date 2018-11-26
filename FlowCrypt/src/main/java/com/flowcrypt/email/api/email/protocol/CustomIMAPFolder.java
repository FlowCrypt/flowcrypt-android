/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol;

import com.flowcrypt.email.api.email.EmailUtil;
import com.sun.mail.iap.CommandFailedException;
import com.sun.mail.iap.ConnectionException;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.Utility;
import com.sun.mail.imap.protocol.FetchItem;
import com.sun.mail.imap.protocol.FetchResponse;
import com.sun.mail.imap.protocol.Item;
import com.sun.mail.imap.protocol.ListInfo;
import com.sun.mail.imap.protocol.MessageSet;

import java.util.ArrayList;
import java.util.List;

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * A custom implementation of the {@link IMAPFolder}
 *
 * @author Denis Bondarenko
 * Date: 30.05.2018
 * Time: 15:05
 * E-mail: DenBond7@gmail.com
 */
public class CustomIMAPFolder extends IMAPFolder implements FlowCryptIMAPFolder {

  protected CustomIMAPFolder(String fullName, char separator, IMAPStore store, Boolean isNamespace) {
    super(fullName, separator, store, isNamespace);
  }

  protected CustomIMAPFolder(ListInfo li, IMAPStore store) {
    super(li, store);
  }

  @Override
  protected IMAPMessage newIMAPMessage(int msgnum) {
    return new CustomIMAPMessage(this, msgnum);
  }

  @Override
  public void fetchGeneralInfo(Message[] messages, FetchProfile fetchProfile) throws MessagingException {
    // cache this information in case connection is closed and
    // protocol is set to null
    boolean isRev1;
    FetchItem[] fitems;

    synchronized (messageCacheLock) {
      checkOpened();
      isRev1 = protocol.isREV1();
      fitems = protocol.getFetchItems();
    }

    StringBuilder command = EmailUtil.prepareFetchCommand(fetchProfile, isRev1, getEnvelopeCommand());
    boolean allHeaders = fetchProfile.contains(IMAPFolder.FetchProfileItem.HEADERS)
        || fetchProfile.contains(IMAPFolder.FetchProfileItem.MESSAGE);

    Utility.Condition condition = new IMAPMessage.FetchProfileCondition(fetchProfile, fitems);

    // Acquire the Folder's MessageCacheLock.
    synchronized (messageCacheLock) {

      // check again to make sure folder is still open
      checkOpened();

      // Apply the test, and get the sequence-number set for
      // the messages that need to be prefetched.
      MessageSet[] msgsets = Utility.toMessageSetSorted(messages, condition);

      if (msgsets == null) {// We already have what we need.
        return;
      }

      Response[] responseArray = null;
      // to collect non-FETCH responses & unsolicited FETCH FLAG responses
      List<Response> responseArrayList = new ArrayList<>();
      try {
        responseArray = getProtocol().fetch(msgsets, command.toString());
      } catch (ConnectionException cex) {
        throw new FolderClosedException(this, cex.getMessage());
      } catch (CommandFailedException cfx) {
        // Ignore these, as per RFC 2180
      } catch (ProtocolException pex) {
        throw new MessagingException(pex.getMessage(), pex);
      }

      if (responseArray == null) {
        return;
      }

      for (Response response : responseArray) {
        if (response == null) {
          continue;
        }
        if (!(response instanceof FetchResponse)) {
          responseArrayList.add(response); // Unsolicited Non-FETCH response
          continue;
        }

        // Got a FetchResponse.
        FetchResponse fetchResponse = (FetchResponse) response;
        // Get the corresponding message.
        CustomIMAPMessage msg = (CustomIMAPMessage) getMessageBySeqNumber(fetchResponse.getNumber());

        int count = fetchResponse.getItemCount();
        boolean unsolicitedFlags = false;

        for (int j = 0; j < count; j++) {
          Item item = fetchResponse.getItem(j);
          // Check for the FLAGS item
          if (item instanceof Flags &&
              (!fetchProfile.contains(FetchProfile.Item.FLAGS) ||
                  msg == null)) {
            // Ok, Unsolicited FLAGS update.
            unsolicitedFlags = true;
          } else if (msg != null) {
            msg.handleFetchItemWithCustomBody(item, null, allHeaders);
          }
        }

        if (msg != null) {
          msg.handleExtensionFetchItems(fetchResponse.getExtensionItems());
        }

        // If this response contains any unsolicited FLAGS
        // add it to the unsolicited response vector
        if (unsolicitedFlags) {
          responseArrayList.add(fetchResponse);
        }
      }

      // Dispatch any unsolicited responses
      if (!responseArrayList.isEmpty()) {
        Response[] responses = new Response[responseArrayList.size()];
        responseArrayList.toArray(responses);
        for (Response aR : responseArray) {
          if (aR != null) {
            handleResponse(aR);
          }
        }
      }

    } // Release messageCacheLock
  }
}
