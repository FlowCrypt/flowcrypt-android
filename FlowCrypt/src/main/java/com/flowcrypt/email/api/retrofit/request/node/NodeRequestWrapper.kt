/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node

/**
 * @author DenBond7
 */
class NodeRequestWrapper<T : NodeRequest>(val requestCode: Int, val request: T)
