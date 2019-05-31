/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source

import android.net.Uri
import android.provider.BaseColumns

import com.flowcrypt.email.database.provider.FlowcryptContract

/**
 * The base data source class.
 *
 * @author DenBond7
 * Date: 13.05.2017
 * Time: 14:52
 * E-mail: DenBond7@gmail.com
 */

abstract class BaseDaoSource : BaseColumns {

  abstract val tableName: String

  val rowsContentType: String
    get() = "vnd.android.cursor.dir/vnd." + FlowcryptContract.AUTHORITY + "." + tableName

  val singleRowContentType: String
    get() = "vnd.android.cursor.item/vnd." + FlowcryptContract.AUTHORITY + "." + tableName

  val baseContentUri: Uri
    get() = Uri.parse(FlowcryptContract.AUTHORITY_URI.toString() + "/" + tableName)

  /**
   * Generate a selection [String] for a database query.
   *
   * @param strings The list of [String] objects for which need to generate a selection.
   * @return <tt>[String]</tt> A generated selection.
   */
  fun prepareSelection(strings: List<String>): String {
    val stringBuilder = StringBuilder()
    stringBuilder.append("( ?")


    if (strings.size > 1) {
      for (i in 1 until strings.size) {
        stringBuilder.append(", ?")
      }
    }

    stringBuilder.append(")")
    return stringBuilder.toString()
  }

  companion object {
    const val URI_PATH_GROUPED = "Grouped"
    const val INDEX_PREFIX = "CREATE INDEX IF NOT EXISTS "
    const val UNIQUE_INDEX_PREFIX = "CREATE UNIQUE INDEX IF NOT EXISTS "

    /**
     * Prepare a selection string depended on amount of selection args.
     *
     * @param objects Selection args.
     * @return A generated string.
     */
    @JvmStatic
    fun prepareSelectionArgsString(objects: Array<Any>?): String {
      return if (objects != null && objects.isNotEmpty()) {
        val result = StringBuilder()
        for (i in objects.indices) {
          if (i == 0) {
            result.append("?")
          } else {
            result.append(",?")
          }
        }

        result.toString()
      } else {
        ""
      }
    }
  }
}
