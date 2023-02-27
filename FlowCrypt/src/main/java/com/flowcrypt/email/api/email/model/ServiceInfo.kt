/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * This class describes service info details. Can be used when create a new messages.
 *
 * @author Denys Bondarenko
 */
@Parcelize
data class ServiceInfo constructor(
  val isToFieldEditable: Boolean = false,
  val isCcFieldEditable: Boolean = false,
  val isBccFieldEditable: Boolean = false,
  val isFromFieldEditable: Boolean = false,
  val isMsgEditable: Boolean = false,
  val isSubjectEditable: Boolean = false,
  val isMsgTypeSwitchable: Boolean = false,
  val hasAbilityToAddNewAtt: Boolean = false,
  val systemMsg: String? = null,
  val atts: List<AttachmentInfo>? = null
) : Parcelable
