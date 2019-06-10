/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails

/**
 * This exception means that the key already added.
 *
 * @author Denis Bondarenko
 * Date: 27.02.2018
 * Time: 14:09
 * E-mail: DenBond7@gmail.com
 */

class KeyAlreadyAddedException(val keyDetails: NodeKeyDetails, errorMsg: String) : Exception(errorMsg)
