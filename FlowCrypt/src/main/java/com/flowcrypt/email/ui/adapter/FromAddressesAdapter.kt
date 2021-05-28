/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.flowcrypt.email.R
import com.flowcrypt.email.util.UIUtil
import java.util.*

/**
 * This is a custom realization of [ArrayAdapter] which can be used for showing the sender addresses.
 *
 * @author Denis Bondarenko
 * Date: 28.11.2018
 * Time: 5:22 PM
 * E-mail: DenBond7@gmail.com
 */
class FromAddressesAdapter<T>(
  context: Context,
  resource: Int,
  textViewResId: Int,
  val objects: List<T>
) : ArrayAdapter<T>(
  context, resource,
  textViewResId, objects
) {
  private val keysAvailability: MutableMap<String, Boolean> = HashMap()
  private var originalColor: Int = 0
  private var useKeysInfo: Boolean = false

  override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
    val view = super.getDropDownView(position, convertView, parent)

    val textView = view.findViewById<TextView>(android.R.id.text1)

    textView?.setTextColor(
      if (isEnabled(position)) originalColor else UIUtil.getColor(
        context,
        R.color.gray
      )
    )

    return view
  }

  override fun isEnabled(position: Int): Boolean {
    if (position < 0 || position >= count) {
      return super.isEnabled(position)
    }

    return if (useKeysInfo && getItem(position) is String) {
      val email = getItem(position) as String
      val result = keysAvailability[email]
      result ?: super.isEnabled(position)
    } else {
      super.isEnabled(position)
    }
  }

  override fun setDropDownViewResource(resource: Int) {
    super.setDropDownViewResource(resource)
    val textView = LayoutInflater.from(context).inflate(resource, null) as TextView
    originalColor = textView.currentTextColor
  }

  /**
   * This method can be used to disable the keys checking.
   *
   * @param useKeysInfo true if we want to check the key available, otherwise false.
   */
  fun setUseKeysInfo(useKeysInfo: Boolean) {
    this.useKeysInfo = useKeysInfo
    notifyDataSetChanged()
  }

  /**
   * Update information about the key availability for the given email.
   *
   * @param emailAddress The given email address
   * @param hasPgp       true if we have a private key for the given email address, otherwise false
   */
  fun updateKeyAvailability(emailAddress: String, hasPgp: Boolean) {
    keysAvailability[emailAddress] = hasPgp
  }

  /**
   * Check is given email address has a private key.
   *
   * @param emailAddress The given email address
   * @return true if the given email address has a private key, otherwise false.
   */
  fun hasPrvKey(emailAddress: String): Boolean {
    val result = keysAvailability[emailAddress]
    return keysAvailability.containsKey(emailAddress) && result != null && result
  }
}
