/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */
package com.flowcrypt.email

import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.manipulation.Filter
import org.junit.runner.manipulation.Filterable
import org.junit.runner.manipulation.NoTestsRemainException
import org.junit.runner.manipulation.Sortable
import org.junit.runner.manipulation.Sorter
import org.junit.runner.notification.RunNotifier
import org.junit.runners.model.InitializationError
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.util.Locale

/**
 * Based on [androidx.test.ext.junit.runners.AndroidJUnit4]
 *
 * @author Denys Bondarenko
 */
open class CustomAndroidJUnit4(klass: Class<*>) : Runner(), Filterable, Sortable {
  val delegate: Runner = loadRunner(klass)

  override fun getDescription(): Description {
    return delegate.description
  }

  override fun run(runNotifier: RunNotifier) {
    delegate.run(runNotifier)
  }

  @Throws(NoTestsRemainException::class)
  override fun filter(filter: Filter) {
    (delegate as Filterable).filter(filter)
  }

  override fun sort(sorter: Sorter) {
    (delegate as Sortable).sort(sorter)
  }

  companion object {
    private const val TAG = "AndroidJUnit4"

    private val runnerClassName: String
      get() {
        val runnerClassName =
          System.getProperty("android.junit.runner", null)
            ?: return if (
              System.getProperty("java.runtime.name")
                ?.lowercase(Locale.getDefault())?.contains("android") == false
              && hasClass("org.robolectric.RobolectricTestRunner")
            ) {
              "org.robolectric.RobolectricTestRunner"
            } else {
              "androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner"
            }
        return runnerClassName
      }

    private fun hasClass(className: String): Boolean {
      return try {
        Class.forName(className) != null
      } catch (e: ClassNotFoundException) {
        false
      }
    }

    @Throws(InitializationError::class)
    private fun loadRunner(testClass: Class<*>): Runner {
      val runnerClassName = runnerClassName
      return loadRunner(testClass, runnerClassName)
    }

    @Throws(InitializationError::class)
    private fun loadRunner(testClass: Class<*>, runnerClassName: String): Runner {
      var runnerClass: Class<out Runner?>? = null
      try {
        runnerClass = Class.forName(runnerClassName) as Class<out Runner?>
      } catch (e: ClassNotFoundException) {
        throwInitializationError(
          String.format(
            "Delegate runner %s for AndroidJUnit4 could not be found.\n", runnerClassName
          ),
          e
        )
      }

      var constructor: Constructor<out Runner?>? = null
      try {
        constructor = runnerClass!!.getConstructor(Class::class.java)
      } catch (e: NoSuchMethodException) {
        throwInitializationError(
          String.format(
            "Delegate runner %s for AndroidJUnit4 requires a public constructor that takes a"
                + " Class<?>.\n",
            runnerClassName
          ),
          e
        )
      }

      try {
        return constructor!!.newInstance(testClass)!!
      } catch (e: IllegalAccessException) {
        throwInitializationError(
          String.format("Illegal constructor access for test runner %s\n", runnerClassName), e
        )
      } catch (e: InstantiationException) {
        throwInitializationError(
          String.format("Failed to instantiate test runner %s\n", runnerClassName), e
        )
      } catch (e: InvocationTargetException) {
        val details = getInitializationErrorDetails(e, testClass)
        throwInitializationError(
          String.format("Failed to instantiate test runner %s\n%s\n", runnerClass, details), e
        )
      }
      throw IllegalStateException("Should never reach here")
    }

    @Throws(InitializationError::class)
    private fun throwInitializationError(details: String, cause: Throwable) {
      throw InitializationError(RuntimeException(details, cause))
    }

    private fun getInitializationErrorDetails(throwable: Throwable, testClass: Class<*>): String {
      val innerCause = StringBuilder()
      val cause = throwable.cause ?: return ""

      val causeClass: Class<out Throwable> = cause.javaClass
      if (causeClass == InitializationError::class.java) {
        val initializationError = cause as InitializationError
        val testClassProblemList = initializationError.causes
        innerCause.append(
          String.format(
            "Test class %s is malformed. (%s problems):\n",
            testClass, testClassProblemList.size
          )
        )
        for (testClassProblem in testClassProblemList) {
          innerCause.append(testClassProblem).append("\n")
        }
      }
      return innerCause.toString()
    }
  }
}
