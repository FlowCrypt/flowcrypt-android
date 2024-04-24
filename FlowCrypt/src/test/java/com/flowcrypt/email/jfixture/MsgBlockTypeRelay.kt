/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jfixture

import com.flextrade.jfixture.NoSpecimen
import com.flextrade.jfixture.SpecimenBuilder
import com.flextrade.jfixture.SpecimenContext
import com.flowcrypt.email.api.retrofit.response.model.DecryptErrorMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.DecryptedAttMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.GenericMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import com.flowcrypt.email.api.retrofit.response.model.MsgBlockFactory
import com.flowcrypt.email.api.retrofit.response.model.PublicKeyMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.SecurityWarningMsgBlock

/**
 * @author Denys Bondarenko
 */
class MsgBlockTypeRelay : SpecimenBuilder {
  private val msgBlockClass = MsgBlock::class.java

  override fun create(request: Any, context: SpecimenContext): Any {
    return when {
      request != msgBlockClass -> {
        NoSpecimen()
      }

      else -> {
        val classForSpecimen: Class<*> =
          when (MsgBlockFactory.supportedMsgBlockTypes.shuffled().first()) {
            MsgBlock.Type.PUBLIC_KEY -> PublicKeyMsgBlock::class.java
            MsgBlock.Type.DECRYPT_ERROR -> DecryptErrorMsgBlock::class.java
            MsgBlock.Type.DECRYPTED_ATT -> DecryptedAttMsgBlock::class.java
            MsgBlock.Type.SECURITY_WARNING -> SecurityWarningMsgBlock::class.java
            else -> GenericMsgBlock::class.java
          }
        context.resolve(classForSpecimen)
      }
    }
  }
}
