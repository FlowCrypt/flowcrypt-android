/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *    Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.javax.mail

import java.util.Locale
import javax.mail.Part

fun Part.isInline(): Boolean {
  return (this.disposition?.toLowerCase(Locale.getDefault()) ?: "") == Part.INLINE
}
