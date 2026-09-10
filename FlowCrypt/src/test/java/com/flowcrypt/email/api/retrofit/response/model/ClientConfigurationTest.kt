/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * @author Denys Bondarenko
 */
class ClientConfigurationTest {
  @Test
  fun testGetDisallowPasswordMessagesForTermsRegex() {
    val clientConfiguration = ClientConfiguration(
      disallowPasswordMessagesForTerms = listOf(
        "droid",
        "test-feature",
        "[Classification: Data Control: Internal Data Control]",
      )
    )

    val regex = requireNotNull(clientConfiguration.getDisallowPasswordMessagesForTermsRegex())
    println(regex)

    val matchingSubjects = listOf(
      "[Classification: Data Control: Internal Data Control] Quarter results",
      "Conference information [Classification: Data Control: Internal Data Control]",
      "[Classification: Data Control: Internal Data Control]",
      "aaaa[Classification: Data Control: Internal Data Control]bbb",
      "[droid]",
      "check -droid- case",
      "TEST-FEATURE",
      "[test-feature}",
      "before {test-feature}",
      "[test-feature] after",
      "before [test-feature} after",
      "test-feature",
      "test-feature after",
      "before test-feature",
      "before test-feature after",
      "before TEST-feature after",
      "before {TEST-feature} after"
    )

    val nonMatchingSubjects = listOf(
      "[1Classification: Data Control: Internal Data Control] Quarter results",
      "Conference information [1Classification: Data Control: Internal Data Control]",
      "[1Classification: Data Control: Internal Data Control]",
      "aaaa[1Classification: Data Control: Internal Data Control]bbb",
      "Microdroid androids",
      "beforetest-feature",
      "test-featureafter",
      "beforetest-featureafter",
      "before {TEST-feature}after",
      "before{TEST-feature} after",
      "before{TEST-feature}after"
    )
    matchingSubjects.forEach { subject ->
      assertNotNull("Exception in :$subject", regex.find(subject))
    }

    nonMatchingSubjects.forEach { subject ->
      assertNull("Exception in :$subject", regex.find(subject))
    }
  }
}