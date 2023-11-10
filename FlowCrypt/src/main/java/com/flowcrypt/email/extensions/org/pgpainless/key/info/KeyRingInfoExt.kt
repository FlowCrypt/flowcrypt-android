/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.org.pgpainless.key.info

import android.content.Context
import android.content.res.ColorStateList
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
val KeyRingInfo.usableForEncryption: Boolean
  get() {
    return !publicKey.hasRevocation()
        && !isExpired
        && isUsableForEncryption
        && primaryUserId?.isNotEmpty() == true
  }

val KeyRingInfo.usableForSigning: Boolean
  get() {
    return !publicKey.hasRevocation()
        && !isExpired
        && isSigningCapable
        && primaryUserId?.isNotEmpty() == true
  }

val KeyRingInfo.isExpired: Boolean
  get() {
    return try {
      primaryKeyExpirationDate?.time?.let { System.currentTimeMillis() > it } ?: false
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

val KeyRingInfo.isPartiallyEncrypted: Boolean
  get() {
    return !isFullyDecrypted && !isFullyEncrypted
  }

val KeyRingInfo.isRevoked: Boolean
  get() {
    return publicKey.hasRevocation()
  }

fun KeyRingInfo.getPrimaryKey(): PGPPublicKey? {
  return publicKeys.firstOrNull { it.isMasterKey }
}

fun KeyRingInfo.getPubKeysWithoutPrimary(): Collection<PGPPublicKey> {
  val primaryKey = getPrimaryKey() ?: return publicKeys
  return publicKeys - setOf(primaryKey)
}

fun KeyRingInfo.generateKeyCapabilitiesDrawable(context: Context, keyId: Long): Drawable? {
  val keyFlags = getKeyFlagsOf(keyId)
  val drawables = listOf(
    KeyFlag.CERTIFY_OTHER to R.drawable.ic_possibility_cert,
    KeyFlag.ENCRYPT_COMMS to R.drawable.ic_possibility_encryption,
    KeyFlag.ENCRYPT_STORAGE to R.drawable.ic_possibility_encryption,
    KeyFlag.SIGN_DATA to R.drawable.ic_possibility_sign,
    KeyFlag.AUTHENTICATION to R.drawable.ic_possibility_auth
  ).mapNotNull { (flag, drawableRes) ->
    drawableRes.takeIf { keyFlags.contains(flag) }
  }.toSet().mapNotNull {
    ContextCompat.getDrawable(context, it)
  }

  return when {
    drawables.size > 1 -> LayerDrawable(drawables.toTypedArray()).apply {
      for (i in drawables.indices) {
        setLayerInset(
          i, drawables.take(i).sumOf { it.intrinsicWidth }, 0,
          drawables.takeLast(drawables.size - i - 1).sumOf { it.intrinsicWidth }, 0
        )
        setLayerGravity(i, Gravity.CENTER_VERTICAL)
      }
    }

    else -> drawables.firstOrNull()
  }
}

fun KeyRingInfo.getColorStateListDependsOnStatus(context: Context): ColorStateList? {
  return ContextCompat.getColorStateList(
    context, when {
      usableForEncryption -> R.color.colorPrimary
      usableForSigning -> R.color.colorAccent
      isRevoked -> R.color.red
      isExpired || isPartiallyEncrypted -> R.color.orange
      else -> R.color.gray
    }
  )
}

fun KeyRingInfo.getStatusIcon(): Int {
  return when {
    !isRevoked
        && !isExpired
        && !isPartiallyEncrypted -> R.drawable.ic_baseline_gpp_good_16

    else -> R.drawable.ic_outline_warning_amber_16
  }
}

fun KeyRingInfo.getStatusText(context: Context): String {
  return when {
    publicKey.hasRevocation() -> context.getString(R.string.revoked)
    isExpired -> context.getString(R.string.expired)
    isPartiallyEncrypted -> context.getString(R.string.not_valid)
    else -> context.getString(R.string.valid)
  }
}
