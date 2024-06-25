/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import com.flowcrypt.email.util.exception.AccountAlreadyAddedException
import com.flowcrypt.email.util.google.GoogleApiClientHelper
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import java.util.UUID

/**
 * @author Denys Bondarenko
 */
class SignInWithGoogleViewModel(application: Application) : AccountViewModel(application) {
  private val controlledRunnerForGoogleIdTokenCredential =
    ControlledRunner<Result<GoogleIdTokenCredential?>>()
  private val googleIdTokenCredentialMutableStateFlow: MutableStateFlow<Result<GoogleIdTokenCredential?>> =
    MutableStateFlow(Result.none())
  val googleIdTokenCredentialStateFlow: StateFlow<Result<GoogleIdTokenCredential?>> =
    googleIdTokenCredentialMutableStateFlow.asStateFlow()

  fun authenticateUser(activityContext: Context) {
    viewModelScope.launch {
      googleIdTokenCredentialMutableStateFlow.value = Result.loading()
      googleIdTokenCredentialMutableStateFlow.value =
        controlledRunnerForGoogleIdTokenCredential.cancelPreviousThenRun {
          try {
            val randomNonce = UUID.randomUUID().toString()
            val getSignInWithGoogleOption =
              GetSignInWithGoogleOption.Builder(GoogleApiClientHelper.SERVER_CLIENT_ID)
                .setNonce(randomNonce)
                .build()

            val getCredentialRequest = GetCredentialRequest.Builder()
              .addCredentialOption(getSignInWithGoogleOption)
              .build()

            val getCredentialResponse = CredentialManager.create(activityContext).getCredential(
              context = activityContext,
              request = getCredentialRequest
            )

            when (val credential = getCredentialResponse.credential) {
              is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                  val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

                  //compare nonce from JWT
                  //mode details cab be found here https://auth0.com/docs/get-started/authentication-and-authorization-flow/implicit-flow-with-form-post/mitigate-replay-attacks-when-using-the-implicit-flow
                  val idToken = googleIdTokenCredential.idToken
                  val jwtConsumerBuilder = JwtConsumerBuilder()
                    //we don't need a verification. Just parse JWT and extract 'nonce' parameter
                    .setSkipSignatureVerification()
                    .setExpectedAudience(GoogleApiClientHelper.SERVER_CLIENT_ID)
                    .build()
                  val claims = jwtConsumerBuilder.processToClaims(idToken)
                  if (claims.getClaimValueAsString("nonce") != randomNonce) {
                    throw IllegalStateException("Security error: 'nonce' mismatch")
                  }

                  //check that the given account is new, not added yet
                  val existedAccount = roomDatabase.accountDao().getAccountsSuspend().firstOrNull {
                    it.email.equals(googleIdTokenCredential.id, ignoreCase = true)
                  }

                  if (existedAccount != null) {
                    throw AccountAlreadyAddedException(
                      activityContext.getString(
                        R.string.template_email_already_added,
                        existedAccount.email
                      )
                    )
                  }

                  return@cancelPreviousThenRun Result.success(googleIdTokenCredential)
                } else {
                  throw IllegalStateException(activityContext.getString(R.string.unsupported_credentials))
                }
              }

              else -> {
                throw IllegalStateException(activityContext.getString(R.string.unsupported_credentials))
              }
            }
          } catch (e: Exception) {
            Result.exception(e)
          }
        }
    }
  }

  fun resetAuthenticationState() {
    googleIdTokenCredentialMutableStateFlow.value = Result.none()
  }

  fun cacheAuthenticationState() {
    googleIdTokenCredentialMutableStateFlow.value =
      googleIdTokenCredentialMutableStateFlow.value.toCached()
  }
}