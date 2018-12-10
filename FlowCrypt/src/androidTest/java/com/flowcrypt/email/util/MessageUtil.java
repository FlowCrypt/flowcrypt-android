/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import com.flowcrypt.email.api.email.LocalFolder;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.js.MimeAddress;
import com.flowcrypt.email.js.ProcessedMime;
import com.flowcrypt.email.js.core.Js;

import java.util.ArrayList;
import java.util.Date;

/**
 * This class helps to work with messages.
 *
 * @author Denis Bondarenko
 * Date: 15.05.2018
 * Time: 12:43
 * E-mail: DenBond7@gmail.com
 */
public class MessageUtil {
  public static IncomingMessageInfo getIncomingMsgInfoWithoutBody(Js js, String rawMsg) {
    ProcessedMime processedMime = js.mime_process(rawMsg);
    ArrayList<String> addressesFrom = new ArrayList<>();
    ArrayList<String> addressesTo = new ArrayList<>();
    ArrayList<String> addressesCc = new ArrayList<>();

    for (MimeAddress mimeAddress : processedMime.getAddressHeader("from")) {
      addressesFrom.add(mimeAddress.getAddress());
    }

    for (MimeAddress mimeAddress : processedMime.getAddressHeader("to")) {
      addressesTo.add(mimeAddress.getAddress());
    }

    for (MimeAddress mimeAddress : processedMime.getAddressHeader("cc")) {
      addressesCc.add(mimeAddress.getAddress());
    }

    IncomingMessageInfo incomingMsgInfo = new IncomingMessageInfo();
    incomingMsgInfo.setFrom(addressesFrom);
    incomingMsgInfo.setTo(addressesTo);
    incomingMsgInfo.setCc(addressesCc);
    incomingMsgInfo.setSubject(processedMime.getStringHeader("subject"));
    incomingMsgInfo.setOriginalRawMsgWithoutAtts(rawMsg);
    incomingMsgInfo.setLocalFolder(new LocalFolder("INBOX", "INBOX", 0, new String[]{"\\HasNoChildren"}, false));

    long timestamp = processedMime.getTimeHeader("date");
    if (timestamp != -1) {
      incomingMsgInfo.setReceiveDate(new Date(timestamp));
    }

    return incomingMsgInfo;
  }
}
