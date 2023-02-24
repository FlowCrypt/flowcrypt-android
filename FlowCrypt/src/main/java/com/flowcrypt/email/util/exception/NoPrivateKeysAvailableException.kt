/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import android.content.Context

import com.flowcrypt.email.R

/**
 * This exception means no private keys available for a given email account.
 *
 * @author Denys Bondarenko
 */
class NoPrivateKeysAvailableException(context: Context, val email: String) :
  FlowCryptException(context.getString(R.string.there_are_no_private_keys, email))
