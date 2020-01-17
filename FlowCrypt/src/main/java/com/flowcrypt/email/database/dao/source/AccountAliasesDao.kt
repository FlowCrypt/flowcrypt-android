/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source

/**
 * This object describes information about an account alias.
 *
 * @author Denis Bondarenko
 * Date: 26.10.2017
 * Time: 16:04
 * E-mail: DenBond7@gmail.com
 */

data class AccountAliasesDao constructor(var email: String? = null,
                                         var accountType: String? = null,
                                         var sendAsEmail: String? = null,
                                         var displayName: String? = null,
                                         var isDefault: Boolean = false,
                                         var verificationStatus: String? = null)
