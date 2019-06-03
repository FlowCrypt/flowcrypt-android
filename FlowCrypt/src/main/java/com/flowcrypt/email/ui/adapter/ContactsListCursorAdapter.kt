/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.Context
import android.database.Cursor
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.ImageButton
import android.widget.TextView

import com.flowcrypt.email.R
import com.flowcrypt.email.database.dao.source.ContactsDaoSource

/**
 * This adapter describes logic to prepare show contacts from the database.
 *
 * @author DenBond7
 * Date: 26.05.2017
 * Time: 18:00
 * E-mail: DenBond7@gmail.com
 */

class ContactsListCursorAdapter @JvmOverloads constructor(context: Context,
                                                          c: Cursor,
                                                          autoRequery: Boolean,
                                                          private val listener: OnDeleteContactListener?,
                                                          private val isDeleteEnabled: Boolean = true) :
    CursorAdapter(context, c, autoRequery) {

  override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
    return LayoutInflater.from(context).inflate(R.layout.contact_item, parent, false)
  }

  override fun bindView(view: View, context: Context, cursor: Cursor) {
    val textViewName = view.findViewById<TextView>(R.id.textViewName)
    val textViewEmail = view.findViewById<TextView>(R.id.textViewEmail)
    val textViewOnlyEmail = view.findViewById<TextView>(R.id.textViewOnlyEmail)
    val imageButtonDeleteContact = view.findViewById<ImageButton>(R.id.imageButtonDeleteContact)

    val name = cursor.getString(cursor.getColumnIndex(ContactsDaoSource.COL_NAME))
    val email = cursor.getString(cursor.getColumnIndex(ContactsDaoSource.COL_EMAIL))

    if (TextUtils.isEmpty(name)) {
      textViewName.visibility = View.GONE
      textViewEmail.visibility = View.GONE
      textViewOnlyEmail.visibility = View.VISIBLE

      textViewOnlyEmail.text = email
      textViewEmail.text = null
      textViewName.text = null
    } else {
      textViewName.visibility = View.VISIBLE
      textViewEmail.visibility = View.VISIBLE
      textViewOnlyEmail.visibility = View.GONE

      textViewEmail.text = email
      textViewName.text = name
      textViewOnlyEmail.text = null
    }

    if (isDeleteEnabled) {
      imageButtonDeleteContact.visibility = View.VISIBLE
      imageButtonDeleteContact.setOnClickListener {
        listener?.onClick(email)
      }
    } else {
      imageButtonDeleteContact.visibility = View.GONE
    }
  }

  /**
   * This listener can be used to determinate when a contact was deleted.
   */
  interface OnDeleteContactListener {
    fun onClick(email: String)
  }
}
