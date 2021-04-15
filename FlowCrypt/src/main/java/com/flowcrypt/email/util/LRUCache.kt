/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.util

// Ideas are from here:
// http://chriswu.me/blog/a-lru-cache-in-10-lines-of-java/

class LRUCache<K, V>(private val cacheSize: Int) : LinkedHashMap<K, V>(16, 0.75F, true) {
  override fun removeEldestEntry(eldest: Map.Entry<K, V>?): Boolean {
    return size >= cacheSize
  }
}
