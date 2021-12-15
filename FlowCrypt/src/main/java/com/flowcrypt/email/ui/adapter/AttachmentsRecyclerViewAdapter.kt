/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.Intent
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.AttachmentInfo

/**
 * @author Denis Bondarenko
 *         Date: 11/24/20
 *         Time: 11:17 AM
 *         E-mail: DenBond7@gmail.com
 */
class AttachmentsRecyclerViewAdapter(private val listener: Listener) :
  ListAdapter<AttachmentInfo, AttachmentsRecyclerViewAdapter.ViewHolder>(DiffUtilCallBack()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.attachment_item, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    getItem(position)?.let { holder.bindPost(it, listener) }
  }

  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val textViewAttName: TextView = itemView.findViewById(R.id.textViewAttachmentName)
    private val textViewAttSize: TextView = itemView.findViewById(R.id.textViewAttSize)
    private val bDownloadAtt: View = itemView.findViewById(R.id.imageButtonDownloadAtt)

    fun bindPost(attachmentInfo: AttachmentInfo, listener: Listener) {
      if (attachmentInfo.isDecrypted) {
        itemView.setBackgroundResource(R.drawable.bg_att_decrypted)
      } else {
        itemView.setBackgroundResource(R.drawable.bg_att)
      }

      textViewAttName.text = attachmentInfo.getSafeName()
      textViewAttSize.text = Formatter.formatFileSize(itemView.context, attachmentInfo.encodedSize)

      bDownloadAtt.setOnClickListener {
        listener.onDownloadClick(attachmentInfo)
      }

      itemView.setOnClickListener { view ->
        attachmentInfo.uri?.let { uri ->
          if (uri.lastPathSegment?.endsWith("." + Constants.PGP_FILE_EXT) == true) {
            view.performClick()
          } else {
            val intentOpenFile = Intent(Intent.ACTION_VIEW, uri)
            intentOpenFile.action = Intent.ACTION_VIEW
            intentOpenFile.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intentOpenFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (intentOpenFile.resolveActivity(view.context.packageManager) != null) {
              view.context.startActivity(intentOpenFile)
            }
          }
        }
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

  interface Listener {
    fun onDownloadClick(attachmentInfo: AttachmentInfo)
  }
}
