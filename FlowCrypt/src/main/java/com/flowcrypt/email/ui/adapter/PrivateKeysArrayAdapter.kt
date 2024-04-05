/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.Context
import android.graphics.Color
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.core.text.toSpannable
import com.flowcrypt.email.R
import com.flowcrypt.email.security.model.PgpKeyRingDetails

/**
 * @author Denys Bondarenko
 */
class PrivateKeysArrayAdapter(context: Context, keys: List<PgpKeyRingDetails>, choiceMode: Int) :
  ArrayAdapter<PgpKeyRingDetails>(context, R.layout.private_key_item_radio_button, keys) {
  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private val layoutId = if (choiceMode == ListView.CHOICE_MODE_MULTIPLE) {
    R.layout.private_key_item_checkbox
  } else {
    R.layout.private_key_item_radio_button
  }

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    var view = convertView
    val item = getItem(position)

    val viewHolder: ViewHolder
    if (view == null) {
      viewHolder = ViewHolder()
      view = inflater.inflate(layoutId, parent, false)
      viewHolder.textViewEmail = view.findViewById(R.id.textViewEmail)
      viewHolder.textViewFingerprint = view.findViewById(R.id.textViewFingerprint)
      view.tag = viewHolder
    } else {
      viewHolder = view.tag as ViewHolder
    }

    updateView(item, viewHolder)

    return requireNotNull(view)
  }

  private fun updateView(pgpKeyRingDetails: PgpKeyRingDetails?, viewHolder: ViewHolder) {
    val userIds = pgpKeyRingDetails?.users?.joinToString(separator = ",\n") ?: ""
    viewHolder.textViewEmail?.text = userIds
    if (userIds.contains("\n")) {
      val spannable = viewHolder.textViewEmail?.text?.toSpannable()
      spannable?.setSpan(
        ForegroundColorSpan(Color.GRAY),
        userIds.indexOfFirst { it == '\n' },
        userIds.length,
        Spanned.SPAN_INCLUSIVE_INCLUSIVE
      )
      viewHolder.textViewEmail?.text = spannable
    }

    viewHolder.textViewFingerprint?.text = pgpKeyRingDetails?.fingerprint
  }

  private class ViewHolder {
    var textViewEmail: TextView? = null
    var textViewFingerprint: TextView? = null
  }
}
