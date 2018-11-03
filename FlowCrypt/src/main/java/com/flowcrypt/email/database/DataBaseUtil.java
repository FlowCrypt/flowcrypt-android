/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database;

import android.content.Context;

import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;

/**
 * This class describes methods which help to work with the local database.
 *
 * @author Denis Bondarenko
 * Date: 28.04.2018
 * Time: 14:46
 * E-mail: DenBond7@gmail.com
 */
public class DataBaseUtil {
  /**
   * Clean the cache information about some folder.
   *
   * @param context     Interface to global information about an application environment.
   * @param email       An owner of the folder;
   * @param folderAlias A folder alias in the local cache
   */
  public static void cleanFolderCache(Context context, String email, String folderAlias) {
    if (!JavaEmailConstants.FOLDER_OUTBOX.equalsIgnoreCase(folderAlias)) {
      new MessageDaoSource().deleteCachedMessagesOfFolder(context, email, folderAlias);
      new AttachmentDaoSource().deleteCachedAttachmentInfoOfFolder(context, email, folderAlias);
    }
  }
}
