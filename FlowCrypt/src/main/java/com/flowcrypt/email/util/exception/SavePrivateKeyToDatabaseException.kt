/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import com.flowcrypt.email.security.model.PgpKeyRingDetails

/**
 * @author Denys Bondarenko
 */
class SavePrivateKeyToDatabaseException(val keys: List<PgpKeyRingDetails>, e: Exception) :
  Exception(e)
