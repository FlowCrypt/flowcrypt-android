/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.android.content

import android.content.Intent
import android.os.Build
import android.os.Parcelable

/**
 * @author Denys Bondarenko
 */
inline fun <reified T : Parcelable> Intent.getParcelableExtraViaExt(key: String): T? = when {
  Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableExtra(key, T::class.java)
  else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T?
}

inline fun <reified T : Parcelable> Intent.getParcelableArrayListExtraViaExt(key: String): ArrayList<T>? =
  when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
      getParcelableArrayListExtra(key, T::class.java)
    }
    else -> @Suppress("DEPRECATION") getParcelableArrayListExtra(key)
  }
