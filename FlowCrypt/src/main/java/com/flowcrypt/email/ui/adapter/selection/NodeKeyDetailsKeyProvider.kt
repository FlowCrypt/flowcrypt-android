/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter.selection

import androidx.recyclerview.selection.ItemKeyProvider
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails

/**
 * @author Denis Bondarenko
 *         Date: 11/2/20
 *         Time: 4:56 PM
 *         E-mail: DenBond7@gmail.com
 */
class NodeKeyDetailsKeyProvider(private val items: List<NodeKeyDetails>) : ItemKeyProvider<NodeKeyDetails>(SCOPE_CACHED) {
  override fun getKey(position: Int) = items.getOrNull(position)
  override fun getPosition(key: NodeKeyDetails) = items.indexOf(key)
}