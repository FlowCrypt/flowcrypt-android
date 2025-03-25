/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.junit.suites

/**
 * @author Denys Bondarenko
 */
/**
 * Typed parameter used with reflective method calls.
 *
 * @param <V> The value of the method parameter.
</V> */
class ClassParameter<V>(val clazz: Class<out V?>?, val value: V?) {
  companion object {
    fun <V> from(clazz: Class<out V?>?, value: V?): ClassParameter<V?> {
      return ClassParameter<V?>(clazz, value)
    }

    fun fromComponentLists(
      classes: Array<Class<*>?>,
      values: Array<Any?>
    ): Array<ClassParameter<*>?> {
      val classParameters = arrayOfNulls<ClassParameter<*>>(classes.size)
      for (i in classes.indices) {
        classParameters[i] = from<Any?>(classes[i], values[i])
      }
      return classParameters
    }

    fun getClasses(vararg classParameters: ClassParameter<*>?): Array<Class<*>?> {
      val classes = arrayOfNulls<Class<*>>(classParameters.size)
      for (i in classParameters.indices) {
        val paramClass: Class<*>? = classParameters[i]!!.clazz
        classes[i] = paramClass
      }
      return classes
    }

    fun getValues(vararg classParameters: ClassParameter<*>?): Array<Any?> {
      val values = arrayOfNulls<Any>(classParameters.size)
      for (i in classParameters.indices) {
        val paramValue: Any? = classParameters[i]!!.value
        values[i] = paramValue
      }
      return values
    }
  }
}