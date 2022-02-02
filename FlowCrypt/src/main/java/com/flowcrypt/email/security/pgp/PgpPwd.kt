/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.Constants
import com.flowcrypt.email.extensions.org.pgpainless.util.asString
import com.flowcrypt.email.util.exception.PrivateKeyStrengthException
import com.nulabinc.zxcvbn.Zxcvbn
import org.pgpainless.util.Passphrase
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

object PgpPwd {
  data class Word(
    val match: String,
    val word: String,
    val bar: Long,
    var color: String,
    val pass: Boolean
  )

  data class PwdStrengthResult(
    val word: Word,
    val seconds: BigInteger,
    val time: String
  )

  enum class PwdType {
    PASSPHRASE,
    PASSWORD
  }

  /**
   * Check if the given passphrase weak.
   * @throws [PrivateKeyStrengthException] if the given passphrase is weak
   * @throws [IllegalArgumentException] if missing passphrase strength evaluation
   */
  fun checkForWeakPassphrase(passphrase: Passphrase) {
    val measure = Zxcvbn().measure(
      passphrase.asString ?: "",
      listOf(*Constants.PASSWORD_WEAK_WORDS)
    ).guesses
    val passwordStrength = estimateStrength(measure)

    when (passwordStrength.word.word) {
      Constants.PASSWORD_QUALITY_WEAK,
      Constants.PASSWORD_QUALITY_POOR -> throw PrivateKeyStrengthException("Pass phrase too weak")

      Constants.PASSWORD_QUALITY_REASONABLE,
      Constants.PASSWORD_QUALITY_GOOD,
      Constants.PASSWORD_QUALITY_GREAT,
      Constants.PASSWORD_QUALITY_PERFECT -> {
        //everything looks good
      }

      else -> throw IllegalArgumentException("Missed passphrase strength evaluation found")
    }
  }

  fun estimateStrength(guesses: Double, type: PwdType = PwdType.PASSPHRASE): PwdStrengthResult {
    return estimateStrength(guesses.toBigDecimal(), type)
  }

  fun estimateStrength(guesses: BigDecimal, type: PwdType = PwdType.PASSPHRASE): PwdStrengthResult {
    val timeToCrack = guesses.divide(CRACK_GUESSES_PER_SECOND)
    val readableTime = readableCrackTime(timeToCrack)
    val words = when (type) {
      PwdType.PASSPHRASE -> CRACK_TIME_WORDS_PASSPHRASE
      PwdType.PASSWORD -> CRACK_TIME_WORDS_PASSWORD
    }
    for (word in words) {
      if (readableTime.contains(word.match)) {
        return PwdStrengthResult(word, timeToCrack.toBigInteger(), readableTime)
      }
    }
    throw IllegalArgumentException("Can't estimate strength for the number of guesses $guesses")
  }

  /**
   * Generates random password using digits and uppercase English letters, for example:
   * TDW6-DU5M-TANI-LJXY
   */
  fun random(): String {
    val bytes = ByteArray(16)
    val rnd = SecureRandom()
    rnd.nextBytes(bytes)
    return bytesToPassword(bytes)
  }

  fun bytesToPassword(bytes: ByteArray): String {
    val minLength = 16
    if (bytes.size < minLength) {
      throw IllegalArgumentException(
        "Source byte array is too short: required minimum length is $minLength, " +
            "but the actual length is ${bytes.size}"
      )
    }
    val s = StringBuilder()
    bytes.forEachIndexed { i, b0 ->
      if (i > 0 && i % 4 == 0) s.append('-')
      var b = b0 % 36
      if (b < 0) b += 36
      s.append(if (b < 10) '0' + b else 'A' + (b - 10))
    }
    return s.toString()
  }

  // https://stackoverflow.com/questions/8211744/convert-time-interval-given-in-seconds-into-more-human-readable-form
  private fun readableCrackTime(totalSeconds: BigDecimal): String {
    val millennia = totalSeconds.divide(SECONDS_PER_MILLENNIUM, 0, RoundingMode.HALF_UP)
    if (millennia > BigDecimal.ZERO) {
      return if (millennia == BigDecimal.ONE) "a millennium" else "millennia"
    }

    val centuries = totalSeconds.divide(SECONDS_PER_CENTURY, 0, RoundingMode.HALF_UP)
    if (centuries > BigDecimal.ZERO) {
      return if (centuries == BigDecimal.ONE) "a century" else "centuries"
    }

    val years = totalSeconds.divide(SECONDS_PER_YEAR, 0, RoundingMode.HALF_UP)
    if (years > BigDecimal.ZERO) {
      return "$years year${numberWordEnding(years)}"
    }

    val months = totalSeconds.divide(SECONDS_PER_MONTH, 0, RoundingMode.HALF_UP)
    if (months > BigDecimal.ZERO) {
      return "$months month${numberWordEnding(months)}"
    }

    val weeks = totalSeconds.divide(SECONDS_PER_WEEK, 0, RoundingMode.HALF_UP)
    if (weeks > BigDecimal.ZERO) {
      return "$weeks week${numberWordEnding(weeks)}"
    }

    val days = totalSeconds.divide(SECONDS_PER_DAY, 0, RoundingMode.HALF_UP)
    if (days > BigDecimal.ZERO) {
      return "$days day${numberWordEnding(days)}"
    }

    val hours = totalSeconds.divide(SECONDS_PER_HOUR, 0, RoundingMode.HALF_UP)
    if (hours > BigDecimal.ZERO) {
      return "$hours hour${numberWordEnding(hours)}"
    }

    val minutes = totalSeconds.divide(SECONDS_PER_MINUTE, 0, RoundingMode.HALF_UP)
    if (minutes > BigDecimal.ZERO) {
      return "$minutes minute${numberWordEnding(minutes)}"
    }

    val seconds = totalSeconds.divide(SECONDS_PER_SECONDS, 0, RoundingMode.HALF_UP)
    if (seconds > BigDecimal.ZERO) {
      return "$seconds second${numberWordEnding(seconds)}"
    }

    return "less than a second"
  }

  private fun numberWordEnding(n: BigDecimal): String {
    return if (n > BigDecimal.ONE) "s" else ""
  }

  // (10k pc)*(2 core p/pc)*(4k guess p/core)
  // https://www.abuse.ch/?p=3294
  // https://threatpost.com/how-much-does-botnet-cost-022813/77573/
  // https://www.abuse.ch/?p=3294
  private val CRACK_GUESSES_PER_SECOND = BigDecimal.valueOf(10000 * 2 * 4000)

  private val SECONDS_PER_MILLENNIUM = TimeUnit.DAYS.toSeconds(365 * 100 * 1000).toBigDecimal()
  private val SECONDS_PER_CENTURY = TimeUnit.DAYS.toSeconds(365 * 100).toBigDecimal()
  private val SECONDS_PER_YEAR = TimeUnit.DAYS.toSeconds(365).toBigDecimal()
  private val SECONDS_PER_MONTH = TimeUnit.DAYS.toSeconds(30).toBigDecimal()
  private val SECONDS_PER_WEEK = TimeUnit.DAYS.toSeconds(7).toBigDecimal()
  private val SECONDS_PER_DAY = TimeUnit.DAYS.toSeconds(1).toBigDecimal()
  private val SECONDS_PER_HOUR = TimeUnit.HOURS.toSeconds(1).toBigDecimal()
  private val SECONDS_PER_MINUTE = TimeUnit.MINUTES.toSeconds(1).toBigDecimal()
  private val SECONDS_PER_SECONDS = TimeUnit.SECONDS.toSeconds(1).toBigDecimal()

  private val CRACK_TIME_WORDS_PASSWORD = arrayOf(
    // the requirements for a one-time password are less strict
    Word(match = "millenni", word = "perfect", bar = 100, color = "green", pass = true),
    Word(match = "centu", word = "perfect", bar = 95, color = "green", pass = true),
    Word(match = "year", word = "great", bar = 80, color = "orange", pass = true),
    Word(match = "month", word = "good", bar = 70, color = "darkorange", pass = true),
    Word(match = "week", word = "good", bar = 30, color = "darkred", pass = true),
    Word(match = "day", word = "reasonable", bar = 40, color = "darkorange", pass = true),
    Word(match = "hour", word = "bare minimum", bar = 20, color = "darkred", pass = true),
    Word(match = "minute", word = "poor", bar = 15, color = "red", pass = false),
    Word(match = "", word = "weak", bar = 10, color = "red", pass = false)
  )

  private val CRACK_TIME_WORDS_PASSPHRASE = arrayOf(
    // the requirements for a pass phrase are meant to be strict
    Word(match = "millenni", word = "perfect", bar = 100, color = "green", pass = true),
    Word(match = "centu", word = "great", bar = 80, color = "green", pass = true),
    Word(match = "year", word = "good", bar = 60, color = "orange", pass = true),
    Word(match = "month", word = "reasonable", bar = 40, color = "darkorange", pass = true),
    Word(match = "week", word = "poor", bar = 30, color = "darkred", pass = false),
    Word(match = "day", word = "poor", bar = 20, color = "darkred", pass = false),
    Word(match = "", word = "weak", bar = 10, color = "red", pass = false)
  )

  @Suppress("unused")
  val weakWords = listOf(
    "crypt", "up", "cryptup", "flow", "flowcrypt", "encryption", "pgp", "email", "set",
    "backup", "passphrase", "best", "pass", "phrases", "are", "long", "and", "have",
    "several", "words", "in", "them", "Best pass phrases are long", "have several words",
    "in them", "bestpassphrasesarelong", "haveseveralwords", "inthem",
    "Loss of this pass phrase", "cannot be recovered", "Note it down", "on a paper",
    "lossofthispassphrase", "cannotberecovered", "noteitdown", "onapaper", "setpassword",
    "set password", "set pass word", "setpassphrase", "set pass phrase", "set passphrase"
  )
}
