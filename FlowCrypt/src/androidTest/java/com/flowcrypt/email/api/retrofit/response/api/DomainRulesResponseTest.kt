/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.api

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.flowcrypt.email.DoesNotNeedMailserver
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.model.DomainRules
import com.flowcrypt.email.database.dao.source.AccountDao
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 11/4/19
 * Time: 1:23 PM
 * E-mail: DenBond7@gmail.com
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
@DoesNotNeedMailserver
class DomainRulesResponseTest {
  @Test
  fun testParcelable() {
    val original = DomainRulesResponse(ApiError(404, "msg", "internal"),
        DomainRules(listOf(AccountDao.DomainRule.NO_PRV_CREATE.name))
    )

    val parcel = Parcel.obtain()
    original.writeToParcel(parcel, original.describeContents())
    parcel.setDataPosition(0)

    val createdFromParcel = DomainRulesResponse.createFromParcel(parcel)
    Assert.assertTrue(null, original == createdFromParcel)
  }
}