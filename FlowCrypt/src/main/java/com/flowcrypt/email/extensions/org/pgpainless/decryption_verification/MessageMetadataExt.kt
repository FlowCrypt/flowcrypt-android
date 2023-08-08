/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.org.pgpainless.decryption_verification

import org.pgpainless.decryption_verification.MessageMetadata

/**
 * @author Denys Bondarenko
 */
val MessageMetadata.isSigned: Boolean
  get() {
    return verifiedSignatures.isNotEmpty()
        || rejectedDetachedSignatures.isNotEmpty()
        || rejectedInlineSignatures.isNotEmpty()
  }
