/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.PgpKeyItemBinding
import com.flowcrypt.email.extensions.kotlin.asInternetAddress
import com.flowcrypt.email.extensions.org.pgpainless.key.info.getColorStateListDependsOnStatus
import com.flowcrypt.email.extensions.org.pgpainless.key.info.getStatusIcon
import com.flowcrypt.email.extensions.org.pgpainless.key.info.getStatusText
import com.flowcrypt.email.extensions.org.pgpainless.key.info.usableForEncryption
import com.flowcrypt.email.extensions.org.pgpainless.key.info.usableForSigning
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.ui.adapter.selection.KeyRingInfoItemDetails
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.GeneralUtil
import org.pgpainless.key.info.KeyRingInfo

/**
 * This adapter will be used to show a list of private keys.
 *
 * @author Denys Bondarenko
 */
class PrivateKeysListAdapter(
  private val onKeySelectedListener: OnKeySelectedListener?,
  private val checkSelection: (KeyRingInfo) -> Boolean = { false }
) : ListAdapter<KeyRingInfo, PrivateKeysListAdapter.ViewHolder>(DIFF_CALLBACK) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(
      LayoutInflater.from(parent.context).inflate(R.layout.pgp_key_item, parent, false)
    )
  }

  override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
    val pgpKeyRingDetails = getItem(position)
    pgpKeyRingDetails?.let {
      viewHolder.bind(it, onKeySelectedListener)
      viewHolder.setActivated(checkSelection.invoke(it))
    }
  }

  interface OnKeySelectedListener {
    fun onKeySelected(position: Int, pgpKeyRingDetails: KeyRingInfo?)
  }

  inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val binding = PgpKeyItemBinding.bind(itemView)
    private val dateFormat = DateTimeUtil.getPgpDateFormat(itemView.context)
    fun getItemDetails(): ItemDetailsLookup.ItemDetails<KeyRingInfo>? {
      return currentList.getOrNull(bindingAdapterPosition)?.let {
        KeyRingInfoItemDetails(bindingAdapterPosition, it)
      }
    }

    fun setActivated(isActivated: Boolean) {
      itemView.isActivated = isActivated
    }

    fun bind(keyRingInfo: KeyRingInfo, listener: OnKeySelectedListener?) {
      val context = itemView.context

      val primaryUserMimeAddress = keyRingInfo.primaryUserId.asInternetAddress()
      binding.tVPrimaryUserOrEmail.text =
        primaryUserMimeAddress?.personal ?: keyRingInfo.primaryUserId

      primaryUserMimeAddress?.address?.let {
        binding.tVPrimaryUserEmail.text = it
        binding.tVPrimaryUserEmail.visibleOrGone(
          binding.tVPrimaryUserEmail.text != binding.tVPrimaryUserOrEmail.text
        )
      }

      binding.imageViewManyUserIds.visibleOrGone(keyRingInfo.userIds.size > 1)
      binding.tVFingerprint.text = GeneralUtil.doSectionsInText(
        originalString = keyRingInfo.fingerprint.toString(), groupSize = 4
      )

      binding.tVCreationDate.text = dateFormat.format(keyRingInfo.creationDate)

      binding.textViewExpiration.text = keyRingInfo.primaryKeyExpirationDate?.let {
        context.getString(R.string.key_expiration, dateFormat.format(it))
      } ?: context.getString(
        R.string.key_expiration,
        context.getString(R.string.key_does_not_expire)
      )

      binding.textViewStatus.apply {
        backgroundTintList = keyRingInfo.getColorStateListDependsOnStatus(context)
        setCompoundDrawablesWithIntrinsicBounds(keyRingInfo.getStatusIcon(), 0, 0, 0)
        text = keyRingInfo.getStatusText(context)
      }

      //we don't care about this one here
      binding.imageViewAuthFlag.visibleOrGone(false)

      binding.imageViewEncryptionFlag.visibleOrGone(keyRingInfo.usableForEncryption)
      binding.imageViewSignFlag.visibleOrGone(keyRingInfo.usableForSigning)

      itemView.setOnClickListener { listener?.onKeySelected(bindingAdapterPosition, keyRingInfo) }
    }
  }

  companion object {
    private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<KeyRingInfo>() {
      override fun areItemsTheSame(oldItem: KeyRingInfo, newItem: KeyRingInfo) = oldItem === newItem

      override fun areContentsTheSame(oldItem: KeyRingInfo, newItem: KeyRingInfo) =
        oldItem.fingerprint.toString() == newItem.fingerprint.toString()
            && oldItem.lastModified == newItem.lastModified
    }
  }
}
