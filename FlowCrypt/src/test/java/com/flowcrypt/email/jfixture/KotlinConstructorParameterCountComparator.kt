/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jfixture

import java.lang.reflect.Constructor

/**
 * @author Denys Bondarenko
 */
class KotlinConstructorParameterCountComparator : Comparator<Constructor<*>> {
  override fun compare(ctor1: Constructor<*>, ctor2: Constructor<*>): Int {
    val ctor1IsDefaultConstructorMarkerFound =
      ctor1.genericParameterTypes.any { it.typeName == "kotlin.jvm.internal.DefaultConstructorMarker" }
    val ctor2IsDefaultConstructorMarkerFound =
      ctor2.genericParameterTypes.any { it.typeName == "kotlin.jvm.internal.DefaultConstructorMarker" }

    val ctor1Parameters =
      if (ctor1IsDefaultConstructorMarkerFound) 0 else ctor1.genericParameterTypes.size
    val ctor2Parameters =
      if (ctor2IsDefaultConstructorMarkerFound) 0 else ctor2.genericParameterTypes.size
    return ctor1Parameters.compareTo(ctor2Parameters)
  }
}
