/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.MimeAddress;
import com.flowcrypt.email.js.ProcessedMime;

import java.util.ArrayList;
import java.util.Date;

/**
 * This class helps to work with messages.
 *
 * @author Denis Bondarenko
 *         Date: 15.05.2018
 *         Time: 12:43
 *         E-mail: DenBond7@gmail.com
 */
public class MessageUtil {
    public static IncomingMessageInfo getIncomingMessageInfoWithOutBody(Js js, String rawMessage) {
        IncomingMessageInfo incomingMessageInfo = new IncomingMessageInfo();
        ProcessedMime processedMime = js.mime_process(rawMessage);
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

        incomingMessageInfo.setFrom(addressesFrom);
        incomingMessageInfo.setTo(addressesTo);
        incomingMessageInfo.setCc(addressesCc);
        incomingMessageInfo.setSubject(processedMime.getStringHeader("subject"));
        incomingMessageInfo.setOriginalRawMessageWithoutAttachments(rawMessage);
        incomingMessageInfo.setFolder(new Folder("INBOX", "INBOX", 0, new String[]{"\\HasNoChildren"}, false));

        long timestamp = processedMime.getTimeHeader("date");
        if (timestamp != -1) {
            incomingMessageInfo.setReceiveDate(new Date(timestamp));
        }

        return incomingMessageInfo;
    }
}
