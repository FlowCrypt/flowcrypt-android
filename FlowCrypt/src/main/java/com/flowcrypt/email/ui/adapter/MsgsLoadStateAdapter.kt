/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.LoadMsgsProgressBinding
import com.flowcrypt.email.extensions.visibleOrGone

/**
 * @author Denis Bondarenko
 *         Date: 5/18/22
 *         Time: 6:23 PM
 *         E-mail: DenBond7@gmail.com
 */
class MsgsLoadStateAdapter : LoadStateAdapter<MsgsLoadStateAdapter.ViewHolder>() {
  override fun onBindViewHolder(holder: ViewHolder, loadState: LoadState) {
    holder.bind(loadState)
  }

  override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): ViewHolder {
    return ViewHolder(
      LayoutInflater.from(parent.context)
        .inflate(R.layout.load_msgs_progress, parent, false)
    )
  }

  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding: LoadMsgsProgressBinding = LoadMsgsProgressBinding.bind(itemView)

    fun bind(loadState: LoadState) {
      binding.progressBar.visibleOrGone(loadState is LoadState.Loading)
    }
  }
}
