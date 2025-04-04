/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.util.exception

/**
 * @author Denys Bondarenko
 */
class MessageNotFoundException(errorMsg: String) : IllegalStateException(errorMsg)