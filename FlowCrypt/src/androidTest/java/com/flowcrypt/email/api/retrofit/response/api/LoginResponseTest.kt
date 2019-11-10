/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.api

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.flowcrypt.email.DoesNotNeedMailserver
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 11/4/19
 * Time: 1:22 PM
 * E-mail: DenBond7@gmail.com
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
@DoesNotNeedMailserver
class LoginResponseTest {
  @Test
  fun testParcelable() {
    val original = LoginResponse(
        ApiError(404, "msg", "internal"),
        isRegistered = true,
        isVerified = false)

    val parcel = Parcel.obtain()
    original.writeToParcel(parcel, original.describeContents())
    parcel.setDataPosition(0)

    val createdFromParcel = LoginResponse.createFromParcel(parcel)
    Assert.assertTrue(null, original == createdFromParcel)
  }
}