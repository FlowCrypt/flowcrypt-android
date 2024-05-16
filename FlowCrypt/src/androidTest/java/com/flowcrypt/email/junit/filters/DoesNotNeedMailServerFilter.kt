/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.junit.filters

import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.junit.annotations.EnterpriseTest
import org.junit.runner.Description

/**
 * @author Denys Bondarenko
 */
class DoesNotNeedMailServerFilter : ReadyForCIAndNonFlakyFilter() {
  override fun evaluateTest(description: Description?): Boolean {
    val annotationClassDependsOnMailServer = DependsOnMailServer::class.java
    val annotationClassEnterpriseTest = EnterpriseTest::class.java
    return super.evaluateTest(description)
        && !isAnnotationPresentAtClassOrMethod(description, annotationClassDependsOnMailServer)
        && !isAnnotationPresentAtClassOrMethod(description, annotationClassEnterpriseTest)
  }

  override fun describe() = "Filter tests that don't need an email server"
}
