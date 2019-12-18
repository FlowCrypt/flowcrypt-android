/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database

import android.content.Context

import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource

/**
 * This class describes methods which help to work with the local database.
 *
 * @author Denis Bondarenko
 * Date: 28.04.2018
 * Time: 14:46
 * E-mail: DenBond7@gmail.com
 */
class DatabaseUtil {
  companion object {
    /**
     * Clean the cache information about some folder.
     *
     * @param context Interface to global information about an application environment.
     * @param email   An owner of the folder;
     * @param folder  A folder in a local database.
     */
    @JvmStatic
    fun cleanFolderCache(context: Context?, email: String?, folder: String?) {
      if (!JavaEmailConstants.FOLDER_OUTBOX.equals(folder, ignoreCase = true)) {
        context?.let {
          MessageDaoSource().deleteCachedMsgs(it, email, folder)
          AttachmentDaoSource().deleteCachedAttInfo(it, email, folder)
        }
      }
    }
  }
}
