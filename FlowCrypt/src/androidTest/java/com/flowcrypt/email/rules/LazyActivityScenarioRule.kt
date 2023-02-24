/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import org.junit.rules.ExternalResource

/**
 * https://medium.com/stepstone-tech/better-tests-with-androidxs-activityscenario-in-kotlin-part-1-6a6376b713ea
 *
 * @author Denys Bondarenko
 */
class LazyActivityScenarioRule<A : Activity> : ExternalResource {

  constructor(launchActivity: Boolean, startActivityIntentSupplier: () -> Intent) {
    this.launchActivity = launchActivity
    scenarioSupplier = { ActivityScenario.launch(startActivityIntentSupplier()) }
  }

  constructor(launchActivity: Boolean, startActivityIntent: Intent) {
    this.launchActivity = launchActivity
    scenarioSupplier = { ActivityScenario.launch(startActivityIntent) }
  }

  constructor(launchActivity: Boolean, startActivityClass: Class<A>) {
    this.launchActivity = launchActivity
    scenarioSupplier = { ActivityScenario.launch(startActivityClass) }
  }

  private var launchActivity: Boolean

  private var scenarioSupplier: () -> ActivityScenario<A>

  var scenario: ActivityScenario<A>? = null

  private var scenarioLaunched: Boolean = false

  override fun before() {
    if (launchActivity) {
      launch()
    }
  }

  override fun after() {
    scenario?.close()
    scenario = null
    scenarioLaunched = false
  }

  fun launch(newIntent: Intent? = null) {
    if (scenarioLaunched) throw IllegalStateException("Scenario has already been launched!")

    newIntent?.let { scenarioSupplier = { ActivityScenario.launch(it) } }

    scenario = scenarioSupplier()
    scenarioLaunched = true
  }

  fun getNonNullScenario(): ActivityScenario<A> = checkNotNull(scenario)
}

inline fun <reified A : Activity> lazyActivityScenarioRule(
  launchActivity: Boolean = true,
  noinline intentSupplier: () -> Intent
): LazyActivityScenarioRule<A> =
  LazyActivityScenarioRule(launchActivity, intentSupplier)

inline fun <reified A : Activity> lazyActivityScenarioRule(
  launchActivity: Boolean = true,
  intent: Intent? = null
): LazyActivityScenarioRule<A> = if (intent == null) {
  LazyActivityScenarioRule(launchActivity, A::class.java)
} else {
  LazyActivityScenarioRule(launchActivity, intent)
}
