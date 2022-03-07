/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.extensions.visibleOrGone

/**
 * @author Denis Bondarenko
 *         Date: 11/24/20
 *         Time: 11:17 AM
 *         E-mail: DenBond7@gmail.com
 */
class AttachmentsRecyclerViewAdapter(
  private val attachmentActionListener: AttachmentActionListener,
  var isPreviewEnabled: Boolean = false
) :
  ListAdapter<AttachmentInfo, AttachmentsRecyclerViewAdapter.ViewHolder>(DiffUtilCallBack()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.attachment_item, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    getItem(position)?.let { holder.bindPost(it, attachmentActionListener) }
  }

  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val textViewAttName: TextView = itemView.findViewById(R.id.textViewAttachmentName)
    private val textViewAttSize: TextView = itemView.findViewById(R.id.textViewAttSize)
    private val imageButtonDownloadAtt: View = itemView.findViewById(R.id.imageButtonDownloadAtt)
    private val imageButtonPreviewAtt: View = itemView.findViewById(R.id.imageButtonPreviewAtt)

    fun bindPost(
      attachmentInfo: AttachmentInfo,
      attachmentActionListener: AttachmentActionListener
    ) {
      imageButtonPreviewAtt.visibleOrGone(isPreviewEnabled)

      if (attachmentInfo.isDecrypted) {
        itemView.setBackgroundResource(R.drawable.bg_att_decrypted)
      } else {
        itemView.setBackgroundResource(R.drawable.bg_att)
      }

      textViewAttName.text = attachmentInfo.getSafeName()
      textViewAttSize.text = Formatter.formatFileSize(itemView.context, attachmentInfo.encodedSize)

      imageButtonDownloadAtt.setOnClickListener {
        attachmentActionListener.onDownloadClick(attachmentInfo)
      }

      imageButtonPreviewAtt.setOnClickListener {
        attachmentActionListener.onAttachmentPreviewClick(attachmentInfo)
      }

      itemView.setOnClickListener {
        attachmentActionListener.onAttachmentClick(attachmentInfo)
      }
    }
  }


  class DiffUtilCallBack : DiffUtil.ItemCallback<AttachmentInfo>() {
    override fun areItemsTheSame(oldItem: AttachmentInfo, newItem: AttachmentInfo) = oldItem
      .uniqueStringId == newItem.uniqueStringId && oldItem.email == newItem.email && oldItem
      .folder == newItem.folder

    override fun areContentsTheSame(oldItem: AttachmentInfo, newItem: AttachmentInfo) =
      oldItem == newItem
  }

  interface AttachmentActionListener {
    fun onDownloadClick(attachmentInfo: AttachmentInfo)
    fun onAttachmentClick(attachmentInfo: AttachmentInfo)
    fun onAttachmentPreviewClick(attachmentInfo: AttachmentInfo)
  }
}
