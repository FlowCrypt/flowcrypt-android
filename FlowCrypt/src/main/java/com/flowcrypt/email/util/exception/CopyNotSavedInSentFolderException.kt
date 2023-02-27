/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

/**
 * This exception means we didn't save a copy of an already sent message
 *
 * @author Denys Bondarenko
 */
class CopyNotSavedInSentFolderException(msg: String) : FlowCryptException(msg)
