/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.org.bouncycastle.openpgp

import android.content.Context
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.flowcrypt.email.R
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.security.pgp.PgpArmor
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPSignature
import java.io.IOException
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * @author Denys Bondarenko
 */
@Throws(IOException::class)
fun PGPPublicKey.armor(
  hideArmorMeta: Boolean = false,
  headers: List<Pair<String, String>>? = PgpArmor.FLOWCRYPT_HEADERS
): String {
  return SecurityUtils.armor(hideArmorMeta, headers) { this.encode(it) }
}

fun PGPPublicKey.getLastModificationDate(): Date? {
  var mostRecent: PGPSignature? = null
  for (signature in signatures) {
    if (mostRecent == null || signature.creationTime.after(mostRecent.creationTime)) {
      mostRecent = signature
    }
  }

  for (signature in keySignatures) {
    if (mostRecent == null || signature.creationTime.after(mostRecent.creationTime)) {
      mostRecent = signature
    }
  }

  return mostRecent?.creationTime
}

fun PGPPublicKey.getExpirationDate(): Date? {
  return if (validSeconds == 0L) {
    null
  } else {
    Date(creationTime.time + TimeUnit.SECONDS.toMillis(validSeconds))
  }
}

fun PGPPublicKey.getStatusColorStateList(context: Context): ColorStateList? {
  val isRevoked = hasRevocation()
  val isExpired = getExpirationDate()?.let { System.currentTimeMillis() > it.time } ?: false

  return ContextCompat.getColorStateList(
    context, when {
      isRevoked -> R.color.red
      isExpired -> R.color.orange
      else -> R.color.colorPrimary
    }
  )
}

fun PGPPublicKey.getStatusIcon(): Int {
  val isRevoked = hasRevocation()
  val isExpired = getExpirationDate()?.let { System.currentTimeMillis() > it.time } ?: false

  return when {
    !isRevoked && !isExpired -> R.drawable.ic_baseline_gpp_good_16

    else -> R.drawable.ic_outline_warning_amber_16
  }
}

fun PGPPublicKey.getStatusText(context: Context): String {
  val isRevoked = hasRevocation()
  val isExpired = getExpirationDate()?.let { System.currentTimeMillis() > it.time } ?: false
  return when {
    isRevoked -> context.getString(R.string.revoked)
    isExpired -> context.getString(R.string.expired)
    else -> context.getString(R.string.valid)
  }
}