/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import com.flowcrypt.email.security.model.PgpKeyDetails

/**
 * @author Denys Bondarenko
 */
class SavePrivateKeyToDatabaseException(val keys: List<PgpKeyDetails>, e: Exception) : Exception(e)
