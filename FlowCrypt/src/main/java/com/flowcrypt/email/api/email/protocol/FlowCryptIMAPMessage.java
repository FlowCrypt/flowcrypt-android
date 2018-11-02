/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol;

import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.protocol.BODY;
import com.sun.mail.imap.protocol.Item;

import javax.mail.MessagingException;

/**
 * An abstract implementation of the {@link IMAPMessage} for our purposes
 *
 * @author Denis Bondarenko
 * Date: 30.05.2018
 * Time: 10:13
 * E-mail: DenBond7@gmail.com
 */
public interface FlowCryptIMAPMessage {
  /**
   * Apply the data in the FETCH item to this message.
   * <p>
   * This method uses a custom realization only for {@link BODY}.
   *
   * @param item       the fetch item
   * @param hdrs       the headers we're asking for
   * @param allHeaders load all headers?
   * @return did we handle this fetch item?
   * @throws MessagingException for failures
   */
  boolean handleFetchItemWithCustomBody(Item item, String[] hdrs, boolean allHeaders) throws MessagingException;

  /**
   * Get the message body as String value.
   *
   * @return The string which contains body.
   */
  String getBodyAsString();
}
