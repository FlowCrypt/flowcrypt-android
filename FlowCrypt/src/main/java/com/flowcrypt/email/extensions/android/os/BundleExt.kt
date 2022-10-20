/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.android.os

import android.os.Build
import android.os.Bundle
import android.os.Parcelable

/**
 * @author Denis Bondarenko
 *         Date: 10/19/22
 *         Time: 4:55 PM
 *         E-mail: DenBond7@gmail.com
 */
inline fun <reified T : Parcelable> Bundle.getParcelableViaExt(key: String): T? = when {
  Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelable(key, T::class.java)
  else -> @Suppress("DEPRECATION") getParcelable(key) as? T?
}

inline fun <reified T : java.io.Serializable> Bundle.getSerializableViaExt(key: String): T? = when {
  Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializable(key, T::class.java)
  else -> @Suppress("DEPRECATION") getSerializable(key) as? T?
}

inline fun <reified T : Parcelable> Bundle.getParcelableArrayListViaExt(key: String): ArrayList<T>? =
  when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
      getParcelableArrayList(key, T::class.java)
    }
    else -> @Suppress("DEPRECATION") getParcelableArrayList(key)
  }
