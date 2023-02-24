/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

/**
 * This exception means that the given folder is not available for any actions
 *
 * @author Denys Bondarenko
 */
class FolderNotAvailableException(message: String) : FlowCryptException(message)
