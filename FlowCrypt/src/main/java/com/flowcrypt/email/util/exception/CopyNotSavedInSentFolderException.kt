/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

/**
 * This exception means we didn't save a copy of an already sent message
 *
 * @author Denis Bondarenko
 *         Date: 6/19/20
 *         Time: 11:11 AM
 *         E-mail: DenBond7@gmail.com
 */
class CopyNotSavedInSentFolderException(msg: String) : FlowCryptException(msg)
