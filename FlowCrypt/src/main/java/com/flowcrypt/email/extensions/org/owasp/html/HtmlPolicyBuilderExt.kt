/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *    Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.org.owasp.html

import org.owasp.html.HtmlPolicyBuilder

// NOTE: This can't be just allowAttributesOnElements() because this would interfere
// with the same named private method in the HtmlPolicyBuilder
fun HtmlPolicyBuilder.allowAttributesOnElementsExt(
  elementsToAttributesMap: Map<String, Array<String>>
): HtmlPolicyBuilder {
  var builder = this
  for (elementAttrs in elementsToAttributesMap) {
    builder = builder.allowAttributes(*elementAttrs.value).onElements(elementAttrs.key)
  }
  return builder
}
