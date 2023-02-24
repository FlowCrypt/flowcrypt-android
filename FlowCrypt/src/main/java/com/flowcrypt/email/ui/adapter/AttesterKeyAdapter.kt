/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.AttesterKeyItemBinding
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.util.UIUtil


/**
 * This adapter can be used to show info about public keys from the https://flowcrypt.com/attester/lookup/email/.
 *
 * @author Denys Bondarenko
 */
class AttesterKeyAdapter :
  ListAdapter<Pair<String, PgpKeyDetails>, AttesterKeyAdapter.ViewHolder>(DIFF_CALLBACK) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(
      LayoutInflater.from(parent.context).inflate(R.layout.attester_key_item, parent, false)
    )
  }

  override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
    viewHolder.bind(getItem(position))
  }

  /**
   * The view holder implementation for a better performance.
   */
  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = AttesterKeyItemBinding.bind(itemView)
    fun bind(pair: Pair<String, PgpKeyDetails>) {
      val context = itemView.context
      binding.textViewKeyOwner.text = pair.first

      when {
        pair.second.publicKey.isEmpty() -> {
          binding.textViewKeyAttesterStatus.setText(R.string.no_public_key_recorded)
          binding.textViewKeyAttesterStatus.setTextColor(
            UIUtil.getColor(
              context,
              R.color.orange
            )
          )
        }

        isPublicKeyMatching(context, pair.second) -> {
          binding.textViewKeyAttesterStatus.setText(R.string.submitted_can_receive_encrypted_email)
          binding.textViewKeyAttesterStatus.setTextColor(
            UIUtil.getColor(
              context,
              R.color.colorPrimary
            )
          )
        }

        else -> {
          binding.textViewKeyAttesterStatus.setText(R.string.wrong_public_key_recorded)
          binding.textViewKeyAttesterStatus.setTextColor(UIUtil.getColor(context, R.color.red))
        }
      }
    }

    /**
     * Check is public key found, and the fingerprint does not match any fingerprints of saved keys.
     *
     * @param context Interface to global information about an application environment.
     * @param pgpKeyDetails The [PgpKeyDetails] object which contains info about a public key.
     * @return true if public key found, and the fingerprint does not match any
     * fingerprints of saved keys, otherwise false.
     */
    private fun isPublicKeyMatching(context: Context, pgpKeyDetails: PgpKeyDetails): Boolean {
      return pgpKeyDetails.fingerprint.let {
        KeysStorageImpl.getInstance(context).getPGPSecretKeyRingByFingerprint(it) != null
      }
    }
  }

  companion object {
    private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Pair<String, PgpKeyDetails>>() {
      override fun areItemsTheSame(
        oldItem: Pair<String, PgpKeyDetails>,
        newItem: Pair<String, PgpKeyDetails>
      ): Boolean {
        return oldItem.first == newItem.first
            && oldItem.second.fingerprint == newItem.second.fingerprint
      }

      override fun areContentsTheSame(
        oldItem: Pair<String, PgpKeyDetails>,
        newItem: Pair<String, PgpKeyDetails>
      ): Boolean {
        return oldItem.first == newItem.first && oldItem.second == newItem.second
      }
    }
  }
}
