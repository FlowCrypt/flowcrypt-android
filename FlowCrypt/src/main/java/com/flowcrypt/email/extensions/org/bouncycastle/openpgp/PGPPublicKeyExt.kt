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

fun PGPPublicKey.getLastModificationDate(): Date {
  val allSignatures = (listOf(signatures, keySignatures).flatMap { it.asSequence() }).toList()
  return allSignatures.maxByOrNull { it.creationTime }?.creationTime ?: creationTime
}

fun PGPPublicKey.getExpirationDate(): Date? = validSeconds.takeIf { it != 0L }?.let {
  Date(creationTime.time + TimeUnit.SECONDS.toMillis(it))
}

fun PGPPublicKey.getStatusColorStateList(context: Context): ColorStateList? {
  return ContextCompat.getColorStateList(
    context, when {
      hasRevocation() -> R.color.red
      getExpirationDate()?.before(Date()) == true -> R.color.orange
      else -> R.color.colorPrimary
    }
  )
}

fun PGPPublicKey.getStatusIconResId(): Int {
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