/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.Locator
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.lazyActivityScenarioRule
import com.flowcrypt.email.ui.activity.MessageDetailsActivity
import org.hamcrest.CoreMatchers.equalTo
import org.junit.After

/**
 * @author Denis Bondarenko
 *         Date: 6/7/21
 *         Time: 1:25 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseMessageDetailsActivityTest : BaseTest() {
  override val useIntents: Boolean = true
  override val activeActivityRule =
    lazyActivityScenarioRule<MessageDetailsActivity>(launchActivity = false)
  override val activityScenario: ActivityScenario<*>?
    get() = activeActivityRule.scenario
  protected open val addAccountToDatabaseRule = AddAccountToDatabaseRule()

  private val localFolder: LocalFolder
    get() = LocalFolder(
      addAccountToDatabaseRule.account.email,
      fullName = "INBOX",
      folderAlias = "INBOX",
      msgCount = 1,
      attributes = listOf("\\HasNoChildren")
    )

  private var idlingForWebView: IdlingResource? = null

  @After
  fun unregisterDecryptionIdling() {
    idlingForWebView?.let { IdlingRegistry.getInstance().unregister(it) }
  }

  protected fun launchActivity(msgEntity: MessageEntity) {
    activeActivityRule.launch(
      MessageDetailsActivity.getIntent(
        getTargetContext(),
        localFolder,
        msgEntity
      )
    )
    registerAllIdlingResources()

    activityScenario?.onActivity { activity ->
      val messageDetailsActivity = (activity as? MessageDetailsActivity) ?: return@onActivity
      idlingForWebView = messageDetailsActivity.idlingForWebView
      idlingForWebView?.let { IdlingRegistry.getInstance().register(it) }
    }
  }

  protected fun checkWebViewText(text: String?) {
    onWebView(withId(R.id.emailWebView)).forceJavascriptEnabled()
    onWebView(withId(R.id.emailWebView))
      .withElement(findElement(Locator.XPATH, "/html/body"))
      .check(webMatches(getText(), equalTo(text)))
  }
}
