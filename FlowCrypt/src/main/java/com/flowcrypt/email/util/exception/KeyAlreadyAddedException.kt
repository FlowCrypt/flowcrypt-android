/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import com.flowcrypt.email.security.model.NodeKeyDetails

/**
 * This exception means that the key already added.
 *
 * @author Denis Bondarenko
 * Date: 27.02.2018
 * Time: 14:09
 * E-mail: DenBond7@gmail.com
 */
class KeyAlreadyAddedException(val keyDetails: NodeKeyDetails, errorMsg: String) : Exception(errorMsg)
