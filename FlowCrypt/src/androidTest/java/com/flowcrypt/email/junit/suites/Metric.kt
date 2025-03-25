/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.junit.suites

/**
 * @author Denys Bondarenko
 */
class Metric(val name: String?, var count: Int, elapsedNs: Int, val isSuccess: Boolean) {
  var elapsedNs: Long
    private set
  var minNs: Long = 0
    private set
  var maxNs: Long = 0
    private set

  init {
    this.elapsedNs = elapsedNs.toLong()
  }

  constructor(name: String?, success: Boolean) : this(name, 0, 0, success)

  fun record(elapsedNs: Long) {
    if (count == 0 || elapsedNs < minNs) {
      minNs = elapsedNs
    }

    if (elapsedNs > maxNs) {
      maxNs = elapsedNs
    }

    this.elapsedNs += elapsedNs

    count++
  }

  fun incrementCount() {
    this.count++
  }

  override fun equals(o: Any?): Boolean {
    if (this === o) {
      return true
    }
    if (o !is Metric) {
      return false
    }

    val metric = o

    if (this.isSuccess != metric.isSuccess) {
      return false
    }
    return if (name != null) (name == metric.name) else metric.name == null
  }

  override fun hashCode(): Int {
    var result = name?.hashCode() ?: 0
    result = 31 * result + (if (this.isSuccess) 1 else 0)
    return result
  }

  override fun toString(): String {
    return ("Metric{"
        + "name='"
        + name
        + '\''
        + ", count="
        + count
        + ", minNs="
        + minNs
        + ", maxNs="
        + maxNs
        + ", elapsedNs="
        + elapsedNs
        + ", success="
        + this.isSuccess
        + '}')
  }
}