/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.preferences

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceDialogFragmentCompat
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.util.exception.ExceptionUtil
import java.util.Formatter
import java.util.Locale

/**
 * This fragment shows a general information about the current build.
 *
 * @author Denis Bondarenko
 * Date: 10.07.2017
 * Time: 12:11
 * E-mail: DenBond7@gmail.com
 */
class BuildConfigInfoPreferencesFragment : PreferenceDialogFragmentCompat() {

  private var msg: String? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val clazz = BuildConfig::class.java
    val fields = clazz.declaredFields

    val formatter = Formatter(Locale.getDefault())

    for (field in fields) {
      try {
        formatter.format("%s: %s\n\n", field.name, field.get(null))
      } catch (e: IllegalAccessException) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
      }

    }

    msg = formatter.toString()
  }

  override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
    super.onPrepareDialogBuilder(builder)
    builder.setMessage(msg)
    builder.setNegativeButton(null, null)
  }

  override fun onDialogClosed(positiveResult: Boolean) {

  }

  companion object {
    fun newInstance(key: String): BuildConfigInfoPreferencesFragment {
      val fragment = BuildConfigInfoPreferencesFragment()
      val bundle = Bundle(1)
      bundle.putString(ARG_KEY, key)
      fragment.arguments = bundle
      return fragment
    }
  }
}
