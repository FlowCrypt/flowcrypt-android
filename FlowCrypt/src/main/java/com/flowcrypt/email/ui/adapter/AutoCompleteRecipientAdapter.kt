/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.Context
import android.view.View
import android.widget.ArrayAdapter
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.RecipientEntity


/**
 * @author Denis Bondarenko
 *         Date: 7/12/22
 *         Time: 12:42 PM
 *         E-mail: DenBond7@gmail.com
 */
class AutoCompleteRecipientAdapter(
  context: Context, autoCompleteResult: List<RecipientEntity>
) : ArrayAdapter<RecipientEntity>(
  context,
  R.layout.fragment_user_recoverable_auth_exception,
  autoCompleteResult
) {

  internal class ViewHolder(itemView: View)
}
