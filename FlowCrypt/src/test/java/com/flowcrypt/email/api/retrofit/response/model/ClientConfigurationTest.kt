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
        "[Classification: Data Control: Internal Data Control]",
      )
    )

    val regex = requireNotNull(clientConfiguration.getDisallowPasswordMessagesForTermsRegex())

    val matchingSubjects = listOf(
      "[Classification: Data Control: Internal Data Control] Quarter results",
      "Conference information [Classification: Data Control: Internal Data Control]",
      "[Classification: Data Control: Internal Data Control]",
      "aaaa[Classification: Data Control: Internal Data Control]bbb",
      "[droid]",
      "check -droid- case",
    )

    val nonMatchingSubjects = listOf(
      "[1Classification: Data Control: Internal Data Control] Quarter results",
      "Conference information [1Classification: Data Control: Internal Data Control]",
      "[1Classification: Data Control: Internal Data Control]",
      "aaaa[1Classification: Data Control: Internal Data Control]bbb",
      "Microdroid androids",
    )
    matchingSubjects.forEach { subject ->
      assertNotNull("Exception in :$subject", regex.find(subject))
    }

    nonMatchingSubjects.forEach { subject ->
      assertNull("Exception in :$subject", regex.find(subject))
    }
  }
}