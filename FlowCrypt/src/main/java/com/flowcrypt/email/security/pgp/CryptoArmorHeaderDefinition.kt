/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

data class CryptoArmorHeaderDefinition (
        val begin: String,
        val middle: String? = null,
        val end: String?,
        val endRegexp: Regex? = null,
        val replace: Boolean
)
