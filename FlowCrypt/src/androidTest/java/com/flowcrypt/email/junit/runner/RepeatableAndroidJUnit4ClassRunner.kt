/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.junit.runner

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.junit.runner.Runner
import org.junit.runners.Suite
import org.junit.runners.model.FrameworkMethod

/**
 * A custom implementation of [Suite] which allows to run a test a few times naturally
 * like [androidx.test.ext.junit.runners.AndroidJUnit4] does it.
 * It helps debug issues with flaky tests when [com.flowcrypt.email.rules.RepeatRule] doesn't help.
 *
 * Inspired by [ParameterizedRobolectricTestRunner](https://github.com/robolectric/robolectric/blob/master/robolectric/src/main/java/org/robolectric/ParameterizedRobolectricTestRunner.java)
 *
 * @author Denys Bondarenko
 */
class RepeatableAndroidJUnit4ClassRunner(klass: Class<*>?) : Suite(klass, emptyList<Runner>()) {
  private val runners = ArrayList<Runner?>()

  init {
    val attemptsCount =
      testClass.getAnnotation(RepeatTest::class.java)?.value?.takeIf { it > 0 } ?: 1
    for (i in 0 until attemptsCount) {
      runners.add(
        CustomAndroidJUnit4ClassRunner(
          klass = testClass.javaClass,
          postfix = "_${i + 1}".takeIf { attemptsCount > 1 } ?: ""
        )
      )
    }
  }

  override fun getChildren(): MutableList<Runner?> = runners

  /**
   * Annotation for a class which provides attempts count
   */
  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.CLASS)
  annotation class RepeatTest(
    /**
     * Should be a positive number
     */
    val value: Int = 1
  )

  /**
   * A custom implementation of [AndroidJUnit4ClassRunner] which provides custom names for tests
   * based on the given postfix.
   */
  private class CustomAndroidJUnit4ClassRunner(
    klass: Class<*>, private val postfix: String
  ) : AndroidJUnit4ClassRunner(klass) {
    override fun testName(method: FrameworkMethod) = method.name + postfix
    override fun toString() = "TestClassRunnerForParameters_$postfix"
  }
}