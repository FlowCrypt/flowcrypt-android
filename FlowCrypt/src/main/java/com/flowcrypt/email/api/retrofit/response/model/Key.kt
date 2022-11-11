/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcelable
import com.google.gson.annotations.Expose
import kotlinx.parcelize.Parcelize

/**
 * @author Denis Bondarenko
 *         Date: 6/30/21
 *         Time: 2:48 PM
 *         E-mail: DenBond7@gmail.com
 */
@Parcelize
data class Key constructor(@Expose val decryptedPrivateKey: String? = null) : Parcelable
