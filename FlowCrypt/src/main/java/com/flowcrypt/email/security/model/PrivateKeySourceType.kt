/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.model

/**
 * @author Denys Bondarenko
 */
enum class PrivateKeySourceType constructor(private val text: String) {
  BACKUP("backup"),
  NEW("new"),
  IMPORT("import");

  override fun toString(): String {
    return text
  }
}
