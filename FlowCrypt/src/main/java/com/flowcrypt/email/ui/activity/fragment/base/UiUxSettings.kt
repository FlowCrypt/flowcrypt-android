/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base

/**
 * @author Denis Bondarenko
 *         Date: 3/8/22
 *         Time: 12:33 PM
 *         E-mail: DenBond7@gmail.com
 */
interface UiUxSettings {
  val isToolbarVisible: Boolean
    get() = true

  val isDisplayHomeAsUpEnabled: Boolean
    get() = true
}
