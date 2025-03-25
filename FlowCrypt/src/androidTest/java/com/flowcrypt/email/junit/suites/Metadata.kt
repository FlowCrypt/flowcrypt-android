/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.junit.suites

/**
 * @author Denys Bondarenko
 */
class Metadata(metadata: MutableMap<Class<*>?, Any?>) {
  private val metadata: MutableMap<Class<*>?, Any?> = java.util.HashMap<Class<*>?, Any?>(metadata)

  fun <T> get(metadataClass: Class<T?>): T? {
    return metadataClass.cast(metadata.get(metadataClass))
  }
}