/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.node

import android.util.Base64
import java.math.BigInteger
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher


class NodeHost {

  /**
   * Receives requests from node that need responding
   */
  fun nodeReqHandler(name: String, request: CharSequence): String {
    val parts = request.split(',')
    return when (name) {
      "decryptRsaNoPadding" -> decryptRsaNoPadding(parts[0], parts[1])
      "verifyRsaModPow" -> verifyRsaModPow(parts[0], parts[1], parts[2])
      else -> throw Exception("Unknown NodeHost request name: $name")
    }
  }

  private fun decryptRsaNoPadding(derPrvBase64: String, encryptedBase64: String): String {
    val encryptedBytes = Base64.decode(encryptedBase64, Base64.DEFAULT)
    val keyBytes = Base64.decode(derPrvBase64, Base64.DEFAULT)
    val spec = PKCS8EncodedKeySpec(keyBytes)
    val kf = KeyFactory.getInstance("RSA")
    val prv = kf.generatePrivate(spec)
    val cipher = Cipher.getInstance("RSA/ECB/NoPadding") // padding is handled in OpenPGP.js
    cipher.init(Cipher.DECRYPT_MODE, prv)
    val decrypted = String(Base64.encode(cipher.doFinal(encryptedBytes), Base64.DEFAULT))
    return decrypted
  }

  private fun verifyRsaModPow(baseStr: String, exponentStr: String, moduloStr: String): String {
    val base = BigInteger(baseStr, 10)
    val exponent = BigInteger(exponentStr, 10)
    val modulo = BigInteger(moduloStr, 10)
    val result = base.modPow(exponent, modulo)
    return result.toString(10)
  }

}
