/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcelable
import com.google.gson.annotations.Expose
import kotlinx.parcelize.Parcelize

/**
 * @author Denys Bondarenko
 */
@Parcelize
data class Key constructor(@Expose val decryptedPrivateKey: String? = null) : Parcelable
