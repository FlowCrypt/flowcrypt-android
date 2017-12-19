/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.js.PgpKey;
import com.sun.mail.imap.IMAPFolder;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.mail.MessagingException;

/**
 * @author Denis Bondarenko
 *         Date: 29.09.2017
 *         Time: 15:31
 *         E-mail: DenBond7@gmail.com
 */

public class EmailUtil {
    /**
     * Generate an unique content id.
     *
     * @return A generated unique content id.
     */
    public static String generateContentId() {
        return "<" + UUID.randomUUID().toString() + "@flowcrypt" + ">";
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
    public static boolean isFolderHasNoSelectAttribute(IMAPFolder imapFolder) throws MessagingException {
        List<String> attributes = Arrays.asList(imapFolder.getAttributes());
        return attributes.contains(JavaEmailConstants.FOLDER_ATTRIBUTE_NO_SELECT);
    }

    /**
     * Get a domain of some email.
     *
     * @return The domain of some email.
     */
    public static String getDomain(String email) {
        if (TextUtils.isEmpty(email)) {
            return "";
        } else if (email.contains("@")) {
            return email.substring(email.indexOf('@') + 1, email.length());
        } else {
            return "";
        }
    }

    /**
     * Generate {@link AttachmentInfo} from the requested information from the file uri.
     *
     * @param uri The file {@link Uri}
     * @return Generated {@link AttachmentInfo}.
     */
    public static AttachmentInfo getAttachmentInfoFromUri(Context context, Uri uri) {
        if (context != null && uri != null) {
            AttachmentInfo attachmentInfo = new AttachmentInfo();
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    attachmentInfo.setName(cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)));
                    attachmentInfo.setEncodedSize(cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE)));
                    attachmentInfo.setType(context.getContentResolver().getType(uri));
                    attachmentInfo.setUri(uri);
                }

                cursor.close();
            }

            return attachmentInfo;
        } else return null;
    }

    /**
     * Generate {@link AttachmentInfo} using the sender public key.
     *
     * @param publicKey The sender public key
     * @return A generated {@link AttachmentInfo}.
     */
    @Nullable
    public static AttachmentInfo generateAttachmentInfoFromPublicKey(PgpKey publicKey) {
        if (publicKey != null) {
            String fileName = "0x" + publicKey.getLongid().toUpperCase() + ".asc";
            String publicKeyValue = publicKey.armor();

            if (!TextUtils.isEmpty(publicKeyValue)) {
                AttachmentInfo attachmentInfo = new AttachmentInfo();

                attachmentInfo.setName(fileName);
                attachmentInfo.setEncodedSize(publicKeyValue.length());
                attachmentInfo.setRawData(publicKeyValue);
                attachmentInfo.setType(Constants.MIME_TYPE_PGP_KEY);
                attachmentInfo.setEmail(publicKey.getPrimaryUserId().getEmail());

                return attachmentInfo;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
