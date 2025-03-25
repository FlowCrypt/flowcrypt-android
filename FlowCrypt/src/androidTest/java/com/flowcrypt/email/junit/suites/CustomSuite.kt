/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.junit.suites

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.junit.Assert
import org.junit.runner.Runner
import org.junit.runners.Suite
import org.junit.runners.model.FrameworkField
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.TestClass
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.text.MessageFormat
import java.util.Arrays
import java.util.Locale

/**
 * @author Denys Bondarenko
 */
class CustomSuite(klass: Class<*>?) : Suite(klass, mutableListOf<Runner?>()) {
  /**
   * Annotation for a method which provides parameters to be injected into the test class
   * constructor by `Parameterized`
   */
  @Retention(AnnotationRetention.RUNTIME)
  @Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
  )
  annotation class Parameters(
    /**
     * Optional pattern to derive the test's name from the parameters. Use numbers in braces to
     * refer to the parameters or the additional data as follows:
     *
     * <pre>
     * {index} - the current parameter index
     * {0} - the first parameter value
     * {1} - the second parameter value
     * etc...
    </pre> *
     *
     *
     * Default value is "{index}" for compatibility with previous JUnit versions.
     *
     * @return [java.text.MessageFormat] pattern string, except the index placeholder.
     * @see java.text.MessageFormat
     */
    val name: String = "{index}"
  )

  /**
   * Annotation for fields of the test class which will be initialized by the method annotated by
   * `Parameters`<br></br>
   * By using directly this annotation, the test class constructor isn't needed.<br></br>
   * Index range must start at 0. Default value is 0.
   */
  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.FIELD)
  annotation class Parameter(
    /**
     * Method that returns the index of the parameter in the array returned by the method annotated
     * by `Parameters`.<br></br>
     * Index range must start at 0. Default value is 0.
     *
     * @return the index of the parameter.
     */
    val value: Int = 0
  )

  private class TestClassRunnerForParameters(
    type: Class<*>,
    private val parametersIndex: Int,
    val customName: String
  ) : AndroidJUnit4ClassRunner(type) {

    @Throws(Exception::class)
    fun createTestInstance(bootstrappedClass: Class<*>): Any {
      val constructors = bootstrappedClass.constructors
      Assert.assertEquals(1, constructors.size.toLong())
      if (!fieldsAreAnnotated()) {
        return constructors[0]!!.newInstance(*computeParams(bootstrappedClass.getClassLoader()))
      } else {
        val instance: Any = constructors[0]!!.newInstance()
        injectParametersIntoFields(instance, bootstrappedClass.getClassLoader())
        return instance
      }
    }

    @Throws(Exception::class)
    fun computeParams(classLoader: ClassLoader): Array<Any?> {
      // Robolectric uses a different class loader when running the tests, so the parameters objects
      // created by the test runner are not compatible with the parameters required by the test.
      // Instead, we compute the parameters within the test's class loader.
      try {
        val parametersList = getParametersList(testClass, classLoader)

        if (parametersIndex >= parametersList.size) {
          throw Exception(
            "Re-computing the parameter list returned a different number of "
                + "parameters values. Is the data() method of your test non-deterministic?"
          )
        }
        val parametersObj = parametersList.get(parametersIndex)
        return if (parametersObj is Array<*> && parametersObj.isArrayOf<Any>())
          parametersObj as Array<Any?>
        else
          arrayOf<Any?>(parametersObj)
      } catch (e: ClassCastException) {
        throw Exception(
          String.format(
            "%s.%s() must return a Collection of arrays.", testClass.getName(), customName
          )
        )
      } catch (exception: Exception) {
        throw exception
      } catch (throwable: Throwable) {
        throw Exception(throwable)
      }
    }

    @Throws(Exception::class)
    fun injectParametersIntoFields(testClassInstance: Any, classLoader: ClassLoader) {
      // Robolectric uses a different class loader when running the tests, so referencing Parameter
      // directly causes type mismatches. Instead, we find its class within the test's class loader.
      val parameterClass = getClassInClassLoader(Parameter::class.java, classLoader)
      val parameters = computeParams(classLoader)
      val parameterFieldsFound = HashSet<Int?>()
      for (field in testClassInstance.javaClass.getFields()) {
        val parameter = field.getAnnotation<Annotation?>(parameterClass as Class<Annotation?>?)
        if (parameter != null) {
          val index = callInstanceMethod<Int?>(parameter, "value") ?: continue
          parameterFieldsFound.add(index)
          try {
            field.set(testClassInstance, parameters[index])
          } catch (iare: IllegalArgumentException) {
            throw Exception(
              (testClass.getName()
                  + ": Trying to set "
                  + field.getName()
                  + " with the value "
                  + parameters[index]
                  + " that is not the right type ("
                  + parameters[index]!!.javaClass.getSimpleName()
                  + " instead of "
                  + field.type.getSimpleName()
                  + ")."),
              iare
            )
          }
        }
      }
      check(parameterFieldsFound.size == parameters.size) {
        String.Companion.format(
          Locale.US,
          "Provided %d parameters, but only found fields for parameters: %s",
          parameters.size,
          parameterFieldsFound.toString()
        )
      }
    }

    override fun testName(method: FrameworkMethod): String {
      return method.name + this.customName
    }

    override fun validateConstructor(errors: MutableList<Throwable?>?) {
      validateOnlyOneConstructor(errors)
      if (fieldsAreAnnotated()) {
        validateZeroArgConstructor(errors)
      }
    }

    override fun toString(): String {
      return "TestClassRunnerForParameters $customName"
    }

    override fun validateFields(errors: MutableList<Throwable?>) {
      super.validateFields(errors)
      // Ensure that indexes for parameters are correctly defined
      if (fieldsAreAnnotated()) {
        val annotatedFieldsByParameter: MutableList<FrameworkField> =
          this.annotatedFieldsByParameter
        val usedIndices = IntArray(annotatedFieldsByParameter.size)
        for (each in annotatedFieldsByParameter) {
          val index: Int = each.field
            .getAnnotation<Parameter?>(
              Parameter::class.java
            ).value
          if (index < 0 || index > annotatedFieldsByParameter.size - 1) {
            errors.add(
              Exception(
                ("Invalid @Parameter value: "
                    + index
                    + ". @Parameter fields counted: "
                    + annotatedFieldsByParameter.size
                    + ". Please use an index between 0 and "
                    + (annotatedFieldsByParameter.size - 1)
                    + ".")
              )
            )
          } else {
            usedIndices[index]++
          }
        }
        for (index in usedIndices.indices) {
          val numberOfUse = usedIndices[index]
          if (numberOfUse == 0) {
            errors.add(Exception("@Parameter($index) is never used."))
          } else if (numberOfUse > 1) {
            errors.add(Exception("@Parameter($index) is used more than once ($numberOfUse)."))
          }
        }
      }
    }

    val annotatedFieldsByParameter: MutableList<FrameworkField>
      get() = testClass.getAnnotatedFields(Parameter::class.java)

    fun fieldsAreAnnotated(): Boolean {
      return !this.annotatedFieldsByParameter.isEmpty()
    }

    companion object {
      fun <R> callInstanceMethod(
        instance: Any, methodName: String, vararg classParameters: ClassParameter<*>?
      ): R? {
        PerfStatsCollector.instance.incrementCount(
          String.format(
            "ReflectionHelpers.callInstanceMethod-%s_%s",
            instance.javaClass.getName(), methodName
          )
        )
        try {
          val classes: Array<Class<*>?> = ClassParameter.getClasses(*classParameters)
          val values: Array<Any?> = ClassParameter.getValues(*classParameters)

          return traverseClassHierarchy<R?, NoSuchMethodException?>(
            instance.javaClass,
            NoSuchMethodException::class.java,
            { traversalClass: Class<*>? ->
              val declaredMethod = traversalClass!!.getDeclaredMethod(methodName, *classes)
              declaredMethod.isAccessible = true
              declaredMethod.invoke(instance, *values) as R?
            })
        } catch (e: InvocationTargetException) {
          if (e.targetException is java.lang.RuntimeException) {
            throw e.targetException as java.lang.RuntimeException? as Throwable
          }
          if (e.targetException is Error) {
            throw e.targetException as Error? as Throwable
          }
          throw java.lang.RuntimeException(e.targetException)
        } catch (e: java.lang.Exception) {
          throw java.lang.RuntimeException(e)
        }
      }

      @Throws(java.lang.Exception::class)
      private fun <R, E : java.lang.Exception?> traverseClassHierarchy(
        targetClass: Class<*>, exceptionClass: Class<out E?>, insideTraversal: InsideTraversal<R?>
      ): R? {
        var hierarchyTraversalClass = targetClass
        while (true) {
          try {
            return insideTraversal.run(hierarchyTraversalClass)
          } catch (e: java.lang.Exception) {
            if (!exceptionClass.isInstance(e)) {
              throw e
            }
            hierarchyTraversalClass = hierarchyTraversalClass.getSuperclass()
            if (hierarchyTraversalClass == null) {
              throw java.lang.RuntimeException(e)
            }
          }
        }
      }
    }
  }

  private val runners = ArrayList<Runner?>()

  /*
   * Only called reflectively. Do not use programmatically.
   */
  init {
    val testClass = getTestClass()
    val classLoader = javaClass.getClassLoader()
    if (classLoader != null) {
      val parameters: Parameters =
        getParametersMethod(
          testClass,
          classLoader
        ).getAnnotation<Parameters?>(Parameters::class.java)
      val parametersList = getParametersList(testClass, classLoader)
      for (i in parametersList.indices) {
        val parametersObj = parametersList[i]
        val parameterArray = if (parametersObj is Array<*> && parametersObj.isArrayOf<Any>()) {
          parametersObj
        } else {
          arrayOf<Any?>(parametersObj)
        }
        runners.add(
          TestClassRunnerForParameters(
            testClass.javaClass, i, nameFor(parameters.name, i, parameterArray)
          )
        )
      }
    }

  }

  override fun getChildren(): MutableList<Runner?> {
    return runners
  }

  companion object {
    @Throws(Throwable::class)
    private fun getParametersList(
      testClass: TestClass,
      classLoader: ClassLoader
    ): MutableList<Any?> {
      val parameters = getParametersMethod(testClass, classLoader).invokeExplosively(null)
      if (parameters != null && parameters.javaClass.isArray) {
        return Arrays.asList<Any?>(*parameters as Array<Any?>)
      } else {
        return parameters as MutableList<Any?>
      }
    }

    @Throws(Exception::class)
    private fun getParametersMethod(
      testClass: TestClass,
      classLoader: ClassLoader
    ): FrameworkMethod {
      val methods = testClass.getAnnotatedMethods(Parameters::class.java)
      for (each in methods) {
        val modifiers = each.method.modifiers
        if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) {
          return getFrameworkMethodInClassLoader(each, classLoader)
        }
      }

      throw Exception("No public static parameters method on class " + testClass.getName())
    }

    private fun nameFor(namePattern: String, index: Int, parameters: Array<out Any?>): String {
      val finalPattern = namePattern.replace("\\{index\\}".toRegex(), index.toString())
      val name = MessageFormat.format(finalPattern, *parameters)
      return "[" + name + "]"
    }

    /**
     * Returns the [FrameworkMethod] object for the given method in the provided class loader.
     */
    @Throws(ClassNotFoundException::class, NoSuchMethodException::class)
    private fun getFrameworkMethodInClassLoader(
      method: FrameworkMethod, classLoader: ClassLoader
    ): FrameworkMethod {
      val methodInClassLoader = getMethodInClassLoader(method.method, classLoader)
      if (methodInClassLoader == method.method) {
        // The method was already loaded in the right class loader, return it as is.
        return method
      }
      return FrameworkMethod(methodInClassLoader)
    }

    /** Returns the [java.lang.reflect.Method] object for the given method in the provided class loader.  */
    @Throws(ClassNotFoundException::class, NoSuchMethodException::class)
    private fun getMethodInClassLoader(method: Method, classLoader: ClassLoader): Method {
      val declaringClass = method.declaringClass

      if (declaringClass.getClassLoader() === classLoader) {
        // The method was already loaded in the right class loader, return it as is.
        return method
      }

      // Find the class in the class loader corresponding to the declaring class of the method.
      val declaringClassInClassLoader = getClassInClassLoader(declaringClass, classLoader)

      // Find the method with the same signature in the class loader.
      return declaringClassInClassLoader.getMethod(method.name, *method.getParameterTypes())
    }

    /** Returns the [Class] object for the given class in the provided class loader.  */
    @Throws(ClassNotFoundException::class)
    private fun getClassInClassLoader(klass: Class<*>, classLoader: ClassLoader): Class<*> {
      if (klass.getClassLoader() === classLoader) {
        // The method was already loaded in the right class loader, return it as is.
        return klass
      }

      // Find the class in the class loader corresponding to the declaring class of the method.
      return classLoader.loadClass(klass.getName())
    }
  }
}