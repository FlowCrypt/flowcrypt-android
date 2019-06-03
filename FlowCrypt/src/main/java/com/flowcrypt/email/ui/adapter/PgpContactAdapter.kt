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
import android.widget.TextView

import com.flowcrypt.email.R
import com.flowcrypt.email.database.dao.source.ContactsDaoSource
import com.flowcrypt.email.model.PgpContact
import com.hootsuite.nachos.NachoTextView

/**
 * This class describe a logic of create and show [PgpContact] objects in the
 * [NachoTextView].
 *
 * @author DenBond7
 * Date: 17.05.2017
 * Time: 17:44
 * E-mail: DenBond7@gmail.com
 */

class PgpContactAdapter(context: Context,
                        c: Cursor,
                        autoRequery: Boolean) : CursorAdapter(context, c, autoRequery) {

  override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
    return LayoutInflater.from(context).inflate(R.layout.pgp_contact_item, parent, false)
  }

  override fun convertToString(cursor: Cursor): CharSequence {
    return cursor.getString(cursor.getColumnIndex(ContactsDaoSource.COL_EMAIL))
  }

  override fun bindView(view: View, context: Context, cursor: Cursor) {
    val textViewName = view.findViewById<TextView>(R.id.textViewName)
    val textViewEmail = view.findViewById<TextView>(R.id.textViewEmail)
    val textViewOnlyEmail = view.findViewById<TextView>(R.id.textViewOnlyEmail)

    val name = cursor.getString(cursor.getColumnIndex(ContactsDaoSource.COL_NAME))
    val email = cursor.getString(cursor.getColumnIndex(ContactsDaoSource.COL_EMAIL))

    if (TextUtils.isEmpty(name)) {
      textViewEmail.text = null
      textViewName.text = null
      textViewOnlyEmail.text = email
    } else {
      textViewEmail.text = email
      textViewName.text = name
      textViewOnlyEmail.text = null
    }
  }
}
