/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jfixture

import com.flextrade.jfixture.JFixture
import com.flextrade.jfixture.customisation.Customisation

/**
 * @author Denis Bondarenko
 *         Date: 4/23/20
 *         Time: 2:31 PM
 *         E-mail: DenBond7@gmail.com
 */
class MsgBlockGenerationCustomization : Customisation {
  override fun customise(fixture: JFixture?) {
    fixture?.addBuilderToStartOfPipeline(MsgBlockTypeRelay())
  }
}