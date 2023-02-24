/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import android.content.Context

import com.flowcrypt.email.R

/**
 * This exception means that no key available for a given email account or alias.
 *
 * @author Denys Bondarenko
 */
class NoKeyAvailableException(context: Context, val email: String, val alias: String? = null) :
  FlowCryptException(
    context.getString(
      R.string.no_key_available_for_your_email_account,
      context.getString(R.string.support_email)
    )
  )
