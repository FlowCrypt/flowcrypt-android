/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails

/**
 * @author Denis Bondarenko
 *         Date: 3/11/20
 *         Time: 4:03 PM
 *         E-mail: DenBond7@gmail.com
 */
class SavePrivateKeyToDatabaseException(val keys: List<NodeKeyDetails>, e: Exception) : Exception(e)