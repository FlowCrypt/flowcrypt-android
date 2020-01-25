/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import android.database.sqlite.SQLiteDatabase
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update

/**
 * It's a base implementation of [androidx.room.Dao] interface which contains the common methods
 *
 * @author Denis Bondarenko
 *         Date: 12/19/19
 *         Time: 11:11 AM
 *         E-mail: DenBond7@gmail.com
 */
interface BaseDao<T> {
  @Insert
  fun insert(entity: T): Long

  @Insert
  fun insert(vararg entities: T)

  @Insert
  fun insert(entities: Iterable<T>)

  @Insert(onConflict = SQLiteDatabase.CONFLICT_REPLACE)
  fun insertWithReplace(vararg entities: T)

  @Insert(onConflict = SQLiteDatabase.CONFLICT_REPLACE)
  fun insertWithReplace(entities: Iterable<T>)

  @Insert(onConflict = SQLiteDatabase.CONFLICT_REPLACE)
  suspend fun insertWithReplaceSuspend(vararg entities: T)

  @Insert(onConflict = SQLiteDatabase.CONFLICT_REPLACE)
  suspend fun insertWithReplaceSuspend(entities: Iterable<T>)

  @Update
  fun update(entity: T): Int

  @Update
  suspend fun updateSuspend(entity: T): Int

  @Update
  fun update(entities: Iterable<T>): Int

  @Update
  suspend fun updateSuspend(entities: Iterable<T>): Int

  @Delete
  fun delete(entity: T): Int

  @Delete
  suspend fun deleteSuspend(entity: T): Int

  @Delete
  fun delete(entities: Iterable<T>): Int

  @Delete
  suspend fun deleteSuspend(entities: Iterable<T>): Int


  companion object {
    fun <M> doOperationViaSteps(stepValue: Int = 50, list: List<M>,
                                block: (list: Collection<M>) -> Int) {
      if (list.isNotEmpty()) {
        if (list.size <= stepValue) {
          block(list)
        } else {
          var i = 0
          while (i < list.size) {
            val tempList = if (list.size - i > stepValue) {
              list.subList(i, i + stepValue)
            } else {
              list.subList(i, list.size)
            }
            block(tempList)
            i += stepValue
          }
        }
      }
    }

    suspend fun <M> doOperationViaStepsSuspend(stepValue: Int = 50, list: List<M>,
                                               block: suspend (list: Collection<M>) -> Int) {
      if (list.isNotEmpty()) {
        if (list.size <= stepValue) {
          block(list)
        } else {
          var i = 0
          while (i < list.size) {
            val tempList = if (list.size - i > stepValue) {
              list.subList(i, i + stepValue)
            } else {
              list.subList(i, list.size)
            }
            block(tempList)
            i += stepValue
          }
        }
      }
    }
  }
}