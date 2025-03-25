/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */
package com.flowcrypt.email.junit.suites

/**
 * @author Denys Bondarenko
 */
fun interface InsideTraversal<R> {
  @Throws(Exception::class)
  fun run(traversalClass: Class<*>?): R?
}
