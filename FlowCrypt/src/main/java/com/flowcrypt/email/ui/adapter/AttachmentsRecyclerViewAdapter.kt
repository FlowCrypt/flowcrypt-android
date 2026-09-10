/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.adapter

import android.graphics.drawable.AnimationDrawable
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.service.attachment.AttachmentDownloadManagerService

/**
 * @author Denys Bondarenko
 */
class AttachmentsRecyclerViewAdapter(
  private val isPreviewEnabled: Boolean = true,
  private val isDownloadEnabled: Boolean = true,
  private val isDeleteEnabled: Boolean = true,
  private val attachmentActionListener: AttachmentActionListener
) : ListAdapter<AttachmentInfo, AttachmentsRecyclerViewAdapter.ViewHolder>(DiffUtilCallBack()) {
  val progressMap = mutableMapOf<String, AttachmentDownloadManagerService.DownloadProgress>()

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
    private val imageButtonDeleteAtt: View = itemView.findViewById(R.id.imageButtonDeleteAtt)
    private val imageViewAttIcon: ImageView = itemView.findViewById(R.id.imageViewAttIcon)

    fun bindPost(
      attachmentInfo: AttachmentInfo,
      attachmentActionListener: AttachmentActionListener
    ) {
      if (attachmentInfo.isDecrypted) {
        itemView.setBackgroundResource(R.drawable.bg_att_decrypted)
      } else {
        itemView.setBackgroundResource(R.drawable.bg_att)
      }

      textViewAttName.text = attachmentInfo.name
      textViewAttSize.text = Formatter.formatFileSize(itemView.context, attachmentInfo.encodedSize)

      imageButtonDownloadAtt.visibleOrGone(isDownloadEnabled)
      imageButtonDownloadAtt.setOnClickListener {
        attachmentActionListener.onDownloadClick(attachmentInfo)
      }

      imageButtonPreviewAtt.visibleOrGone(isPreviewEnabled)
      imageButtonPreviewAtt.setOnClickListener {
        attachmentActionListener.onPreviewClick(attachmentInfo)
      }

      imageButtonDeleteAtt.visibleOrGone(isDeleteEnabled && !attachmentInfo.isProtected)
      imageButtonDeleteAtt.setOnClickListener {
        attachmentActionListener.onDeleteClick(attachmentInfo)
      }

      val value = progressMap[attachmentInfo.uniqueStringId]
      if (value?.progressInPercentage in 0..99) {
        imageViewAttIcon.setImageResource(R.drawable.stat_sys_download_blue)
        val animationDrawable = imageViewAttIcon.drawable as? AnimationDrawable
        animationDrawable?.let { animationDrawable.start() }
      } else {
        imageViewAttIcon.setImageResource(R.drawable.ic_attachment)
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
    fun onPreviewClick(attachmentInfo: AttachmentInfo)
    fun onDeleteClick(attachmentInfo: AttachmentInfo)
  }
}
