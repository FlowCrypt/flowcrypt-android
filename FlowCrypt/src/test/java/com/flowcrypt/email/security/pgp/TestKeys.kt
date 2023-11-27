/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.pgpainless.key.OpenPgpV4Fingerprint
import org.pgpainless.key.protection.KeyRingProtectionSettings
import org.pgpainless.key.protection.PasswordBasedSecretKeyRingProtector
import org.pgpainless.key.protection.passphrase_provider.SecretKeyPassphraseProvider
import org.pgpainless.util.Passphrase

object TestKeys {
  fun genRingProtector(keys: List<KeyWithPassPhrase>): PasswordBasedSecretKeyRingProtector {
    val passphraseProvider = object : SecretKeyPassphraseProvider {
      override fun getPassphraseFor(keyId: Long?): Passphrase? {
        return doGetPassphrase(keyId)
      }

      override fun hasPassphrase(keyId: Long?): Boolean {
        return doGetPassphrase(keyId) != null
      }

      private fun doGetPassphrase(keyId: Long?): Passphrase? {
        for (pgpSecretKeyRing in keys.map { it.keyRing }) {
          val keyIDs = pgpSecretKeyRing.secretKeys.iterator().asSequence().map { it.keyID }
          if (keyIDs.contains(keyId)) {
            for (secretKey in pgpSecretKeyRing.secretKeys) {
              val openPgpV4Fingerprint = OpenPgpV4Fingerprint(secretKey)
              val passphrase = keys.firstOrNull {
                OpenPgpV4Fingerprint(it.keyRing) == openPgpV4Fingerprint
              }?.passphrase
              return passphrase
            }
          }
        }
        return null
      }
    }

    return PasswordBasedSecretKeyRingProtector(
      KeyRingProtectionSettings.secureDefaultSettings(), passphraseProvider
    )
  }

  data class TestKey(
    val publicKey: String,
    val privateKey: String,
    val decryptedPrivateKey: String,
    val passphrase: String,
    val longid: String
  ) {
    val listOfKeysWithPassPhrase: List<KeyWithPassPhrase>
      get() {
        return listOf(keyWithPassPhrase)
      }

    private val keyWithPassPhrase: KeyWithPassPhrase
      get() {
        return KeyWithPassPhrase(
          keyRing = PgpKey.extractSecretKeyRing(privateKey),
          passphrase = Passphrase.fromPassword(passphrase)
        )
      }
  }

  data class KeyWithPassPhrase(val keyRing: PGPSecretKeyRing, val passphrase: Passphrase)

  val KEYS = mapOf(
    "rsa1" to TestKey(
      publicKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
          "Version: FlowCrypt Email Encryption 6.3.5\n" +
          "Comment: Seamlessly send and receive encrypted email\n" +
          "\n" +
          "xsBNBFwBWOEBB/9uIqBYIPDQbBqHMvGXhgnm+b2i5rNLXrrGoalrp7wYQ654\n" +
          "Zln/+ffxzttRLRiwRQAOG0z78aMDXAHRfI9d3GaRKTkhTqVY+C02E8NxgB3+\n" +
          "mbSsF0Ui+oh1//LT1ic6ZnISCA7Q2h2U/DSAPNxDZUMu9kjh9TjkKlR81fiA\n" +
          "lxuD05ivRxCnmZnzqZtHoUvvCqsENgRjO9a5oWpMwtdItjdRFF7UFKYpfeA+\n" +
          "ct0uUNMRVdPK7MXBEr2FdWiKN1K21dQ1pWiAwj/5cTA8hu5Jue2RcF8FcPfs\n" +
          "niRihQkNqtLDsfY5no1B3xeSnyO2SES1bAHw8ObXZn/C/6jxFztkn4NbABEB\n" +
          "AAHNEFRlc3QgPHRAZXN0LmNvbT7CwHUEEAEIACkFAlwBWOEGCwkHCAMCCRA6\n" +
          "MPTMCpqPEAQVCAoCAxYCAQIZAQIbAwIeAQAA1pMH/R9oEVHaTdEzs/jbsfJk\n" +
          "6xm2oQ/G7KewtSqawAC6nou0+GKvgICxvkNK+BivMLylut+MJqh2gHuExdzx\n" +
          "HFNtKH69BzlK7hDBjyyrLuHIxc4YZaxHGe5ny3wF4QkEgfI+C5chH7Bi+jV6\n" +
          "94L40zEeFO2OhIif8Ti9bRb2Pk6UV5MrsdM0K6J0gTQeTaRecQSg07vO3E8/\n" +
          "GwfP2Dnq4yHICF/eaop+9QWj8UstEE6nEs7SSTrjIAxwAeZzpkjkXPXTLjz6\n" +
          "EcS/9EU7B+5v1qwXk1YeW1qerKJn6Qd6hqJ5gkVzq3sy3eODyrEwpNQoAR4J\n" +
          "8e3VQkKOn9oiAlFTglFeBhfOwE0EXAFY4QEH/2dyWbH3y9+hKk9RxwFzO+5n\n" +
          "GaqT6Njoh368GEEWgSG11NKlrD8k2y1/R1Nc3xEIWMHSUe1rnWWVONKhupwX\n" +
          "ABTnj8coM5beoxVu9p1oYgum4IwLF0yAtaWll1hjsECm/U33Ok36JDa0iu+d\n" +
          "RDfXbEo5cX9bzc1QnWdM5tBg2mxRkssbY3eTPXUe4FLcT0WAQ5hjLW0tPneG\n" +
          "zlu2q9DkmngjDlwGgGhMCa/508wMpgGugE/C4V41EiiTAtOtVzGtdqPGVdoZ\n" +
          "eaYZLc9nTQderaDu8oipaWIwsshYWX4uVVvo7xsx5c5PWXRdI70aUs5IwMRz\n" +
          "uljbq+SYCNta/uJRYc0AEQEAAcLAXwQYAQgAEwUCXAFY4QkQOjD0zAqajxAC\n" +
          "GwwAAI03B/9aWF8l1v66Qaw4O8P3VyQn0/PkVWJYVt5KjMW4nexAfM4BlUw6\n" +
          "97rP5IvfYXNh47Cm8VKqxgcXodzJrouzgwiPFxXmJe5Ug24FOpmeSeIl83Uf\n" +
          "CzaiIm+B6K5cf2NuHTrr4pElDaQ7RQGH2m2cMcimv4oWU9a0tRjt1e7XQAfQ\n" +
          "SWoCalUbLBeYORgVAF97MUNqeth6FMT5STjq+AGgnNZ2vdsUnASS/HbQQUUO\n" +
          "aVGVjo29lB6fS+UHT2gV/E/WQInjok5UrUMaFHwpO0VNP057DNyqhZwxaAs5\n" +
          "BsSgJlOC5hrT+PKlfr9ic75fqnJqmLircB+hVnfhGR9OzH3RCIky\n" +
          "=VKq5\n" +
          "-----END PGP PUBLIC KEY BLOCK-----\n" +
          "\n",
      privateKey = "-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
          "Version: FlowCrypt Email Encryption [BUILD_REPLACEABLE_VERSION]\n" +
          "Comment: Seamlessly send and receive encrypted email\n" +
          "\n" +
          "xcMGBFwBWOEBB/9uIqBYIPDQbBqHMvGXhgnm+b2i5rNLXrrGoalrp7wYQ654\n" +
          "Zln/+ffxzttRLRiwRQAOG0z78aMDXAHRfI9d3GaRKTkhTqVY+C02E8NxgB3+\n" +
          "mbSsF0Ui+oh1//LT1ic6ZnISCA7Q2h2U/DSAPNxDZUMu9kjh9TjkKlR81fiA\n" +
          "lxuD05ivRxCnmZnzqZtHoUvvCqsENgRjO9a5oWpMwtdItjdRFF7UFKYpfeA+\n" +
          "ct0uUNMRVdPK7MXBEr2FdWiKN1K21dQ1pWiAwj/5cTA8hu5Jue2RcF8FcPfs\n" +
          "niRihQkNqtLDsfY5no1B3xeSnyO2SES1bAHw8ObXZn/C/6jxFztkn4NbABEB\n" +
          "AAH+CQMIOXj58ei52QtgxArMeSOTfW3TXaT8V9bVH6G0wK1mVtHIZl5OXVkd\n" +
          "DWiOdwHiCPmphMkIeWurg5j8aL0vPTJx2pGFrfr/+Nj4LKfL3LC3UrEsYVQg\n" +
          "FyT5pSFYCONnMb3+uBg6mdBaCG9U7WyzSvAMH0bWhX4X1rEdReJO5CVwl84A\n" +
          "UN00olSMKW2KZ7BtwADm0qf/vfmfMH6BYrdZVhK1KXsXWLvvVhu7Y60a/V3c\n" +
          "U7okca2Fe8OzJpk3yJDkiT7IhDqePE5UCRBV6CYFAJeAbA/R38mysVGFGM9J\n" +
          "CRHmhiqsRt/USkQ2Il+Cc4BpiS7wMv8uhIWACg66jN7EsqmHXcdKkq3N6DgB\n" +
          "ABQzxfEXdUaqJbNEbkJamhgSWfwmL3Va59vADp4BgaogMCaPT0p4GS7vwtt3\n" +
          "vIOUB0CKgPTofyh1G5pW6DGLX5UthxLs6+Nt4woaD90zTYwld1cG6HjmYBmy\n" +
          "wVEpxkFSnYtHimEP+nq1pll/3I2wKwVbZFELXaRNTWiYVkjhLR9Vbx1E7Mkg\n" +
          "gjc72zxAxYso7oCtAODhjy5WA0vKV830500cHUaiDtHmCSOqnJHJ5kcIWtC2\n" +
          "y1qt25jv8wOHCpLT77z1OkIS/keabRwvaivWH7TXp3qKvyCYyhO4EpoJk29n\n" +
          "LACVZBVZFmLy6/oyVWrRXXFWeURtb/dUZG1k9AZlecMrTIaEAJKqDBshjat/\n" +
          "eF0KhJ+C2AdIe2PCnX4LWS4Y6shM4VZoRcSBzpx8QbhOUUzAM5WYm9JH7kTE\n" +
          "F9p0qqKVHbXHFup7p2ptjwyL3Axu3Oi8/8pqRe2Kl+YVfR0JWT7/UZTDQomq\n" +
          "s72AFZddJy6RbgfeJxX376UhUqDVgZN07Ih2PcCcex8Bf10IccMNC74dxmAy\n" +
          "Ytf6LQP7Uws0pyqiusBZJoNsdgsJ9MbTzRBUZXN0IDx0QGVzdC5jb20+wsB1\n" +
          "BBABCAApBQJcAVjhBgsJBwgDAgkQOjD0zAqajxAEFQgKAgMWAgECGQECGwMC\n" +
          "HgEAANaTB/0faBFR2k3RM7P427HyZOsZtqEPxuynsLUqmsAAup6LtPhir4CA\n" +
          "sb5DSvgYrzC8pbrfjCaodoB7hMXc8RxTbSh+vQc5Su4QwY8sqy7hyMXOGGWs\n" +
          "RxnuZ8t8BeEJBIHyPguXIR+wYvo1eveC+NMxHhTtjoSIn/E4vW0W9j5OlFeT\n" +
          "K7HTNCuidIE0Hk2kXnEEoNO7ztxPPxsHz9g56uMhyAhf3mqKfvUFo/FLLRBO\n" +
          "pxLO0kk64yAMcAHmc6ZI5Fz10y48+hHEv/RFOwfub9asF5NWHltanqyiZ+kH\n" +
          "eoaieYJFc6t7Mt3jg8qxMKTUKAEeCfHt1UJCjp/aIgJRU4JRXgYXx8MGBFwB\n" +
          "WOEBB/9nclmx98vfoSpPUccBczvuZxmqk+jY6Id+vBhBFoEhtdTSpaw/JNst\n" +
          "f0dTXN8RCFjB0lHta51llTjSobqcFwAU54/HKDOW3qMVbvadaGILpuCMCxdM\n" +
          "gLWlpZdYY7BApv1N9zpN+iQ2tIrvnUQ312xKOXF/W83NUJ1nTObQYNpsUZLL\n" +
          "G2N3kz11HuBS3E9FgEOYYy1tLT53hs5btqvQ5Jp4Iw5cBoBoTAmv+dPMDKYB\n" +
          "roBPwuFeNRIokwLTrVcxrXajxlXaGXmmGS3PZ00HXq2g7vKIqWliMLLIWFl+\n" +
          "LlVb6O8bMeXOT1l0XSO9GlLOSMDEc7pY26vkmAjbWv7iUWHNABEBAAH+CQMI\n" +
          "PqtEWmogeSBgMbGVnYVID1zzpRIum4ifUnA7HOgJ/AbrWrD6OvUjQsHsQtSo\n" +
          "jANPVtL85PICEKGDLm/wFKzENgB1ZsFvSi6IwdOIdq4rckCgJRw+R0xNxtiX\n" +
          "FoqoFM5MkwQRfrXJgWO0YjdG2AGMsPufWRV9N2aFBoiWQqbxvkmOdO4/qAdS\n" +
          "FOGr1+eu3P693yuuZlD9cdO44Md28PtldoXenNhLuEqxhw8/Yb1/U8u66WAl\n" +
          "z9JUYLwI4U/juhqekU+zNWs9H0Bh1yd4dcN9NT0nyc1GrdCKypcWth2DVMmP\n" +
          "zFluwz4NnIW2VokE5rKofKUXbEYstua0ZY5Vz9mdNEmX9LZmBwCLwwC0j71d\n" +
          "KYiJWVgxL28jCrF85eBqnmXEIkoE6hGeptaBZ8nTkSMpEdZZCif6+Vxn9JAd\n" +
          "G9KYV/EeP2Hf07aYI6YRMmgNSHIso5m5rrfX9E8P2mhmqAhiV6xBPDJM4SdQ\n" +
          "1y93zUm/rpWflBw3PkC6CHtZ2pem9aLdigBcIgGYtmbblY234vT/EdlA8OPy\n" +
          "qUXZ8HPIby911qzDmWEXdhuG8OdIhvp4GVgyJ6sUvgzrcDM4Uond7jG8m5O3\n" +
          "lQmbYBx3L4ZLYoUW5pIjxXVWSPrbBhjnShwwNukhj2GfXOS8+gZS0Mrw/EVT\n" +
          "BUIe4sgiv0M7XaVXX+CYMJ+1dsWzgPwMqN3MrxCgf2D7ujsfSTHunE5sCei1\n" +
          "O0H2SAL3Lr2V2b2PnfRy/UMPaFdAfxXGJKrOdpuM27LZvAa+QeLKA0emlZuT\n" +
          "4nKsl1QGzTV/3EI2gdCYLyjwOq05qdCy0B/0tfJ2tXS1AOPPaKcDyCkrenzA\n" +
          "w6rZipO7t7oQYsDXOzZEE1Y370M8DFBTcVbC5OjRy1M/REXD5QIP9Fl4DYUW\n" +
          "gk8zqqjQfuyQkd0r3kS0NHL1wsBfBBgBCAATBQJcAVjhCRA6MPTMCpqPEAIb\n" +
          "DAAAjTcH/1pYXyXW/rpBrDg7w/dXJCfT8+RVYlhW3kqMxbid7EB8zgGVTDr3\n" +
          "us/ki99hc2HjsKbxUqrGBxeh3Mmui7ODCI8XFeYl7lSDbgU6mZ5J4iXzdR8L\n" +
          "NqIib4Horlx/Y24dOuvikSUNpDtFAYfabZwxyKa/ihZT1rS1GO3V7tdAB9BJ\n" +
          "agJqVRssF5g5GBUAX3sxQ2p62HoUxPlJOOr4AaCc1na92xScBJL8dtBBRQ5p\n" +
          "UZWOjb2UHp9L5QdPaBX8T9ZAieOiTlStQxoUfCk7RU0/TnsM3KqFnDFoCzkG\n" +
          "xKAmU4LmGtP48qV+v2Jzvl+qcmqYuKtwH6FWd+EZH07MfdEIiTI=\n" +
          "=15Xc\n" +
          "-----END PGP PRIVATE KEY BLOCK-----\n",
      decryptedPrivateKey = "-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
          "Version: FlowCrypt Email Encryption 0.0.1-dev\n" +
          "Comment: Seamlessly send and receive encrypted email\n" +
          "\n" +
          "xcLYBFwBWOEBB/9uIqBYIPDQbBqHMvGXhgnm+b2i5rNLXrrGoalrp7wYQ654\n" +
          "Zln/+ffxzttRLRiwRQAOG0z78aMDXAHRfI9d3GaRKTkhTqVY+C02E8NxgB3+\n" +
          "mbSsF0Ui+oh1//LT1ic6ZnISCA7Q2h2U/DSAPNxDZUMu9kjh9TjkKlR81fiA\n" +
          "lxuD05ivRxCnmZnzqZtHoUvvCqsENgRjO9a5oWpMwtdItjdRFF7UFKYpfeA+\n" +
          "ct0uUNMRVdPK7MXBEr2FdWiKN1K21dQ1pWiAwj/5cTA8hu5Jue2RcF8FcPfs\n" +
          "niRihQkNqtLDsfY5no1B3xeSnyO2SES1bAHw8ObXZn/C/6jxFztkn4NbABEB\n" +
          "AAEAB/9UPKvDfD50S5rmubJLILxGK9I93JJaHXRiJJf+vWaCcJHriO1hegGI\n" +
          "s5zPs9xkRgJKx9rUAPebxC2n2suVENRqRstpjEuvhvKdn/QmxcUrTMkBrzK0\n" +
          "FEd3aXKDUBLk+iJZZExgtdNWdqh5RRN7gOIn8zu/h94htba1XLsbL3heFOKW\n" +
          "BeOiKQXgbxi0swEEThK9kVWRuZjNIVnEGf+Oiguj/g4f3FT5u16lLSMjXXLF\n" +
          "o05EmqvZ/rGtchaLzDlroMVXk9ME17tcztWJw2ThPXR+oyMsWYmSMCyS8Stw\n" +
          "roYI4rMWfZBUXX1A7Wq23/fzbey5yIHlSWIQBDkHISM0DYvxBACrD0JZkB9v\n" +
          "bRqn/zgtngoM4+EfZfF1UVZXYD0l6WtyOQzU2/egyG0r59nad2j5OXYlaZfw\n" +
          "BJDWkGe5Zoalsqdx/AtPW8XS/MmvA8EaZaP9d8fcR8NH5dhu5WoZ6rKtb1mg\n" +
          "IvcLEpVlHOtwU2j0teWYRt1R83S6bRrHbtcU0T67jQQApNLKhB9O5fhkMAcx\n" +
          "rUdaxgujHAdV3m0dFbHc6gcqW/AX3vZlISGH4ev2QUY1cBGtBNgox0o82v7h\n" +
          "ehOw0AgVAar2zL+lgvR9+2bVlcO8RKyAFWj9CEoOEBf8P6AvpX2l1P6d6cSU\n" +
          "lwrl8k34b3Nv5lS1qcJaeGef43FN5brADIcD/3/zLbYaRX9pJCz1xoVX1nkC\n" +
          "Mu0K19/USDJmgMyU+0NrLt9xcRw9aI5FszYPqs4NBzRFl4ChPtZiT5MTxQMi\n" +
          "pVtBGGrbQSAbaj34o2RAf/A1CgkHwsDim11H9CIRlVlYXJCc1yMFZkcNsXhh\n" +
          "34W4EbqZqErtbd3RoGhU96CQFKS3QULNEFRlc3QgPHRAZXN0LmNvbT7CwH8E\n" +
          "EAEIACkFAlwBWOEGCwkHCAMCCRA6MPTMCpqPEAQVCAoCAxYCAQIZAQIbAwIe\n" +
          "AQAKCRA6MPTMCpqPENaTB/0faBFR2k3RM7P427HyZOsZtqEPxuynsLUqmsAA\n" +
          "up6LtPhir4CAsb5DSvgYrzC8pbrfjCaodoB7hMXc8RxTbSh+vQc5Su4QwY8s\n" +
          "qy7hyMXOGGWsRxnuZ8t8BeEJBIHyPguXIR+wYvo1eveC+NMxHhTtjoSIn/E4\n" +
          "vW0W9j5OlFeTK7HTNCuidIE0Hk2kXnEEoNO7ztxPPxsHz9g56uMhyAhf3mqK\n" +
          "fvUFo/FLLRBOpxLO0kk64yAMcAHmc6ZI5Fz10y48+hHEv/RFOwfub9asF5NW\n" +
          "HltanqyiZ+kHeoaieYJFc6t7Mt3jg8qxMKTUKAEeCfHt1UJCjp/aIgJRU4JR\n" +
          "XgYXx8LYBFwBWOEBB/9nclmx98vfoSpPUccBczvuZxmqk+jY6Id+vBhBFoEh\n" +
          "tdTSpaw/JNstf0dTXN8RCFjB0lHta51llTjSobqcFwAU54/HKDOW3qMVbvad\n" +
          "aGILpuCMCxdMgLWlpZdYY7BApv1N9zpN+iQ2tIrvnUQ312xKOXF/W83NUJ1n\n" +
          "TObQYNpsUZLLG2N3kz11HuBS3E9FgEOYYy1tLT53hs5btqvQ5Jp4Iw5cBoBo\n" +
          "TAmv+dPMDKYBroBPwuFeNRIokwLTrVcxrXajxlXaGXmmGS3PZ00HXq2g7vKI\n" +
          "qWliMLLIWFl+LlVb6O8bMeXOT1l0XSO9GlLOSMDEc7pY26vkmAjbWv7iUWHN\n" +
          "ABEBAAEAB/sF5T91ZBDrasz1fkygKYgV2yxcS1eu3Pmz4FZlhznOyQUbCDQb\n" +
          "2SbgnetbteREnTwpt6nRpRtwSaPWZT80XB82Echg6kqeY4vZ0dweNm+4CEet\n" +
          "04f9ZSx1B03rzKqj1KCFC/z3qrTbpUhxxX24zP8v77wnLP06oUiHNZvF7m8k\n" +
          "UyMTPk9S1NuI5/pM+szMGu/gXXK7yoOfvgrDvMgI2Ko2V7t6VT4Qg2cK+ZBc\n" +
          "rhhPDsADHc7lh5IpfcwOIHiBD+IWIOP80Q9NjGpeOCUfq72uzTGadd99KSH8\n" +
          "qliBIQHS4rbfheF+j0yKJJJF9kMSAKRd1A2HTtXoqMBLl/+DFCBlBAC8ERD/\n" +
          "6WDRXeCGvepGZC2XCFwrwvoZH3sxOKsZK/0w2IN8Uyn5/TdbT3fLYx6ZQE9F\n" +
          "Oa+iypHtWjVKniBaUldf2qnM3xPln3lzAoDQUMLuKqwQkrEgGrJcRePuQWSl\n" +
          "6a8PIsZ1fyEMtZ4HBXtNo5UiAM3eGRxly8tD7DhPY9IPBwQAjNBKOuartuKR\n" +
          "rnAzXpANPlQIKjmAbnWb/p34VBeGWtF00DJZCVkXUO4SW1PbRTj4wepKNppS\n" +
          "fgrA3FXdr4WW/Ku1gqBWynlhboobPXZ2pKAUYlyK95OH/ff6v0303oqFbGTv\n" +
          "yjRlQ0GVV6A2SZ73c0bxgUVGJbYxY+zagTlLv4sD/2dRJLgGHPHXh5kJHCoy\n" +
          "v1iUxUgPJ9mfozRFuXl5vxz1NaghqdMcSyftTsDIy7lKOqf4lOxLCfRp1l+j\n" +
          "Def9QQD1J7six0cDwdZ8AI8j5vEJWLMluO7Dzil010nBhU6hzKFa84aJQ7Sm\n" +
          "7kBvyUUlyuiCR5olTyvvIlIFvmJojOtsPlnCwGkEGAEIABMFAlwBWOEJEDow\n" +
          "9MwKmo8QAhsMAAoJEDow9MwKmo8QjTcH/1pYXyXW/rpBrDg7w/dXJCfT8+RV\n" +
          "YlhW3kqMxbid7EB8zgGVTDr3us/ki99hc2HjsKbxUqrGBxeh3Mmui7ODCI8X\n" +
          "FeYl7lSDbgU6mZ5J4iXzdR8LNqIib4Horlx/Y24dOuvikSUNpDtFAYfabZwx\n" +
          "yKa/ihZT1rS1GO3V7tdAB9BJagJqVRssF5g5GBUAX3sxQ2p62HoUxPlJOOr4\n" +
          "AaCc1na92xScBJL8dtBBRQ5pUZWOjb2UHp9L5QdPaBX8T9ZAieOiTlStQxoU\n" +
          "fCk7RU0/TnsM3KqFnDFoCzkGxKAmU4LmGtP48qV+v2Jzvl+qcmqYuKtwH6FW\n" +
          "d+EZH07MfdEIiTI=\n" +
          "=GVVZ\n" +
          "-----END PGP PRIVATE KEY BLOCK-----\n" +
          "\n",
      passphrase = "some long pp",
      longid = "3A30F4CC0A9A8F10"
    ),
    "rsa2" to TestKey(
      publicKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
          "Version: FlowCrypt Email Encryption 6.3.5\n" +
          "Comment: Seamlessly send and receive encrypted email\n" +
          "\n" +
          "xsFNBFwFqkQBEADxLDVykJKqNCBGHqF8Hw2lLkCWnR8OPGmoqMALl+KstBPm\n" +
          "7vraDYy/JDRZ6Cju5X7z8IrIrrM7knyjz3Z/ICYjdpaA5XSCqMjrmlXbhnRH\n" +
          "rdy/c5/ubQsAgUB9VqjNEpYC1OZ9Fz8tB0IiHgq+keIVh/xKf7EAvq1VYLZO\n" +
          "k8kE81lvNeqX0hXo2JVvGiQ6fuBv5w4shvDzKfirsIepxaLwj3GJUcW+zhrg\n" +
          "QztuoRskr+PerGp4sf5sX8pci/kDuwaYFXJ4DNqCt/LLZ+XtxhyHDW4Dbh5f\n" +
          "LKXWoNq7RPkCX18aA9nRCPwuyxKd6TkjzwKSm0r16ResgnnCVGeqjBHxlyQq\n" +
          "RDR9MhmjOvmEuZ19axnwcwBbFHvmcSy8Or/RMuPv4ZusaOEyeC3VLn3Tj+be\n" +
          "BgkikcpMWEJH8nDppEX5hIW2hjsHz3atD21LoXyQFi8c0E6wArcIyDbxWKZj\n" +
          "1/nZkP1Fk3MDk7L/f2YO5LkUDHlhb12zNDJ4B/nggpAODMxqCPF2aoY0ryvg\n" +
          "bru54WG3z2+Z0n6KP3m9mIHQZosBdYCnvKilKotO2SgUqa7B7pPDV7XPynO5\n" +
          "Cprl2CHixIzZ9R50jGkR7q8H4BGWBXXfm8kap0/Yy/rICs6nYAhSAPN6CNny\n" +
          "FpirPawL7iRzkMalvMhrCotJRGiB+qOPPhhFkQARAQABzRF1c3IgPHVzckB1\n" +
          "c3IuY29tPsLBdQQQAQgAKQUCXAWqRgYLCQcIAwIJEHwwfm8gkpYtBBUICgID\n" +
          "FgIBAhkBAhsDAh4BAADBZw//UvbLWWKHloxn3GoWlWPpHvuNdnsSVqY0+iQy\n" +
          "zHa2QYp4MxYXbiedXEqlp57yp49nd4pgwCLIgGhHp2hHpyK/SSeV5WK6gUyy\n" +
          "ba1NzEdXBJQHEwTn88nFJw9gGNOXTZJkhDYrtCJDHIms1WlQoayY1Xx/Nrr9\n" +
          "pm/TSmYaH3DaldktHPCxq7CoKaoHrnZiI1qmC22J92/psDTgbk2EfyWU6qyI\n" +
          "X/rOveiIRVdBpiXflGduf9896IZqss/BwdEyH0rCG6dxuxIcy3G62zWmYf5X\n" +
          "4yMz3qQ9fWOpORBGCIYvAjNiPcTmnUaNpa1oVz/jGL1i72bSYn4DPTPiA3Y9\n" +
          "iE1Ql4CnZKQQ/hNqPtnSBOXSv3jKCcts+YsMcoK2y5bZhFtG943XbKGryV8M\n" +
          "3J7pYH2T9g6vY44NGtVRMdfDiMFNSXI5gnzCzQ0JjKPEfpnrGfCLepWvheTp\n" +
          "zvA8PJSkHHM8XVzxj78+KaKRRWg6Xi0zpNK0+sJRNf6GdkDT9Tr4dfcwqedd\n" +
          "xMt9SQHlNssT4hLB6if0pEI3PKbPSe+UuCwt8Euh1BsfuFRbd3K9qJN5rsxq\n" +
          "3nrLoIwbEKsXbmv1kzXwNnFeotFA4CdDzf6wZ4t+zKPMYQ/TjTd+AHeprE8g\n" +
          "bMzBB1TCwj1oL2fOiMwmIrINU2ITTKsfK3mX/SKrsuUmXc7OwU0EXAWqRAEQ\n" +
          "AK/dtuYoguNc0axw2HKlyBsM9h1UWLZ1SViYrsguYZUmG63o51cQn5e/2+he\n" +
          "C0IceMVVgsaPNv/aHB78hWnsKDUR9sXcTFncdyFiXx7NaahkgXE1dC/5wO64\n" +
          "k5gBv/OIWNae02jNgijtjc86UkGqVGOYhRuiBtaNfegUs0Uhww/6N4zoYCmH\n" +
          "4PFdRXQqT2es/8uWW1o9QkbsdOeP95ZwbGR1FyfktFY0nUYpiQWpo0o1kEqE\n" +
          "ux/fT9GyfU00vqE8g0naHLNLezP0OvvDbE6PMVmSmh3cWXEQdlL9+9zjKF6j\n" +
          "ewANqEJgE2AL7kCYQVw7QM1wmwtWJDMtkhqeH7qe9BJdxfRLxoCi1BwYLb0q\n" +
          "jKYq6xE8U/fZ5Zi7BJMGOMT5cAIVjCUuzzic72GLVnpMt+U76Un1F4sBH+jD\n" +
          "fpHVMwQ0592XW+6fzVS6e7mYD/p3rYOKD9XDGVdCDrW9bs214T0f/WWzqon7\n" +
          "q+49Btm9Xg/Pj35/OIDMJtJ8m59zqQItkV1XWaT6yZTre2yglMZnzIprj+KP\n" +
          "z4TnNmlGKPg8XZAski6bYknnff8YvSNKacrpJPY7fDFy0pIUIBDRXIqFA7EC\n" +
          "RvMIjpJtQtu5VjI8M480afhFh3MY3I0IMIpyYn94KbPFNgYPjJLsLPVsZEI0\n" +
          "eFI6LJrl4CNt3iaVvERZ9RujABEBAAHCwV8EGAEIABMFAlwFqkgJEHwwfm8g\n" +
          "kpYtAhsMAAAqvhAAr5zPDmpxfEHvPsq4dewxBwQ4ieb8Ui9PiYN4MkVR6Dz9\n" +
          "szqBtgZDojNFRwqUscJMAes3WgaMDvBq4vcL3GE1EC1laeQ0zMbbwZckK7Po\n" +
          "VYFnPix8dLNBnjTpIbV7A5HD0bs645bnKcXcIP5LUUvEiGo7hnIxzVmeZii7\n" +
          "B2c/9mcBV1EJKCi8XCz91kfmQ8+44dhjMwQ9g1LR1A4E5e0M+YNRJB83dvKo\n" +
          "0U6iN26OBfr12juYIV5iK8j2n7Ads+WORmK2W+gFwu0T2B2udDE31eJmVU9T\n" +
          "UOIWa79BZ/6h4dSGUiPGe3z5m7yecJDQ9Z9Bcrjgkyonw45PGN1zbSnzgw68\n" +
          "sTi9NcrsOXsSu1s7LAhzroJMOtRvN/3N41eoiwoAdfWLQ9nNpTGWj28hbhKt\n" +
          "f1B+K4u9KHctlRuB+APguVeaJ60zB4pM31Cxn7i2HwhLnpbri8YCsMhc53hG\n" +
          "pkxfiX3ZmyE3bXplI3s1uY6yVNpDAUfJfbVQzlMpS4xqJaphB/2lfOypN1r4\n" +
          "46/ttzi7vX6S3fvmOtnUPC7JK7I1EoK++rCyreepBvuFQ72RekLaIFj5xVcz\n" +
          "KuueNumlpGvWsfXrqIegggJcdBFBGxmoSugnZ3budhYTFmaol2QKDZFqAFeJ\n" +
          "KQk670ezsXQMRX/AeM4Ttn2ZIjItmIpo7mUOfqE=\n" +
          "=eep/\n" +
          "-----END PGP PUBLIC KEY BLOCK-----\n" +
          "\n",
      privateKey = "-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
          "Version: FlowCrypt Email Encryption\n" +
          "Comment: Seamlessly send, receive and search encrypted email\n" +
          "\n" +
          "xcaGBFwFqkQBEADxLDVykJKqNCBGHqF8Hw2lLkCWnR8OPGmoqMALl+KstBPm\n" +
          "7vraDYy/JDRZ6Cju5X7z8IrIrrM7knyjz3Z/ICYjdpaA5XSCqMjrmlXbhnRH\n" +
          "rdy/c5/ubQsAgUB9VqjNEpYC1OZ9Fz8tB0IiHgq+keIVh/xKf7EAvq1VYLZO\n" +
          "k8kE81lvNeqX0hXo2JVvGiQ6fuBv5w4shvDzKfirsIepxaLwj3GJUcW+zhrg\n" +
          "QztuoRskr+PerGp4sf5sX8pci/kDuwaYFXJ4DNqCt/LLZ+XtxhyHDW4Dbh5f\n" +
          "LKXWoNq7RPkCX18aA9nRCPwuyxKd6TkjzwKSm0r16ResgnnCVGeqjBHxlyQq\n" +
          "RDR9MhmjOvmEuZ19axnwcwBbFHvmcSy8Or/RMuPv4ZusaOEyeC3VLn3Tj+be\n" +
          "BgkikcpMWEJH8nDppEX5hIW2hjsHz3atD21LoXyQFi8c0E6wArcIyDbxWKZj\n" +
          "1/nZkP1Fk3MDk7L/f2YO5LkUDHlhb12zNDJ4B/nggpAODMxqCPF2aoY0ryvg\n" +
          "bru54WG3z2+Z0n6KP3m9mIHQZosBdYCnvKilKotO2SgUqa7B7pPDV7XPynO5\n" +
          "Cprl2CHixIzZ9R50jGkR7q8H4BGWBXXfm8kap0/Yy/rICs6nYAhSAPN6CNny\n" +
          "FpirPawL7iRzkMalvMhrCotJRGiB+qOPPhhFkQARAQAB/gkDCGfXhgmvVIIh\n" +
          "YCzHEZSujH8lhiL+4rbr+u2Z7ZhLq1K545Xv5FNPB3GWX1OMwlurkyw8mVvO\n" +
          "gTMzzcr85tP4yaaknlt7CbvciDo6qBTYqdF4SsNJnZ46zbecb4dcPUU/Xbua\n" +
          "RhAQvVwkpX+uBVEKsSme353NCHAmfAD/iZtqIoh8A4LEgpIArPuyXlotT3LW\n" +
          "093NEa/1N9WjP/OtFfEn5P0afCGXMK8ZOvAb8559WT5XyAUewesC37gwfaXO\n" +
          "rAedOrTkxtAZn6bh6GXZ2SbXxvR/G27L8/sizWMJMIZ7V/kVDk5s6COqxVRd\n" +
          "1kK3JZ2xcZO/kE+oH6RFtKKEATy7fm+HIy9g9z4/Gc9TeOu1WzwnRkXVXMMz\n" +
          "Wg2ks6SnhEB8vnzaeQxN74o4Y/qV56OFHy1jaKed/jaLMIdSRCxYm2o59Jj6\n" +
          "HvBMQg9yR4Ibub/7E76u1X2BqYgkRVn8Z5TdXpwrbrNFleRNHgzu9pk98r+l\n" +
          "4NqwLK0jXQ9LU9NWIktrrNl5FbvwiREVcFJP5dPgXXXh4gjLxbEqaDp/xg7x\n" +
          "YnfjuEC/lonnKl3ej8IdzyiizcYCu2Ic1/oVVMiLscp5/+uL8Q/BdLic6+j4\n" +
          "Cx+UljHTR3Bci9iI0v+hCVub6Bcz/GyXHDoLzMhjN4VK8UVBjf155UuB9a/m\n" +
          "hJ8XzAXld6ObUGOqV9YtiHrhhPChJCgh4M2nLHW12oCuS5Eu5y3aQO4jLA/D\n" +
          "SlLHZe8Gzmv0zAv9jldoIz5l86Yoao2BaGmyL2QIlQUoE8+fOwVCcf61nLjN\n" +
          "gVhdiL/8JybxNZ18dejJVFUaYP8VdcT7bpg04X5nLe2GmSG4T3DFXtF6NIgT\n" +
          "jSdHnheqDSjB1pQXkS/VjRXGZVyHSMP9RVrNMVdy2KhMcEWw/Ci97ORlt66N\n" +
          "iSI+D8a+l6TNajX6XkZg+Mm7tX6Aa6ecdgkMndogFqISZC+Mcumzn8ftBL9l\n" +
          "0sW/dnio3JK9Bv5rNo5AB4MUGJTun2Cy14yPkEzfYpyC0KiYWfnK/Hjplp36\n" +
          "wwJ/944Q7VRJc4RZfjC0nb5sgfnh4ynYSyxauMhziZlai+FOCkuYOLWNHx1d\n" +
          "S5TTm9AthQsTPBH7o7r41/ujV53XSgpEaFUFTB8KUd9FREbEUSxT7j6RmO0r\n" +
          "jilWBepNPjPnQBgu9PnfQl2TsUor7r6pMBrpQidRSr5bWRVCi7zj/+CPaXaY\n" +
          "r99DIOhEGVIXhlNSOBO1bCHKgMt4lRsKTF0sWcyf7P1wVriSl5prU1ffpk9t\n" +
          "yoNGIIEpEw6J8B6VHBoi6WQr/zvqSYAmLwMZgoK7p4HDV87yQvbUhdiAxlT4\n" +
          "w0zLy0bYUJ5trfUYeLt40eppMed9iJj+BaXyxxXWiIcE12v9TkmIHGwzgzst\n" +
          "RGaSU4Q3utGLuqEhh8HvKlrhSv7iQtAdbJ299iVk2fLUPG73OhzI1ESHYBsb\n" +
          "VTYnWZTvuFy+m/Odxma5FOI8e/Zd85+FNwpPHBrbImexLqDxXeArmCoIItiX\n" +
          "bleAWDh+Qx0m7akPcPSYtXWAlQjm/TdGGpaNBvfcEh6GNZOqEHuEIpspxlNM\n" +
          "FN0HDP9WKZY06WYdIuEt9slJEhifICpt6X5ZD0MyA8904C6pf+Dt4w4DNZQW\n" +
          "aChVC6XADH5/mBOBssQF1rqfgC/JvWsci9oWo551uJqgDg8WqqXZr9WRLZ9n\n" +
          "rSPFtx40TrTyuXcJDpi9A84/6usTXcye7NBbdIq7h0enygBtnw20i6G7a9YR\n" +
          "XMx/Y4jSatIoL8urrjTs9QAvzO3NEXVzciA8dXNyQHVzci5jb20+wsF1BBAB\n" +
          "CAApBQJcBapGBgsJBwgDAgkQfDB+byCSli0EFQgKAgMWAgECGQECGwMCHgEA\n" +
          "AMFnD/9S9stZYoeWjGfcahaVY+ke+412exJWpjT6JDLMdrZBingzFhduJ51c\n" +
          "SqWnnvKnj2d3imDAIsiAaEenaEenIr9JJ5XlYrqBTLJtrU3MR1cElAcTBOfz\n" +
          "ycUnD2AY05dNkmSENiu0IkMciazVaVChrJjVfH82uv2mb9NKZhofcNqV2S0c\n" +
          "8LGrsKgpqgeudmIjWqYLbYn3b+mwNOBuTYR/JZTqrIhf+s696IhFV0GmJd+U\n" +
          "Z25/3z3ohmqyz8HB0TIfSsIbp3G7EhzLcbrbNaZh/lfjIzPepD19Y6k5EEYI\n" +
          "hi8CM2I9xOadRo2lrWhXP+MYvWLvZtJifgM9M+IDdj2ITVCXgKdkpBD+E2o+\n" +
          "2dIE5dK/eMoJy2z5iwxygrbLltmEW0b3jddsoavJXwzcnulgfZP2Dq9jjg0a\n" +
          "1VEx18OIwU1JcjmCfMLNDQmMo8R+mesZ8It6la+F5OnO8Dw8lKQcczxdXPGP\n" +
          "vz4popFFaDpeLTOk0rT6wlE1/oZ2QNP1Ovh19zCp513Ey31JAeU2yxPiEsHq\n" +
          "J/SkQjc8ps9J75S4LC3wS6HUGx+4VFt3cr2ok3muzGreesugjBsQqxdua/WT\n" +
          "NfA2cV6i0UDgJ0PN/rBni37Mo8xhD9ONN34Ad6msTyBszMEHVMLCPWgvZ86I\n" +
          "zCYisg1TYhNMqx8reZf9Iquy5SZdzsfGhgRcBapEARAAr9225iiC41zRrHDY\n" +
          "cqXIGwz2HVRYtnVJWJiuyC5hlSYbrejnVxCfl7/b6F4LQhx4xVWCxo82/9oc\n" +
          "HvyFaewoNRH2xdxMWdx3IWJfHs1pqGSBcTV0L/nA7riTmAG/84hY1p7TaM2C\n" +
          "KO2NzzpSQapUY5iFG6IG1o196BSzRSHDD/o3jOhgKYfg8V1FdCpPZ6z/y5Zb\n" +
          "Wj1CRux054/3lnBsZHUXJ+S0VjSdRimJBamjSjWQSoS7H99P0bJ9TTS+oTyD\n" +
          "Sdocs0t7M/Q6+8NsTo8xWZKaHdxZcRB2Uv373OMoXqN7AA2oQmATYAvuQJhB\n" +
          "XDtAzXCbC1YkMy2SGp4fup70El3F9EvGgKLUHBgtvSqMpirrETxT99nlmLsE\n" +
          "kwY4xPlwAhWMJS7POJzvYYtWeky35TvpSfUXiwEf6MN+kdUzBDTn3Zdb7p/N\n" +
          "VLp7uZgP+netg4oP1cMZV0IOtb1uzbXhPR/9ZbOqifur7j0G2b1eD8+Pfn84\n" +
          "gMwm0nybn3OpAi2RXVdZpPrJlOt7bKCUxmfMimuP4o/PhOc2aUYo+DxdkCyS\n" +
          "LptiSed9/xi9I0ppyukk9jt8MXLSkhQgENFcioUDsQJG8wiOkm1C27lWMjwz\n" +
          "jzRp+EWHcxjcjQgwinJif3gps8U2Bg+Mkuws9WxkQjR4UjosmuXgI23eJpW8\n" +
          "RFn1G6MAEQEAAf4JAwj5olwqnpz1AGBabb4N9PPYszUi42U0dYPP22yfNfWh\n" +
          "R9hBSz+jvIujHsJJyksOSQMCFYVZ0QX5MTjBkjtjs1PV7FsgOJSRILY51WpP\n" +
          "gDvhnUhyHRSrph0l7cgbyezxSfayIntIymfN2BfTBCYHv5y6TIzocCZDXOgI\n" +
          "GhjLfPVUe5Rc0QeOnk13eYiWTOPI+LyQi/mG27BbdZez4nyubH9scsgjY69v\n" +
          "rne42F7e4+Bo8L8Pc5k2ctcabZrmhMbIEH8+EKub7LXqSylS+FnpZCbsICGt\n" +
          "bL/ZOP4X7LMAOmLKVLonqr3h5ihpcsIU8MbME5IZSI6qSGWf4/Sly93+Zw7+\n" +
          "VetmH83yTkVPfM6ah5XU5HiY7H3LZ/4DeRoIqRC4S+Ym4tef5+F2lGLGTSgx\n" +
          "PtqBOwFrpVn3ary0ecfOQQDQKvWwkj3vYURUH5ze5o+zcgMgXe0K+EGVXzm4\n" +
          "JMsYReE4UG3LRdHv2QME0bd/okRwpp4TA2gxC9IaQ76u1ZDTdEUk/zXjgQ/S\n" +
          "B87+wH6N6FUgO9ER8Jj7L1epwECXYSaKV6P+rO5rre1R1NhqQ8keI36Fz/Vy\n" +
          "eXBB+haxSvVKkGcnWdGWJ5vbDBsBhcZfT1fF+NN5l5a5g6qgms6s+0bpvTmd\n" +
          "rPVp03goqRXKgH/gb05X4xzOtBGZrKR732CtpuODXtpfvleuJKroSpm5BbhJ\n" +
          "g6JZyyGn0Rnvj+TSCBarBkLGecRMdyAvXeLZUOtapW5wO0V4JJqi3P8mmr1n\n" +
          "sNVYzjmVCkx6qTrn3M1wMbUznci1oZuSzy0COukF6qTYyGiKe5yn2D0Ue/jj\n" +
          "jsaJjHY5mgX/ZEjgN+qDDxW7ANt+sGWnJZqvRcq47YJSrbGyPcdg0gaYRm7v\n" +
          "gIXvEEhZy5YNmVDxTyL2qPQzsbhJAi62PbUWgnvovbLnwYwSbf1o9MtCTVF+\n" +
          "MdR88iUQW253elP5uF9oMUaZUzgbyr4/RCcRMpb3kumbBTJDRDjE44o6rEnl\n" +
          "JkXK3itvOqfrfM349NVnub46u811tjwws/3yw1nxOPN2xG6vErPNftHihu4H\n" +
          "UL0X5/w3h1qqjIc/cCihprfOwREIT6neV11X4Q68F1rJIOwV6sx5ke8G3ius\n" +
          "kvpncAY0SygzjNwMbE5Up11lNF+MNu3mB6oxIq8q3SIc7ki97enCGnJIknuy\n" +
          "/wkgZPyFoSBuBnyc5hTcM27LTxFHzMuholkHdTdRaJcPTddhvLb4fsxcljxt\n" +
          "OiR51QL+EwtZvIa5RdjFYitLgS0yeW3dJ20f48X4D4MAEjOdqVRj4YLCU9qw\n" +
          "r0DPI7jYab582cmlILTk1X/GnYCp2x1AHHzzXanVc5O90YOa4cOCn4lTFrnh\n" +
          "2bM4eURX9N4sw+QCKz2X/BNZToM7uVcuRHbF0DhFVly8Gfh5jAtEwbq3n79n\n" +
          "SWXrXYD+121hqXCvyGa46GI1wS6fTJR3RlOojIh/e0WktuTzvvC0TQUzdX4H\n" +
          "Ei0PY95JWvH1TdtXYvfzJRWvckcQBTJevPX52w475uwpsyF1hc0U77R6IaGV\n" +
          "+aW9eE9bTlFfUYtiGmvGe60M90r82QASn6k4w5vuEydNUk07Mr6ZSWlNSbD6\n" +
          "p5Th5Oi9NxOb4/gR6JTvekF28CYyqTU2dU8j8/JMIrUi8MIKYdNCpqr5pBFs\n" +
          "aVRZqoG7+mmVvlv/I9NgvzK3mvt007qPLRmaBZNifkZwKk66DDy2WeOqn0yB\n" +
          "JAkiG6/pLuN6IqNoDKUuK0rJx0yCuWenQKqIpDX748uHl9DEAOrl1ucVwsFf\n" +
          "BBgBCAATBQJcBapICRB8MH5vIJKWLQIbDAAAKr4QAK+czw5qcXxB7z7KuHXs\n" +
          "MQcEOInm/FIvT4mDeDJFUeg8/bM6gbYGQ6IzRUcKlLHCTAHrN1oGjA7wauL3\n" +
          "C9xhNRAtZWnkNMzG28GXJCuz6FWBZz4sfHSzQZ406SG1ewORw9G7OuOW5ynF\n" +
          "3CD+S1FLxIhqO4ZyMc1ZnmYouwdnP/ZnAVdRCSgovFws/dZH5kPPuOHYYzME\n" +
          "PYNS0dQOBOXtDPmDUSQfN3byqNFOojdujgX69do7mCFeYivI9p+wHbPljkZi\n" +
          "tlvoBcLtE9gdrnQxN9XiZlVPU1DiFmu/QWf+oeHUhlIjxnt8+Zu8nnCQ0PWf\n" +
          "QXK44JMqJ8OOTxjdc20p84MOvLE4vTXK7Dl7ErtbOywIc66CTDrUbzf9zeNX\n" +
          "qIsKAHX1i0PZzaUxlo9vIW4SrX9QfiuLvSh3LZUbgfgD4LlXmietMweKTN9Q\n" +
          "sZ+4th8IS56W64vGArDIXOd4RqZMX4l92ZshN216ZSN7NbmOslTaQwFHyX21\n" +
          "UM5TKUuMaiWqYQf9pXzsqTda+OOv7bc4u71+kt375jrZ1DwuySuyNRKCvvqw\n" +
          "sq3nqQb7hUO9kXpC2iBY+cVXMyrrnjbppaRr1rH166iHoIICXHQRQRsZqEro\n" +
          "J2d27nYWExZmqJdkCg2RagBXiSkJOu9Hs7F0DEV/wHjOE7Z9mSIyLZiKaO5l\n" +
          "Dn6h\n" +
          "=5aR+\n" +
          "-----END PGP PRIVATE KEY BLOCK-----\n",
      decryptedPrivateKey = "\n",
      passphrase = "some long pp",
      longid = "7C307E6F2092962D"
    ),
    "ecc" to TestKey(
      publicKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
          "Version: FlowCrypt Email Encryption 6.3.5\n" +
          "Comment: Seamlessly send and receive encrypted email\n" +
          "\n" +
          "xjMEXAZt6RYJKwYBBAHaRw8BAQdAHk2PLEMfkVLjxI6Vdg+dnJ5ElKcAX78x\n" +
          "P+GVCYDZyfLNEXVzciA8dXNyQHVzci5jb20+wncEEBYKACkFAlwGbekGCwkH\n" +
          "CAMCCRAGNjWz4z6xTAQVCAoCAxYCAQIZAQIbAwIeAQAA5H0A/3J+MZijs58O\n" +
          "o18O5vY33swAREm78aQLAUi9JWMkxdYOAQD2Cl58wQDDoyx2fgmS9NQOSON+\n" +
          "TCaGfIaPldt923KqD844BFwGbekSCisGAQQBl1UBBQEBB0BqkLKrGBakm/MV\n" +
          "NicvptKH4c7UdikdbpHPlfg2srb/dQMBCAfCYQQYFggAEwUCXAZt6QkQBjY1\n" +
          "s+M+sUwCGwwAAJQrAP4xAV2NYRnB8CcllBYvHeOkXE3K4qNQRHmFF+mEhcZ6\n" +
          "pQD/TCpMKlsFZCVzCaXyOohESrVD+UM7f/1A9QsqKh7Zmgw=\n" +
          "=WZgv\n" +
          "-----END PGP PUBLIC KEY BLOCK-----\n" +
          "\n",
      privateKey = "-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
          "Version: FlowCrypt Email Encryption 6.3.5\n" +
          "Comment: Seamlessly send and receive encrypted email\n" +
          "\n" +
          "xYYEXAZt6RYJKwYBBAHaRw8BAQdAHk2PLEMfkVLjxI6Vdg+dnJ5ElKcAX78x\n" +
          "P+GVCYDZyfL+CQMI1riV1EDicFNg4/f/0U/ZJZ9udC0F7GvtFKagL3EIqz6f\n" +
          "m+bm2E5qdDdyM2Z/7U2YOOVPc/HBxTg9SHrCTAYmfLtXEwU21uRzKIW9Y6N0\n" +
          "Ls0RdXNyIDx1c3JAdXNyLmNvbT7CdwQQFgoAKQUCXAZt6QYLCQcIAwIJEAY2\n" +
          "NbPjPrFMBBUICgIDFgIBAhkBAhsDAh4BAADkfQD/cn4xmKOznw6jXw7m9jfe\n" +
          "zABESbvxpAsBSL0lYyTF1g4BAPYKXnzBAMOjLHZ+CZL01A5I435MJoZ8ho+V\n" +
          "233bcqoPx4sEXAZt6RIKKwYBBAGXVQEFAQEHQGqQsqsYFqSb8xU2Jy+m0ofh\n" +
          "ztR2KR1ukc+V+Daytv91AwEIB/4JAwhPqxwBR+9JFWD07K5gQ/ahdz6fd7jf\n" +
          "piGAGZfJc3qN/W9MTqZcsl0qIiM4IaMeAuqlqm5xVHSHA3r7SnyfGtzDURM+\n" +
          "c9pzQRYLwp33TgHXwmEEGBYIABMFAlwGbekJEAY2NbPjPrFMAhsMAACUKwD+\n" +
          "MQFdjWEZwfAnJZQWLx3jpFxNyuKjUER5hRfphIXGeqUA/0wqTCpbBWQlcwml\n" +
          "8jqIREq1Q/lDO3/9QPULKioe2ZoM\n" +
          "=8qZ6\n" +
          "-----END PGP PRIVATE KEY BLOCK-----\n",
      decryptedPrivateKey = "\n",
      passphrase = "some long pp",
      longid = "063635B3E33EB14C"
    ),
    "gpg-dummy" to TestKey(
      publicKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
          "\n" +
          "mQGNBF1gO6wBDACy3MHo3fjP4Npnf0zfrr4b4utxjchrPoWX7Be08RpKyZgzH2o/\n" +
          "GVrkMD0nXWcJR/xAH9eI5QyedZHJxb3ukTH0sgSlSxiF2imXwJGFqmDXof5VOmtm\n" +
          "MGHpSu2d3cpM4Dy+nr44WZ2QxfLkRDDKNbkYRlOQLnPmNnDu7Bb4tAYfHsaAcNIz\n" +
          "i6hyBLsvZqeSpocUMD4E6/pmCNFxpYZ4ORitbhiffrYRC4uL1ZmsghgBhoFvV4Jk\n" +
          "Frulh8I0ojkS8Q7EkxmwIF/CTCgR+K4M0o2lw4DbYKUUd8DIFfbqli4WKCTFExES\n" +
          "ycdyB3pwsALSH1K6N0RJcRIAZ5Nw3vbIuTAA2LWoHSLvOHu1qGYFh2+bsYGMsJTF\n" +
          "82oa8i5b7XXzUjcWTi4kzWbGLqml+xXzO+3rAjWWO3R2lj2UE8zKKCwstOyGF3na\n" +
          "93+Ffa2czgg/C1ui6T8HiI68N4GMXitR8EcVjlr9w9EHo+UfUZluCybZLT3csh0i\n" +
          "cvWrIWNwnIzczEsAEQEAAbQxRHVtbXkgZ3BnIHByaW1hcnkga2V5IDxkdW1teS5n\n" +
          "cGcucHJpbWFyeUBrZXkuY29tPokBzgQTAQoAOAIbAwULCQgHAgYVCgkICwIEFgID\n" +
          "AQIeAQIXgBYhBB35P+4klUalHDvyAJb7PJZhpVc8BQJdYDwCAAoJEJb7PJZhpVc8\n" +
          "P9gL/AyH8VWhjwh9NPLfe2jNu4iMvV1aQy9iA9DWpP8D5lNbpZk0kDxZ/Nve7Px2\n" +
          "5eRrcmNAnHjC+eoJ4/pzEWDoVJnMbIioNwx8C5i+Abvl0wlEeEyMYXH5DMT0qpqQ\n" +
          "Y9Zlds1SCmSGmA3DYqt30RIutIw3oJisJyIK2i0O6x/bDSKecjORp/5T+cdC4kxa\n" +
          "Oe2rrc/9H7rZqZ/5x+EzQsTcHsk7QrpIAJCZMH66OiR2I1+msrdDJEVIDpIMeYe+\n" +
          "gPt/QZEj2uBHq0M46Fmsb7BEQ68mIQDvIAVDsHlb0tiWtOd8ux0X0P2d51eEnx4C\n" +
          "A2bfZezZvAgaBwds/uaPfV7/FpIFY44jylD7oCS9mn6cJ8e1egGTKb2yxgD/w+WG\n" +
          "fOz4GCtYxqDQHuHIkpxiwwvqrrpdgKYseS8sftowPY6uMLfJZkDNSzOKDetdIahf\n" +
          "Zh8uwHsRzIKQtcNRmzKynjUMRkBJlINGxsVyNTG6/r1XJm5QKZiptEbSeOptrOjx\n" +
          "e9p+ErkBjQRdYDusAQwApKPyhRgY4jWTQP5CKi6zJ8addl/uWu9k4Y9Y3L+nDm00\n" +
          "i1aumM/RXtXeLIwF+b4kM2dYEb9WNvrRvWLBzA7P8Yim6vvN+TRIQnXaE+YKojD2\n" +
          "myTdK6b7IALG6nJbMMEgNACs6poF21aKSEOsJg77ydxa9ModFGnvS7OyasXK3o2i\n" +
          "rCd120yb84LzII5mjONQdDWS0OySOOaNJoUc9JbDbj4lSuUOWYu0ygBdLsidZI4h\n" +
          "9u6piB1TrPeADXuh6yN9l4R2FbjlEkytZdgrvvfdRvoAJMSyLlcdeDpmyot9POiG\n" +
          "5kkxg5uQfWNquQEmz1ya/KsAKoYowV9VBXa2L+741XLf//5sL8/A2a4rPc37JYAi\n" +
          "RKeM89oJ374/EL202gSqINzKEKFpn31sCmGtV4ODHh9qFulpL26DvYNkdMsAZtws\n" +
          "nQSvp2Q6058UIM86JHBpboJ7o+q3M59RH//jpFo7+0oAnovHdXTQj5lXWNo1LzJP\n" +
          "AK9RovnZJhaLs5IS1O/XABEBAAGJAbYEGAEKACACGwwWIQQd+T/uJJVGpRw78gCW\n" +
          "+zyWYaVXPAUCXWA8KgAKCRCW+zyWYaVXPK+dDACcLBNT/hagfZR8162EZtCpgmKg\n" +
          "H15t7Etve2llt1OH8C3u/LzmFpSYGuNPyCmlJPEzKmsyIxMyizbWVjp3aKNTdZG+\n" +
          "P9zfAv5ao4WmZqAi0eN4jFHPUl5YHfbJBL4sPv43QEO/3mO2vFr5mSSKabPQtWfh\n" +
          "4fQiwt6FkHiXvOUar92piZLGqLKmBdeNnoCqR10AehsqHRSTrHWwNGQoiHe6Uj7u\n" +
          "4KBKonItRZAln2PQ7fPTA0mLAlFDmZKFRGv1MzFzj/Mb41KWS8L6XexJZPU09Weo\n" +
          "djpQREvjqhoHc+6KGOlu26274w8rH2e4BvSxhjJ8hKJsxv9URR5e/SEuvK0fSr+5\n" +
          "yR0nFvOumU9fQZVcvR0nH3XA6Hjpl1CqMSXTDdnIvL9YuTmvgTzM10qiB4HhwUFw\n" +
          "4EdrCsp8J7T9IUz9lW851wpDB7c4CKeDst+cgFcp9Fg/esHHzyyEukMdUkn777Uv\n" +
          "fga3xzGyrbBy00LYVylMDvs5GPYyCCi7Ch9cgvg=\n" +
          "=nkDv\n" +
          "-----END PGP PUBLIC KEY BLOCK-----\n",
      privateKey = "-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
          "\n" +
          "lQGVBF1gO6wBDACy3MHo3fjP4Npnf0zfrr4b4utxjchrPoWX7Be08RpKyZgzH2o/\n" +
          "GVrkMD0nXWcJR/xAH9eI5QyedZHJxb3ukTH0sgSlSxiF2imXwJGFqmDXof5VOmtm\n" +
          "MGHpSu2d3cpM4Dy+nr44WZ2QxfLkRDDKNbkYRlOQLnPmNnDu7Bb4tAYfHsaAcNIz\n" +
          "i6hyBLsvZqeSpocUMD4E6/pmCNFxpYZ4ORitbhiffrYRC4uL1ZmsghgBhoFvV4Jk\n" +
          "Frulh8I0ojkS8Q7EkxmwIF/CTCgR+K4M0o2lw4DbYKUUd8DIFfbqli4WKCTFExES\n" +
          "ycdyB3pwsALSH1K6N0RJcRIAZ5Nw3vbIuTAA2LWoHSLvOHu1qGYFh2+bsYGMsJTF\n" +
          "82oa8i5b7XXzUjcWTi4kzWbGLqml+xXzO+3rAjWWO3R2lj2UE8zKKCwstOyGF3na\n" +
          "93+Ffa2czgg/C1ui6T8HiI68N4GMXitR8EcVjlr9w9EHo+UfUZluCybZLT3csh0i\n" +
          "cvWrIWNwnIzczEsAEQEAAf8AZQBHTlUBtDFEdW1teSBncGcgcHJpbWFyeSBrZXkg\n" +
          "PGR1bW15LmdwZy5wcmltYXJ5QGtleS5jb20+iQHOBBMBCgA4AhsDBQsJCAcCBhUK\n" +
          "CQgLAgQWAgMBAh4BAheAFiEEHfk/7iSVRqUcO/IAlvs8lmGlVzwFAl1gPAIACgkQ\n" +
          "lvs8lmGlVzw/2Av8DIfxVaGPCH008t97aM27iIy9XVpDL2ID0Nak/wPmU1ulmTSQ\n" +
          "PFn8297s/Hbl5GtyY0CceML56gnj+nMRYOhUmcxsiKg3DHwLmL4Bu+XTCUR4TIxh\n" +
          "cfkMxPSqmpBj1mV2zVIKZIaYDcNiq3fREi60jDegmKwnIgraLQ7rH9sNIp5yM5Gn\n" +
          "/lP5x0LiTFo57autz/0futmpn/nH4TNCxNweyTtCukgAkJkwfro6JHYjX6ayt0Mk\n" +
          "RUgOkgx5h76A+39BkSPa4EerQzjoWaxvsERDryYhAO8gBUOweVvS2Ja053y7HRfQ\n" +
          "/Z3nV4SfHgIDZt9l7Nm8CBoHB2z+5o99Xv8WkgVjjiPKUPugJL2afpwnx7V6AZMp\n" +
          "vbLGAP/D5YZ87PgYK1jGoNAe4ciSnGLDC+quul2Apix5Lyx+2jA9jq4wt8lmQM1L\n" +
          "M4oN610hqF9mHy7AexHMgpC1w1GbMrKeNQxGQEmUg0bGxXI1Mbr+vVcmblApmKm0\n" +
          "RtJ46m2s6PF72n4SnQWGBF1gO6wBDACko/KFGBjiNZNA/kIqLrMnxp12X+5a72Th\n" +
          "j1jcv6cObTSLVq6Yz9Fe1d4sjAX5viQzZ1gRv1Y2+tG9YsHMDs/xiKbq+835NEhC\n" +
          "ddoT5gqiMPabJN0rpvsgAsbqclswwSA0AKzqmgXbVopIQ6wmDvvJ3Fr0yh0Uae9L\n" +
          "s7JqxcrejaKsJ3XbTJvzgvMgjmaM41B0NZLQ7JI45o0mhRz0lsNuPiVK5Q5Zi7TK\n" +
          "AF0uyJ1kjiH27qmIHVOs94ANe6HrI32XhHYVuOUSTK1l2Cu+991G+gAkxLIuVx14\n" +
          "OmbKi3086IbmSTGDm5B9Y2q5ASbPXJr8qwAqhijBX1UFdrYv7vjVct///mwvz8DZ\n" +
          "ris9zfslgCJEp4zz2gnfvj8QvbTaBKog3MoQoWmffWwKYa1Xg4MeH2oW6WkvboO9\n" +
          "g2R0ywBm3CydBK+nZDrTnxQgzzokcGlugnuj6rczn1Ef/+OkWjv7SgCei8d1dNCP\n" +
          "mVdY2jUvMk8Ar1Gi+dkmFouzkhLU79cAEQEAAf4HAwKPfgsWNH6FAP8aZS5717Q3\n" +
          "VvFX35lVZifBdpgRd+LLiK+r0uC8pZVy/TbSrbBlp4oyY5hs3iOG0v92wtGSF6MD\n" +
          "wUgPvzMep1f7OAHvvX48geLemKpEiUJ4NifjoyLZXcOu2tJr6KYOV3R64gPVcmdN\n" +
          "uH/6CPvWCuG7pi/Rp40Gpc6L1JCx2+iNWKkdvxezN8KJR20dTvLXco0BQ7cgGorz\n" +
          "YWa1g/Ut4LlFaAJ4frZM0t00s5++CO1EKXhn0q3qdFccFGeMmYjjOl9aaeY7FXOL\n" +
          "6fdavpJRY/NrdcT9EWkfbviqPyRMatwEwO2qw2vPQ67us8xFijYm9DbzR3Zt0OcV\n" +
          "fw6ib4ALRcXsic95jHmj+s32YhW77f7hqXUjjwnfSbSC+s5qC02DRJLcPM/7SvNQ\n" +
          "b2WFBxqOBJ2+G+kBoJ3eH7bs/K91FCLCta2QKg7R6Mj+trB6JXz5ar4FL7emD3T3\n" +
          "boZdOnZ5ERKdhsjQa5+f+BqD92sEmyZpFFmPL8ZemNOuO2T3PxF7uJ0fqFNoofwW\n" +
          "/PsGUcsbqaiLsJ98E0CSZhhCVC19CUrjeK1ox9K3EYiPOKmKZDspxZ29SMZ6Rt3Q\n" +
          "ZnlCE55KeSxDlToKHfMNCyusS5zJ1qG6wR1eRGrNLGJ0CRUkAHocr9V5BrQrC5wu\n" +
          "BOWqf6rANcSi1EkSB+/alktf/Sphb5xcQ4fLbmXoNfN6GCtCCZ8QuFu98PF/bHOI\n" +
          "vM8fJql2TAIqbnWk/9r1PKpUv5lP1yN0VPeSXW989YOV2ae+TPCSsaOd5jpOaXmF\n" +
          "D0p1odXXa5ESOXyzYJyTlJAXHMUJQtUMk5E4qj5UdwppygEI8wmhBJk+vlY7OuU4\n" +
          "r3NUjispyx1rVhxtiJkZh+LGWh/G/8y1lnXWwSLsR1nl2/oGa7969EGuI2ECBv9t\n" +
          "LctgwyJaRWi2w9ywsRt2n0ae+OJxOHyOqvld1UiIgzs0ysb7NU58Vs9SdRC5xPvS\n" +
          "0ZUEzfM97ap8nRZAzKJh9zhK2PW6qqHKBpfOINS3VeHTAQSmBEMoiflyIVv5gH0n\n" +
          "SGUhw1vi4MfNJuM2E6fBdEQVzKJr7b1Alet4dYpqQui6iVjIZnYfnAoPNci0kgzF\n" +
          "LZVawdM95se/BSJW4/Q1h8UHwJhF2zyyWW2u0qdq3ASfv5LF9nEMepD+ZIGOVrSb\n" +
          "lp8e8lLDiDUBySbrP3VUvxLmPamIXRlxkfSku8FlUTKdPrrKCU6edKJB0LbuJ/w7\n" +
          "QbNO2N935Vow6kemqxGZMr64mPg09TniQr+b7DIeHKu6RVd3n+2749H8O3+A3Alq\n" +
          "Hzd58XE3XscZXMQoztj9rc9L30XkdIr9vARQCKcTX8QU4ET1HIkBtgQYAQoAIAIb\n" +
          "DBYhBB35P+4klUalHDvyAJb7PJZhpVc8BQJdYDwqAAoJEJb7PJZhpVc8r50MAJws\n" +
          "E1P+FqB9lHzXrYRm0KmCYqAfXm3sS297aWW3U4fwLe78vOYWlJga40/IKaUk8TMq\n" +
          "azIjEzKLNtZWOndoo1N1kb4/3N8C/lqjhaZmoCLR43iMUc9SXlgd9skEviw+/jdA\n" +
          "Q7/eY7a8WvmZJIpps9C1Z+Hh9CLC3oWQeJe85Rqv3amJksaosqYF142egKpHXQB6\n" +
          "GyodFJOsdbA0ZCiId7pSPu7goEqici1FkCWfY9Dt89MDSYsCUUOZkoVEa/UzMXOP\n" +
          "8xvjUpZLwvpd7Elk9TT1Z6h2OlBES+OqGgdz7ooY6W7brbvjDysfZ7gG9LGGMnyE\n" +
          "omzG/1RFHl79IS68rR9Kv7nJHScW866ZT19BlVy9HScfdcDoeOmXUKoxJdMN2ci8\n" +
          "v1i5Oa+BPMzXSqIHgeHBQXDgR2sKynwntP0hTP2VbznXCkMHtzgIp4Oy35yAVyn0\n" +
          "WD96wcfPLIS6Qx1SSfvvtS9+BrfHMbKtsHLTQthXKUwO+zkY9jIIKLsKH1yC+A==\n" +
          "=R73C\n" +
          "-----END PGP PRIVATE KEY BLOCK-----\n",
      decryptedPrivateKey = "\n",
      passphrase = "FlowCrypt",
      longid = "96FB3C9661A5573C"
    ),
    "expired" to TestKey(
      publicKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----\r\n" +
          "Version: FlowCrypt Email Encryption 7.8.4\r\n" +
          "Comment: Seamlessly send and receive encrypted email\r\n" +
          "\r\n" +
          "xsBNBF8PcdUBCADi8no6T4Bd9Ny5COpbheBuPWEyDOedT2EVeaPrfutB1D8i\r\n" +
          "CP6Rf1cUvs/qNUX/O7HQHFpgFuW2uOY4OU5cvcrwmNpOxT3pPt2cavxJMdJo\r\n" +
          "fwEvloY3OfY7MCqdAj5VUcFGMhubfV810V2n5pf2FFUNTirksT6muhviMymy\r\n" +
          "uWZLdh0F4WxrXEon7k3y2dZ3mI4xsG+Djttb6hj3gNr8/zNQQnTmVjB0mmpO\r\n" +
          "FcGUQLTTTYMngvVMkz8/sh38trqkVGuf/M81gkbr1egnfKfGz/4NT3qQLjin\r\n" +
          "nA8In2cSFS/MipIV14gTfHQAICFIMsWuW/xkaXUqygvAnyFa2nAQdgELABEB\r\n" +
          "AAHNKDxhdXRvLnJlZnJlc2guZXhwaXJlZC5rZXlAcmVjaXBpZW50LmNvbT7C\r\n" +
          "wJMEEAEIACYFAl8PcdUFCQAAAAEGCwkHCAMCBBUICgIEFgIBAAIZAQIbAwIe\r\n" +
          "AQAhCRC+46QtmpyKyRYhBG0+CYZ1RO5ify6Sj77jpC2anIrJIvQIALG8TGMN\r\n" +
          "YB4CRouMJawNCLui6Fx4Ba1ipPTaqlJPybLoe6z/WVZwAA9CmbjkCIk683pp\r\n" +
          "mGQ3GXv7f8Sdk7DqhEhfZ7JtAK/Uw2VZqqIryNrrB0WV3EUHsENCOlq0YJod\r\n" +
          "Lqtkqgl83lCNDIkeoQwq4IyrgC8wsPgF7YMpxxQLONJvChZxSdCDjnfX3kvO\r\n" +
          "ZsLYFiKnNlX6wyrKAQxWnxxYhglMf0GDDyh0AJ+vOQHJ9m+oeBnA1tJ5AZU5\r\n" +
          "aQHvRtyWBKkYaEhljhyWr3eu1JjK4mn7/W6Rszveso33987wtIoQ66GpGcX2\r\n" +
          "mh7y217y/uXz4D3X5PUEBXIbhvAPty71bnTOwE0EXw9x1QEIALdJgAsQ0Jnv\r\n" +
          "LXwAKoOammWlUQmracK89v1Yc4mFnImtHDHS3pGsbx3DbNGuiz5BhXCdoPDf\r\n" +
          "gMxlGmJgShy9JAhrhWFXkvsjW/7aO4bM1wU486VPKXb7Av/dcrfHH0ASj4zj\r\n" +
          "/TYAeubNoxQtxHgyb13LVCW1kh4Oe6s0ac/hKtxogwEvNFY3x+4yfloHH0Ik\r\n" +
          "9sbLGk0gS03bPABDHMpYk346406f5TuP6UDzb9M90i2cFxbq26svyBzBZ0vY\r\n" +
          "zfMRuNsm6an0+B/wS6NLYBqsRyxwwCTdrhYS512yBzCHDYJJX0o3OJNe85/0\r\n" +
          "TqEBO1prgkh3QMfw13/Oxq8PuMsyJpUAEQEAAcLAfAQYAQgADwUCXw9x1QUJ\r\n" +
          "AAAAAQIbDAAhCRC+46QtmpyKyRYhBG0+CYZ1RO5ify6Sj77jpC2anIrJARgH\r\n" +
          "/1KV7JBOS2ZEtO95FrLYnIqI45rRpvT1XArpBPrYLuHtDBwgMcmpiMhhKIZC\r\n" +
          "FlZkR1W88ENdSkr8Nx81nW+f9JWRR6HuSyom7kOfS2Gdbfwo3bgp48DWr7K8\r\n" +
          "KV/HHGuqLqd8UfPyDpsBGNx0w7tRo+8vqUbhskquLAIahYCbhEIE8zgy0fBV\r\n" +
          "hXKFe1FjuFUoW29iEm0tZWX0k2PT5r1owEgDe0g/X1AXgSQyfPRFVDwE3QNJ\r\n" +
          "1np/Rmygq1C+DIW2cohJOc7tO4gbl11XolsfQ+FU+HewYXy8aAEbrTSRfsff\r\n" +
          "MvK6tgT9BZ3kzjOxT5ou2SdvTa0eUk8k+zv8OnJJfXA=\r\n" +
          "=LPeQ\r\n" +
          "-----END PGP PUBLIC KEY BLOCK-----\r\n" +
          "\n",
      privateKey = "\n",
      decryptedPrivateKey = "\n",
      passphrase = "",
      longid = ""
    ),
  )
}
