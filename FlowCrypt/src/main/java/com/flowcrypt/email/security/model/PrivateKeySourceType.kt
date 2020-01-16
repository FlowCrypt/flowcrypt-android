/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.model

/**
 * @author DenBond7
 * Date: 13.05.2017
 * Time: 15:09
 * E-mail: DenBond7@gmail.com
 */

enum class PrivateKeySourceType constructor(private val text: String) {
  BACKUP("backup"),
  NEW("new"),
  IMPORT("import");

  override fun toString(): String {
    return text
  }
}
