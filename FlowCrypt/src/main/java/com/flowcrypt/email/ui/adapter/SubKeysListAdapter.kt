/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.ItemSubKeyDetailsBinding
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.getLastModificationDate
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.getStatusColorStateList
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.getStatusIconResId
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.getStatusText
import com.flowcrypt.email.extensions.org.pgpainless.key.info.generateKeyCapabilitiesDrawable
import com.flowcrypt.email.extensions.org.pgpainless.key.info.getPubKeysWithoutPrimary
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.GeneralUtil
import org.bouncycastle.openpgp.PGPPublicKey
import org.pgpainless.algorithm.PublicKeyAlgorithm
import org.pgpainless.key.OpenPgpFingerprint
import org.pgpainless.key.info.KeyRingInfo
import java.util.Date

/**
 * @author Denys Bondarenko
 */
class SubKeysListAdapter(private var keyRingInfo: KeyRingInfo? = null) :
  ListAdapter<PGPPublicKey, SubKeysListAdapter.SubKeysViewHolder>(DIFF_CALLBACK) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubKeysViewHolder {
    return SubKeysViewHolder(
      LayoutInflater.from(parent.context).inflate(R.layout.item_sub_key_details, parent, false)
    )
  }

  override fun onBindViewHolder(holder: SubKeysViewHolder, position: Int) {
    val publicKey = getItem(position)
    holder.bind(keyRingInfo, publicKey)
  }

  fun submit(keyRingInfo: KeyRingInfo?) {
    this.keyRingInfo = keyRingInfo
    super.submitList(keyRingInfo?.getPubKeysWithoutPrimary()?.toList())
  }

  inner class SubKeysViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val binding = ItemSubKeyDetailsBinding.bind(itemView)
    private val dateFormat = DateTimeUtil.getPgpDateFormat(itemView.context)
    fun bind(keyRingInfo: KeyRingInfo?, publicKey: PGPPublicKey) {
      val context = itemView.context
      binding.textViewKeyFingerprint.text = GeneralUtil.doSectionsInText(
        originalString = OpenPgpFingerprint.of(publicKey).toString(), groupSize = 4
      )

      binding.textViewKeyAlgorithm.apply {
        val algorithm = PublicKeyAlgorithm.requireFromId(publicKey.algorithm)
        val bitStrength = if (publicKey.bitStrength != -1) publicKey.bitStrength else null
        val algoWithBits = algorithm.name + (bitStrength?.let { "/$it" } ?: "")
        text = algoWithBits
      }

      binding.textViewKeyCreated.text = context.getString(
        R.string.template_created,
        dateFormat.format(Date(publicKey.creationTime.time))
      )

      binding.textViewKeyModified.apply {
        text = context.getString(
          R.string.template_modified,
          dateFormat.format(Date(publicKey.getLastModificationDate().time))
        )
      }

      binding.textViewKeyExpiration.apply {
        val expirationDate = keyRingInfo?.getSubkeyExpirationDate(OpenPgpFingerprint.of(publicKey))
        text = if (expirationDate == null) {
          context?.getString(R.string.expires, context.getString(R.string.never))
        } else {
          context?.getString(R.string.expires, dateFormat.format(expirationDate))
        }
      }

      binding.textViewKeyCapabilities.setCompoundDrawablesWithIntrinsicBounds(
        null,
        null,
        keyRingInfo?.generateKeyCapabilitiesDrawable(context, publicKey.keyID),
        null
      )

      binding.textViewStatusValue.apply {
        backgroundTintList = publicKey.getStatusColorStateList(context)
        setCompoundDrawablesWithIntrinsicBounds(publicKey.getStatusIconResId(), 0, 0, 0)
        text = publicKey.getStatusText(context)
      }
    }
  }

  companion object {
    private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PGPPublicKey>() {
      override fun areItemsTheSame(
        oldItem: PGPPublicKey,
        newItem: PGPPublicKey
      ): Boolean {
        return oldItem === newItem
      }

      override fun areContentsTheSame(
        oldItem: PGPPublicKey,
        newItem: PGPPublicKey
      ): Boolean {
        return OpenPgpFingerprint.of(oldItem).equals(OpenPgpFingerprint.of(newItem))
            && oldItem.getLastModificationDate() == newItem.getLastModificationDate()
      }
    }
  }
}