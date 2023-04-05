/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.oauth2

import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * @author Denys Bondarenko
 */
@Parcelize
data class MicrosoftOAuth2TokenResponse constructor(
  @Expose override val code: Int? = null,
  @Expose override val message: String? = null,
  @Expose override val details: String? = null,
  @SerializedName("access_token") @Expose val accessToken: String? = null,
  @SerializedName("token_type") @Expose val tokenType: String? = null,
  @SerializedName("expires_in") @Expose val expiresIn: Long? = null,
  @Expose val scope: String? = null,
  @SerializedName("refresh_token") @Expose val refreshToken: String? = null,
  @SerializedName("id_token") @Expose val idToken: String? = null,
  @Expose val error: String? = null,
  @SerializedName("error_description") @Expose val errorDescription: String? = null,
  @SerializedName("error_codes") @Expose val errorCodes: IntArray? = null,
  @Expose val timestamp: String? = null,
  @SerializedName("trace_id") @Expose val traceId: String? = null,
  @SerializedName("correlation_id") @Expose val correlationId: String? = null
) : ApiResponse {
  override val apiError: ApiError?
    get() = if (error != null) {
      ApiError(
        code = errorCodes?.firstOrNull(),
        message = error + "\n" + errorDescription,
        details = error
      )
    } else null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as MicrosoftOAuth2TokenResponse

    if (accessToken != other.accessToken) return false
    if (tokenType != other.tokenType) return false
    if (expiresIn != other.expiresIn) return false
    if (scope != other.scope) return false
    if (refreshToken != other.refreshToken) return false
    if (idToken != other.idToken) return false
    if (error != other.error) return false
    if (errorDescription != other.errorDescription) return false
    if (errorCodes != null) {
      if (other.errorCodes == null) return false
      if (!errorCodes.contentEquals(other.errorCodes)) return false
    } else if (other.errorCodes != null) return false
    if (timestamp != other.timestamp) return false
    if (traceId != other.traceId) return false
    if (correlationId != other.correlationId) return false
    if (apiError != other.apiError) return false

    return true
  }

  override fun hashCode(): Int {
    var result = accessToken?.hashCode() ?: 0
    result = 31 * result + (tokenType?.hashCode() ?: 0)
    result = 31 * result + (expiresIn?.hashCode() ?: 0)
    result = 31 * result + (scope?.hashCode() ?: 0)
    result = 31 * result + (refreshToken?.hashCode() ?: 0)
    result = 31 * result + (idToken?.hashCode() ?: 0)
    result = 31 * result + (error?.hashCode() ?: 0)
    result = 31 * result + (errorDescription?.hashCode() ?: 0)
    result = 31 * result + (errorCodes?.contentHashCode() ?: 0)
    result = 31 * result + (timestamp?.hashCode() ?: 0)
    result = 31 * result + (traceId?.hashCode() ?: 0)
    result = 31 * result + (correlationId?.hashCode() ?: 0)
    result = 31 * result + (apiError?.hashCode() ?: 0)
    return result
  }
}
