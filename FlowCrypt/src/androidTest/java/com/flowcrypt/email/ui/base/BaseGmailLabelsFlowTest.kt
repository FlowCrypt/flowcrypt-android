/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.base

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddLabelsToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.ui.adapter.GmailApiLabelsListAdapter
import com.flowcrypt.email.util.AccountDaoManager
import org.hamcrest.Description
import org.junit.runner.RunWith

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
abstract class BaseGmailLabelsFlowTest : BaseMessageDetailsFlowTest() {
  protected val userWithClientConfiguration = AccountDaoManager.getDefaultAccountDao().copy(
    clientConfiguration = ClientConfiguration(
      flags = listOf(
        ClientConfiguration.ConfigurationProperty.NO_PRV_CREATE,
        ClientConfiguration.ConfigurationProperty.NO_PRV_BACKUP,
        ClientConfiguration.ConfigurationProperty.RESTRICT_ANDROID_ATTACHMENT_HANDLING
      ),
      customKeyserverUrl = null,
      keyManagerUrl = "https://keymanagerurl.test",
      disallowAttesterSearchForDomains = null,
      enforceKeygenAlgo = null,
      enforceKeygenExpireMonths = null
    ),
    accountType = AccountEntity.ACCOUNT_TYPE_GOOGLE,
    useCustomerFesUrl = true,
    useAPI = true
  )

  final override val addAccountToDatabaseRule =
    AddAccountToDatabaseRule(userWithClientConfiguration)
  protected val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule(
    accountEntity = addAccountToDatabaseRule.account,
    keyPath = "pgp/default@flowcrypt.test_fisrtKey_prv_strong.asc",
    passphrase = TestConstants.DEFAULT_STRONG_PASSWORD,
    sourceType = KeyImportDetails.SourceType.EMAIL,
    passphraseType = KeyEntity.PassphraseType.DATABASE
  )

  protected val addLabelsToDatabaseRule = AddLabelsToDatabaseRule(
    account = addAccountToDatabaseRule.account, folders = mutableListOf(
      LocalFolder(
        account = addAccountToDatabaseRule.account.email,
        fullName = JavaEmailConstants.FOLDER_INBOX,
        folderAlias = JavaEmailConstants.FOLDER_INBOX,
        attributes = listOf("\\HasNoChildren")
      )
    ).apply {
      addAll(LABELS.map {
        LocalFolder(
          account = addAccountToDatabaseRule.account.email,
          fullName = it.name,
          folderAlias = it.name,
          isCustom = true,
          labelColor = it.backgroundColor,
          textColor = it.textColor,
          attributes = listOf("\\HasNoChildren")
        )
      })
    }
  )

  protected fun genIncomingMessageInfo() = getMsgInfo(
    "messages/info/standard_msg_info_plaintext.json",
    "messages/mime/standard_msg_info_plaintext.txt"
  ) { incomingMsgInfo ->
    val originalMessageEntity = incomingMsgInfo?.msgEntity ?: return@getMsgInfo null
    return@getMsgInfo incomingMsgInfo.copy(
      msgEntity = originalMessageEntity.copy(
        labelIds = initLabelIds().joinToString(MessageEntity.LABEL_IDS_SEPARATOR)
      )
    )
  }

  protected fun initLabelIds() = mutableListOf(GmailApiHelper.LABEL_INBOX).apply {
    addAll(LABELS.map { it.name })
  }

  class GmailApiLabelMatcher(private val label: GmailApiLabelsListAdapter.Label) :
    BoundedMatcher<ViewHolder, GmailApiLabelsListAdapter.ViewHolder>(
      GmailApiLabelsListAdapter.ViewHolder::class.java
    ) {
    override fun matchesSafely(holder: GmailApiLabelsListAdapter.ViewHolder): Boolean {
      return holder.binding.textViewLabel.text == label.name
    }

    override fun describeTo(description: Description) {
      description.appendText("with: $label")
    }
  }

  companion object {
    val LABELS = listOf(
      GmailApiLabelsListAdapter.Label("Test1", "#ff2323ff", "#FFFFFF"),
      GmailApiLabelsListAdapter.Label("Test2", "#FFD14836", "#FFFFFF"),
      GmailApiLabelsListAdapter.Label("Test3", "#FFFFEB3B", "#FFFFFF"),
    )
  }
}
