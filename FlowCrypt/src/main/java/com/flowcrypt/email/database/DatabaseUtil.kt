/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
public class DatabaseUtil {
  /**
   * Clean the cache information about some folder.
   *
   * @param context Interface to global information about an application environment.
   * @param email   An owner of the folder;
   * @param folder  A folder in a local database.
   */
  public static void cleanFolderCache(Context context, String email, String folder) {
    if (!JavaEmailConstants.FOLDER_OUTBOX.equalsIgnoreCase(folder)) {
      new MessageDaoSource().deleteCachedMsgs(context, email, folder);
      new AttachmentDaoSource().deleteCachedAttInfo(context, email, folder);
    }
  }
}
