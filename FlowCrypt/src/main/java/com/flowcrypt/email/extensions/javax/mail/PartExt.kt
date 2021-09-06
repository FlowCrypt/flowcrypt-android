/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *    Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.javax.mail

import com.flowcrypt.email.extensions.java.lang.lowercase
import javax.mail.Part

fun Part.isInline(): Boolean {
  return (this.disposition?.lowercase() ?: "") == Part.INLINE
}
