/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import com.flowcrypt.email.api.retrofit.response.model.node.Error

/**
 * This exception can occur during the encryption process.
 *
 * @author Denis Bondarenko
 * Date: 1/25/19
 * Time: 6:34 PM
 * E-mail: DenBond7@gmail.com
 */
class NodeEncryptException(error: Error) : NodeException(error)
