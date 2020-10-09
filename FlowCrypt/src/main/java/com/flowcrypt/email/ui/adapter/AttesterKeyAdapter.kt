/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.util.UIUtil


/**
 * This adapter can be used to show info about public keys from the https://flowcrypt.com/attester/lookup/email/.
 *
 * @author Denis Bondarenko
 * Date: 14.11.2017
 * Time: 9:42
 * E-mail: DenBond7@gmail.com
 */
class AttesterKeyAdapter(
    private val responses: MutableList<NodeKeyDetails> = mutableListOf()) : RecyclerView.Adapter<AttesterKeyAdapter.ViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.attester_key_item, parent, false))
  }

  override fun getItemCount(): Int {
    return responses.size
  }

  override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
    val context = viewHolder.itemView.context
    val nodeKeyDetails = responses[position]
    updateView(nodeKeyDetails, context, viewHolder)
  }

  fun setData(newList: List<NodeKeyDetails>) {
    val productDiffUtilCallback = ResponseDiffUtilCallback(responses, newList)
    val productDiffResult = DiffUtil.calculateDiff(productDiffUtilCallback)

    responses.clear()
    responses.addAll(newList)
    productDiffResult.dispatchUpdatesTo(this)
  }

  private fun updateView(nodeKeyDetails: NodeKeyDetails, context: Context, viewHolder: ViewHolder) {
    viewHolder.textViewKeyOwner.text = nodeKeyDetails.primaryPgpContact.email

    when {
      nodeKeyDetails.publicKey.isNullOrEmpty() -> {
        viewHolder.textViewKeyAttesterStatus.setText(R.string.no_public_key_recorded)
        viewHolder.textViewKeyAttesterStatus.setTextColor(UIUtil.getColor(context, R.color.orange))
      }

      isPublicKeyMatched(viewHolder.itemView.context, nodeKeyDetails) -> {
        viewHolder.textViewKeyAttesterStatus.setText(R.string.submitted_can_receive_encrypted_email)
        viewHolder.textViewKeyAttesterStatus.setTextColor(UIUtil.getColor(context, R.color.colorPrimary))
      }

      else -> {
        viewHolder.textViewKeyAttesterStatus.setText(R.string.wrong_public_key_recorded)
        viewHolder.textViewKeyAttesterStatus.setTextColor(UIUtil.getColor(context, R.color.red))
      }
    }
  }

  /**
   * Check is public key found, and the longid does not match any longids of saved keys.
   *
   * @param context Interface to global information about an application environment.
   * @param nodeKeyDetails The [NodeKeyDetails] object which contains info about a public key.
   * @return true if public key found, and the longid does not match any longids of saved keys, otherwise false.
   */
  private fun isPublicKeyMatched(context: Context, nodeKeyDetails: NodeKeyDetails): Boolean {
    return KeysStorageImpl.getInstance(context).getPgpPrivateKey(nodeKeyDetails.longId) != null
  }

  /**
   * The view holder implementation for a better performance.
   */
  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var textViewKeyOwner: TextView = itemView.findViewById(R.id.textViewKeyOwner)
    var textViewKeyAttesterStatus: TextView = itemView.findViewById(R.id.textViewKeyAttesterStatus)
  }

  inner class ResponseDiffUtilCallback(private val oldList: List<NodeKeyDetails>,
                                       private val newList: List<NodeKeyDetails>) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
      val oldProduct = oldList[oldItemPosition]
      val newProduct = newList[newItemPosition]
      return oldProduct.longId == newProduct.longId
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
      val oldProduct = oldList[oldItemPosition]
      val newProduct = newList[newItemPosition]
      return oldProduct == newProduct
    }
  }
}
