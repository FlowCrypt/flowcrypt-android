/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node

/**
 * @author Denis Bondarenko
 *         Date: 6/7/21
 *         Time: 6:19 PM
 *         E-mail: DenBond7@gmail.com
 */
class KeyCacheWipeRequest : BaseNodeRequest() {
  override val endpoint: String = "keyCacheWipe"
}
