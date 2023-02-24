/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [ViewModel] implementation can be used to check the web portal strength
 *
 * @author Denys Bondarenko
 */
class WebPortalPasswordStrengthViewModel(application: Application) :
  BaseAndroidViewModel(application) {
  private val controlledRunnerForChecking = ControlledRunner<List<RequirementItem>>()

  private val pwdStrengthResultMutableStateFlow: MutableStateFlow<List<RequirementItem>> =
    MutableStateFlow(emptyList())
  val pwdStrengthResultStateFlow: StateFlow<List<RequirementItem>> =
    pwdStrengthResultMutableStateFlow.asStateFlow()

  fun check(password: CharSequence) {
    viewModelScope.launch {
      pwdStrengthResultMutableStateFlow.value = controlledRunnerForChecking.cancelPreviousThenRun {
        return@cancelPreviousThenRun checkInternal(password)
      }
    }
  }

  private suspend fun checkInternal(password: CharSequence): List<RequirementItem> =
    withContext(Dispatchers.Default) {
      return@withContext Requirement.values().map {
        RequirementItem(it, it.regex.containsMatchIn(password))
      }
    }

  data class RequirementItem(
    val requirement: Requirement,
    val isMatching: Boolean
  )

  enum class Requirement constructor(val regex: Regex) {
    MIN_LENGTH(".{8,}".toRegex(RegexOption.MULTILINE)),
    ONE_UPPERCASE("\\p{Lu}".toRegex(RegexOption.MULTILINE)),
    ONE_LOWERCASE("\\p{Ll}".toRegex(RegexOption.MULTILINE)),
    ONE_NUMBER("\\d".toRegex(RegexOption.MULTILINE)),
    ONE_SPECIAL_CHARACTER("[&\"#-'_%-/@,;:!*()]".toRegex(RegexOption.MULTILINE));
  }
}
