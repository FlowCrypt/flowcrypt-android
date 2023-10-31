/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import com.flowcrypt.email.security.model.PgpKeyRingDetails

/**
 * This exception means that the key already added.
 *
 * @author Denys Bondarenko
 */
class KeyAlreadyAddedException(val keyDetails: PgpKeyRingDetails, errorMsg: String) :
  Exception(errorMsg)
