/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.Context
import android.database.Cursor
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.provider.BaseColumns
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.util.LongSparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.model.GeneralMessageDetails
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.UIUtil
import java.util.*
import java.util.regex.Pattern
import javax.mail.internet.InternetAddress

/**
 * The MessageListAdapter responsible for displaying the message in the list.
 *
 * @author DenBond7
 * Date: 28.04.2017
 * Time: 10:29
 * E-mail: DenBond7@gmail.com
 */

class MessageListAdapter(context: Context,
                         c: Cursor?) : CursorAdapter(context, c, false) {
  private val msgDaoSource: MessageDaoSource = MessageDaoSource()
  var localFolder: LocalFolder? = null
    set(localFolder) {
      field = localFolder
      if (localFolder != null) {
        this.folderType = FoldersManager.getFolderType(localFolder)
      } else {
        folderType = null
      }
    }
  private var folderType: FoldersManager.FolderType? = null
  private val senderNamePattern: Pattern
  private val states = LongSparseArray<Boolean>()
  private var defItemBg: Drawable? = null

  init {
    this.senderNamePattern = prepareSenderNamePattern()
  }

  override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
    val view = LayoutInflater.from(context).inflate(R.layout.messages_list_item, parent, false)
    if (defItemBg == null) {
      defItemBg = view.background
    }
    return view
  }

  override fun bindView(view: View, context: Context, cursor: Cursor) {
    val viewHolder = ViewHolder()
    viewHolder.textViewSenderAddress = view.findViewById(R.id.textViewSenderAddress)
    viewHolder.textViewDate = view.findViewById(R.id.textViewDate)
    viewHolder.textViewSubject = view.findViewById(R.id.textViewSubject)
    viewHolder.imageViewAtts = view.findViewById(R.id.imageViewAtts)
    viewHolder.viewIsEncrypted = view.findViewById(R.id.viewIsEncrypted)

    updateItem(context, msgDaoSource.getMsgInfo(cursor), viewHolder)

    val itemId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID))

    if (states.get(itemId) != null && states.get(itemId)) {
      view.setBackgroundColor(UIUtil.getColor(context, R.color.silver))
    } else {
      view.background = defItemBg
    }
  }

  override fun getItem(position: Int): GeneralMessageDetails? {
    val cursor = super.getItem(position) as Cursor
    return msgDaoSource.getMsgInfo(cursor)
  }

  fun updateItemState(position: Int, isCheck: Boolean) {
    states.put(getItemId(position), isCheck)
    notifyDataSetChanged()
  }

  fun clearSelection() {
    states.clear()
    notifyDataSetChanged()
  }

  /**
   * Update information of some item.
   *
   * @param details    A model which consist information about the
   * generalMessageDetails.
   * @param viewHolder A View holder object which consist links to views.
   */
  private fun updateItem(context: Context, details: GeneralMessageDetails?,
                         viewHolder: ViewHolder) {
    if (details != null) {
      val subject = if (TextUtils.isEmpty(details.subject)) {
        context.getString(R.string.no_subject)
      } else {
        details.subject
      }

      if (folderType != null) {
        when (folderType) {
          FoldersManager.FolderType.SENT -> viewHolder.textViewSenderAddress!!.text = generateAddresses(details.to)

          FoldersManager.FolderType.OUTBOX -> {
            val status = generateOutboxStatus(viewHolder.textViewSenderAddress!!.context,
                details.msgState)
            viewHolder.textViewSenderAddress!!.text = status
          }

          else -> viewHolder.textViewSenderAddress!!.text = generateAddresses(details.from)
        }
      } else {
        viewHolder.textViewSenderAddress!!.text = generateAddresses(details.from)
      }

      viewHolder.textViewSubject!!.text = subject
      if (folderType === FoldersManager.FolderType.OUTBOX) {
        viewHolder.textViewDate!!.text = DateTimeUtil.formatSameDayTime(context, details.sentDate)
      } else {
        viewHolder.textViewDate!!.text = DateTimeUtil.formatSameDayTime(context, details.receivedDate)
      }

      if (details.isSeen()) {
        changeViewsTypeface(viewHolder, Typeface.NORMAL)
        viewHolder.textViewSenderAddress!!.setTextColor(UIUtil.getColor(context, R.color.dark))
        viewHolder.textViewDate!!.setTextColor(UIUtil.getColor(context, R.color.gray))
      } else {
        changeViewsTypeface(viewHolder, Typeface.BOLD)
        viewHolder.textViewSenderAddress!!.setTextColor(UIUtil.getColor(context, android.R.color.black))
        viewHolder.textViewDate!!.setTextColor(UIUtil.getColor(context, android.R.color.black))
      }

      viewHolder.imageViewAtts!!.visibility = if (details.hasAtts) View.VISIBLE else View.GONE
      viewHolder.viewIsEncrypted!!.visibility = if (details.isEncrypted) View.VISIBLE else View.GONE
    } else {
      clearItem(viewHolder)
    }
  }

  private fun changeViewsTypeface(viewHolder: ViewHolder, typeface: Int) {
    viewHolder.textViewSenderAddress!!.setTypeface(null, typeface)
    viewHolder.textViewDate!!.setTypeface(null, typeface)
  }

  /**
   * Prepare a [Pattern] which will be used for finding some information in the sender name. This pattern is
   * case insensitive.
   *
   * @return A generated [Pattern].
   */
  private fun prepareSenderNamePattern(): Pattern {
    val domains = ArrayList<String>()
    domains.add("gmail.com")
    domains.add("yahoo.com")
    domains.add("live.com")
    domains.add("outlook.com")

    val stringBuilder = StringBuilder()
    stringBuilder.append("@")
    stringBuilder.append("(")
    stringBuilder.append(domains[0])

    for (i in 1 until domains.size) {
      stringBuilder.append("|")
      stringBuilder.append(domains[i])
    }
    stringBuilder.append(")$")

    return Pattern.compile(stringBuilder.toString(), Pattern.CASE_INSENSITIVE)
  }

  /**
   * Prepare the sender name.
   *
   *  * Remove common mail domains: gmail.com, yahoo.com, live.com, outlook.com
   *
   *
   * @param name An incoming name
   * @return A generated sender name.
   */
  private fun prepareSenderName(name: String): String {
    return senderNamePattern.matcher(name).replaceFirst("")
  }

  /**
   * Clear all views in the item.
   *
   * @param viewHolder A View holder object which consist links to views.
   */
  private fun clearItem(viewHolder: ViewHolder) {
    viewHolder.textViewSenderAddress!!.text = null
    viewHolder.textViewSubject!!.text = null
    viewHolder.textViewDate!!.text = null
    viewHolder.imageViewAtts!!.visibility = View.GONE
    viewHolder.viewIsEncrypted!!.visibility = View.GONE

    changeViewsTypeface(viewHolder, Typeface.NORMAL)
  }

  private fun generateAddresses(internetAddresses: List<InternetAddress>?): String {
    if (internetAddresses == null) {
      return "null"
    }

    val iMax = internetAddresses.size - 1
    if (iMax == -1) {
      return ""
    }

    val b = StringBuilder()
    var i = 0
    while (true) {
      val address = internetAddresses[i]
      val displayName = if (TextUtils.isEmpty(address.personal)) address.address else address.personal
      b.append(displayName)
      if (i == iMax) {
        return prepareSenderName(b.toString())
      }
      b.append(", ")
      i++
    }
  }

  private fun generateOutboxStatus(context: Context, messageState: MessageState): CharSequence {
    val me = context.getString(R.string.me)
    var state = ""
    var stateTextColor = ContextCompat.getColor(context, R.color.red)

    when (messageState) {
      MessageState.NEW, MessageState.NEW_FORWARDED -> {
        state = context.getString(R.string.preparing)
        stateTextColor = ContextCompat.getColor(context, R.color.colorAccent)
      }

      MessageState.QUEUED -> {
        state = context.getString(R.string.queued)
        stateTextColor = ContextCompat.getColor(context, R.color.colorAccent)
      }

      MessageState.SENDING -> {
        state = context.getString(R.string.sending)
        stateTextColor = ContextCompat.getColor(context, R.color.colorPrimary)
      }

      MessageState.ERROR_CACHE_PROBLEM, MessageState.ERROR_DURING_CREATION, MessageState.ERROR_ORIGINAL_MESSAGE_MISSING, MessageState.ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND, MessageState.ERROR_SENDING_FAILED, MessageState.ERROR_PRIVATE_KEY_NOT_FOUND -> {
        stateTextColor = ContextCompat.getColor(context, R.color.red)

        when (messageState) {
          MessageState.ERROR_CACHE_PROBLEM -> state = context.getString(R.string.cache_error)

          MessageState.ERROR_DURING_CREATION -> state = context.getString(R.string.could_not_create)

          MessageState.ERROR_ORIGINAL_MESSAGE_MISSING -> state = context.getString(R.string.original_message_missing)

          MessageState.ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND -> state = context.getString(R.string.original_attachment_not_found)

          MessageState.ERROR_SENDING_FAILED -> state = context.getString(R.string.cannot_send_message_unknown_error)

          MessageState.ERROR_PRIVATE_KEY_NOT_FOUND -> state = context.getString(R.string.could_not_create_no_key_available)

          else -> {
          }
        }
      }

      else -> {
      }
    }

    val meTextSize = context.resources.getDimensionPixelSize(R.dimen.default_text_size_big)
    val statusTextSize = context.resources.getDimensionPixelSize(R.dimen.default_text_size_very_small)

    val spannableStringMe = SpannableString(me)
    spannableStringMe.setSpan(AbsoluteSizeSpan(meTextSize), 0, me.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

    val status = SpannableString(state)
    status.setSpan(AbsoluteSizeSpan(statusTextSize), 0, state.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
    status.setSpan(ForegroundColorSpan(stateTextColor), 0, state.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

    return TextUtils.concat(spannableStringMe, " ", status)
  }

  /**
   * A view holder class which describes information about item views.
   */
  private class ViewHolder {
    internal var textViewSenderAddress: TextView? = null
    internal var textViewDate: TextView? = null
    internal var textViewSubject: TextView? = null
    internal var imageViewAtts: ImageView? = null
    internal var viewIsEncrypted: View? = null
  }
}
