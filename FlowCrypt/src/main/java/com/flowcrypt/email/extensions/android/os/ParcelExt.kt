/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.android.os

import android.os.Build
import android.os.Parcel
import android.os.Parcelable

/**
 * @author Denys Bondarenko
 */
inline fun <reified T : Parcelable> Parcel.readParcelableViaExt(
  clazz: Class<T>
): T? = when {
  Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> readParcelable(clazz.classLoader, clazz)
  else -> @Suppress("DEPRECATION") readParcelable(clazz.classLoader)
}

inline fun <reified T> Parcel.readListViaExt(
  outVal: MutableList<T>,
  clazz: Class<T>
) = when {
  Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> readList(
    outVal,
    clazz.classLoader,
    clazz
  )
  else -> @Suppress("DEPRECATION") readList(outVal, clazz.classLoader)
}

inline fun <reified T : java.io.Serializable> Parcel.readSerializableViaExt(
  clazz: Class<T>
): T? = when {
  Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> readSerializable(
    clazz.classLoader,
    clazz
  )
  else -> @Suppress("DEPRECATION") readSerializable() as? T?
}
