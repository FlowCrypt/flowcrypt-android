/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jfixture

import com.flextrade.jfixture.ConstructorQuery
import com.flextrade.jfixture.DefaultConstructorQuery
import com.flextrade.jfixture.JFixture
import com.flextrade.jfixture.SpecimenBuilder
import com.flextrade.jfixture.builders.ClassToConstructorRelay
import com.flextrade.jfixture.customisation.Customisation
import com.flextrade.jfixture.specifications.SpecificTypeSpecification
import com.flextrade.jfixture.utility.SpecimenType
import com.flextrade.jfixture.utility.comparators.InverseComparator
import java.lang.reflect.Type

/**
 * @author Denys Bondarenko
 */
class SelectConstructorCustomisation(type: Type?) : Customisation {
  private val type: SpecimenType<*> = SpecimenType.of(type)
  private val mostParameterCountConstructorQuery: ConstructorQuery

  init {
    mostParameterCountConstructorQuery =
      DefaultConstructorQuery(InverseComparator(KotlinConstructorParameterCountComparator()))
  }

  override fun customise(fixture: JFixture) {
    val greedyConstructorRelay: SpecimenBuilder =
      ClassToConstructorRelay(mostParameterCountConstructorQuery, SpecificTypeSpecification(type))
    fixture.addBuilderToStartOfPipeline(greedyConstructorRelay)
  }
}
