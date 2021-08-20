/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.converters

import androidx.room.TypeConverter
import com.flowcrypt.email.api.retrofit.response.model.OrgRules
import com.google.gson.GsonBuilder

/**
 * @author Denis Bondarenko
 *         Date: 7/29/21
 *         Time: 2:19 PM
 *         E-mail: DenBond7@gmail.com
 */
class OrgRulesConverter {
  private val gson = GsonBuilder()
    .excludeFieldsWithoutExposeAnnotation()
    .serializeNulls()
    .create()

  @TypeConverter
  fun fromOrgRules(orgRules: OrgRules?): String? {
    return try {
      orgRules?.let { gson.toJson(orgRules) }
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  @TypeConverter
  fun toOrgRules(orgRulesJson: String?): OrgRules? {
    return try {
      orgRulesJson?.let { gson.fromJson(it, OrgRules::class.java) }
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }
}
