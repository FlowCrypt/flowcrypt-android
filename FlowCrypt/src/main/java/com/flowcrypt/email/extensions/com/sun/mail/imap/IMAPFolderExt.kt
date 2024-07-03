/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.com.sun.mail.imap

import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import org.eclipse.angus.mail.imap.IMAPFolder

/**
 * @author Denys Bondarenko
 */
fun IMAPFolder.canBeUsedToSearchBackups(): Boolean {
  return attributes.none {
    it in listOf(
      JavaEmailConstants.FOLDER_ATTRIBUTE_NO_SELECT,
      FoldersManager.FolderType.TRASH.value,
      FoldersManager.FolderType.SENT.value,
    )
  }
}