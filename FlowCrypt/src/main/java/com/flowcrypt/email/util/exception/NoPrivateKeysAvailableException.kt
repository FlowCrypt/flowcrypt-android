/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import android.content.Context

import com.flowcrypt.email.R

/**
 * This exception means no private keys available for a given email account.
 *
 * @author Denis Bondarenko
 * Date: 19.10.2018
 * Time: 10:22
 * E-mail: DenBond7@gmail.com
 */
class NoPrivateKeysAvailableException(context: Context, val email: String)
  : FlowCryptException(context.getString(R.string.there_are_no_private_keys, email))
