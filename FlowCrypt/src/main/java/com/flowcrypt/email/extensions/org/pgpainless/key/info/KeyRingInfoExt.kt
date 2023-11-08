/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.org.pgpainless.key.info

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import androidx.core.content.ContextCompat
import com.flowcrypt.email.R
import org.bouncycastle.openpgp.PGPPublicKey
import org.pgpainless.algorithm.KeyFlag
import org.pgpainless.key.info.KeyRingInfo

/**
 * @author Denys Bondarenko
 */
fun KeyRingInfo.usableForEncryption(): Boolean {
  return !publicKey.hasRevocation()
      && !isExpired()
      && isUsableForEncryption
      && primaryUserId?.isNotEmpty() == true
}

fun KeyRingInfo.usableForSigning(): Boolean {
  return !publicKey.hasRevocation()
      && !isExpired()
      && isSigningCapable
      && primaryUserId?.isNotEmpty() == true
}

fun KeyRingInfo.isExpired(): Boolean {
  return try {
    primaryKeyExpirationDate?.time?.let { System.currentTimeMillis() > it } ?: false
  } catch (e: Exception) {
    e.printStackTrace()
    false
  }
}

fun KeyRingInfo.getPrimaryKey(): PGPPublicKey? {
  return publicKeys.firstOrNull { it.isMasterKey }
}

fun KeyRingInfo.getPubKeysWithoutPrimary(): Collection<PGPPublicKey> {
  val primaryKey = getPrimaryKey() ?: return publicKeys
  return publicKeys - setOf(primaryKey)
}

fun KeyRingInfo.generateKeyCapabilitiesDrawable(context: Context, keyId: Long): Drawable? {
  val iconCertify = ContextCompat.getDrawable(context, R.drawable.ic_possibility_cert)
  val iconEncrypt = ContextCompat.getDrawable(context, R.drawable.ic_possibility_encryption)
  val iconSign = ContextCompat.getDrawable(context, R.drawable.ic_possibility_sign)
  val iconAuth = ContextCompat.getDrawable(context, R.drawable.ic_possibility_auth)

  val set = linkedSetOf<Drawable?>().toMutableSet()
  val keyFlags = getKeyFlagsOf(keyId)

  if (keyFlags.contains(KeyFlag.CERTIFY_OTHER)) {
    set.add(iconCertify)
  }
  if (keyFlags.contains(KeyFlag.ENCRYPT_COMMS) || keyFlags.contains(KeyFlag.ENCRYPT_STORAGE)) {
    set.add(iconEncrypt)
  }
  if (keyFlags.contains(KeyFlag.SIGN_DATA)) {
    set.add(iconSign)
  }
  if (keyFlags.contains(KeyFlag.AUTHENTICATION)) {
    set.add(iconAuth)
  }

  val array = set.filterNotNull().toTypedArray()
  return if (array.size > 1) {
    LayerDrawable(array).apply {
      for (i in array.indices) {
        val insetLeft = if (i > 0) {
          array.slice(0..<i).sumOf { it.intrinsicWidth }
        } else {
          0
        }

        val insetRight = if (i == array.size - 1) {
          0
        } else {
          array.slice(i + 1..<array.size).sumOf { it.intrinsicWidth }
        }

        setLayerInset(
          i,
          insetLeft,
          0,
          insetRight,
          0
        )
        setLayerGravity(i, Gravity.CENTER_VERTICAL)
      }
    }
  } else {
    array.firstOrNull()
  }
}
