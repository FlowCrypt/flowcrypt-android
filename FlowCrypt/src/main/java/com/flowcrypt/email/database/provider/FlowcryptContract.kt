/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.provider

import android.net.Uri

import com.flowcrypt.email.BuildConfig

/**
 * The contract between the Flowcrypt provider and an application. Contains definitions for the
 * supported URIs and data columns.
 *
 * @author Denis Bondarenko
 * Date: 13.05.2017
 * Time: 11:53
 * E-mail: DenBond7@gmail.com
 */
class FlowcryptContract {
  companion object {
    @JvmField
    val AUTHORITY = BuildConfig.APPLICATION_ID + "." + SecurityContentProvider::class.java.simpleName

    @JvmField
    val AUTHORITY_URI: Uri = Uri.parse("content://$AUTHORITY")

    const val CLEAN_DATABASE = "/clean"
    const val ERASE_DATABASE = "/erase"
  }
}
