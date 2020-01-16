/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.preferences

import android.content.Context
import android.util.AttributeSet

import androidx.preference.DialogPreference


/**
 * A custom [androidx.preference.Preference]
 *
 * @author Denis Bondarenko
 * Date: 17.11.2018
 * Time: 13:00
 * E-mail: DenBond7@gmail.com
 */
class BuildConfInfoPreference : DialogPreference {
  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) :
      super(context, attrs, defStyleAttr, defStyleRes)

  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

  constructor(context: Context) : super(context)
}
