/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.coroutines.runners

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A helper class to execute tasks sequentially in coroutines.
 *
 * Calling [afterPrevious] will always ensure that all previously requested work completes prior to
 * calling the block passed. Any future calls to [afterPrevious] while the current block is running
 * will wait for the current block to complete before starting.
 *
 * See https://gist.github.com/objcode/7ab4e7b1df8acd88696cb0ccecad16f7#file-concurrencyhelpers-kt
 */

/**
 * @author Denys Bondarenko
 */
class SingleRunner {
  /**
   * A coroutine mutex implements a lock that may only be taken by one coroutine at a time.
   */
  private val mutex = Mutex()

  /**
   * Ensure that the block will only be executed after all previous work has completed.
   *
   * When several coroutines call afterPrevious at the same time, they will queue up in the order
   * that they call afterPrevious. Then, one coroutine will enter the block at a time.
   *
   * In the following example, only one save operation (user or song) will be executing at a time.
   *
   * ```
   * class UserAndSongSaver {
   *    val singleRunner = SingleRunner()
   *
   *    fun saveUser(user: User) {
   *        singleRunner.afterPrevious { api.post(user) }
   *    }
   *
   *    fun saveSong(song: Song) {
   *        singleRunner.afterPrevious { api.post(song) }
   *    }
   * }
   * ```
   *
   * @param block the code to run after previous work is complete.
   */
  suspend fun <T> afterPrevious(block: suspend () -> T): T {
    // Before running the block, ensure that no other blocks are running by taking a lock on the
    // mutex.

    // The mutex will be released automatically when we return.

    // If any other block were already running when we get here, it will wait for it to complete
    // before entering the `withLock` block.
    mutex.withLock {
      return block()
    }
  }
}
