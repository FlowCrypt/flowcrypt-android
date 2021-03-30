/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.javax.mail.internet

import javax.mail.internet.InternetAddress

/**
 * @author Denis Bondarenko
 *         Date: 3/30/21
 *         Time: 10:41 AM
 *         E-mail: DenBond7@gmail.com
 */
val InternetAddress.domain: String
  get() = address.substring(address.indexOf('@') + 1)