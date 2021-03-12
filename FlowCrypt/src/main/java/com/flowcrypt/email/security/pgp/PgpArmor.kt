/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors = Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock

object PgpArmor {
    @JvmStatic
    val ARMOR_HEADER_DICT: Map<MsgBlock.Type, CryptoArmorHeaderDefinition> = mapOf(
            MsgBlock.Type.UNKNOWN to CryptoArmorHeaderDefinition(
                begin = "-----BEGIN",
                end = "-----END",
                replace = false
            ),
            MsgBlock.Type.PUBLIC_KEY to CryptoArmorHeaderDefinition(
                    begin = "-----BEGIN PGP PUBLIC KEY BLOCK-----",
                    end = "-----END PGP PUBLIC KEY BLOCK-----",
                    replace = true
            ),
            MsgBlock.Type.PRIVATE_KEY to CryptoArmorHeaderDefinition(
                    begin = "-----BEGIN PGP PRIVATE KEY BLOCK-----",
                    end = "-----END PGP PRIVATE KEY BLOCK-----",
                    replace = true
            ),
            MsgBlock.Type.CERTIFICATE to CryptoArmorHeaderDefinition(
                    begin = "-----BEGIN CERTIFICATE-----",
                    end = "-----END CERTIFICATE-----",
                    replace = true
            ),
            MsgBlock.Type.SIGNED_MSG to CryptoArmorHeaderDefinition(
                    begin = "-----BEGIN PGP SIGNED MESSAGE-----",
                    middle = "-----BEGIN PGP SIGNATURE-----",
                    end = "-----END PGP SIGNATURE-----",
                    replace = true
            ),
            MsgBlock.Type.SIGNATURE to CryptoArmorHeaderDefinition(
                    begin = "-----BEGIN PGP SIGNATURE-----",
                    end = "-----END PGP SIGNATURE-----",
                    replace = false
            ),
            MsgBlock.Type.ENCRYPTED_MSG to CryptoArmorHeaderDefinition(
                    begin = "-----BEGIN PGP MESSAGE-----",
                    end = "-----END PGP MESSAGE-----",
                    replace = true
            )
    )
}
