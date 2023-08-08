/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.CheckKeysFragment
import com.flowcrypt.email.ui.activity.fragment.CheckKeysFragmentArgs
import com.flowcrypt.email.util.PrivateKeysManager
import org.apache.commons.io.FilenameUtils
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.UUID

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class CheckKeysFragmentMultiBackupsInIsolationTest : BaseTest() {
  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(ScreenshotTestRule())

  /**
   * There are two keys (all keys are different and have different pass phrases). Only one key from two keys is using.
   */
  @Test
  fun testUseTwoKeysFirstCombination() {
    val keysPaths = arrayOf(
      "pgp/key_testing@flowcrypt.test_keyA_strong.asc",
      "pgp/key_testing@flowcrypt.test_keyB_default.asc"
    )
    launchFragmentInContainerWithPredefinedArgs(keysPaths)

    checkKeysTitleAtStart(2)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
    checkKeysTitle(1, 2, 1)
    checkIsSkipRemainingBackupsButtonDisplayed()
  }

  /**
   * There are two keys (all keys are different and have different pass phrases). All keys are checking in the queue.
   */
  @Test
  fun testUseTwoKeysSecondCombination() {
    val keysPaths = arrayOf(
      "pgp/key_testing@flowcrypt.test_keyA_strong.asc",
      "pgp/key_testing@flowcrypt.test_keyB_default.asc"
    )
    launchFragmentInContainerWithPredefinedArgs(keysPaths)

    checkKeysTitleAtStart(2)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
    checkKeysTitle(1, 2, 1)
    typePassword(TestConstants.DEFAULT_PASSWORD)
  }

  /**
   * There are two keys with the same pass phrase. All keys will be imported per one transaction.
   */
  @Test
  fun testUseTwoKeysWithSamePasswordThirdCombination() {
    val keysPaths = arrayOf(
      "pgp/key_testing@flowcrypt.test_keyA_strong.asc",
      "pgp/key_testing@flowcrypt.test_keyC_strong.asc"
    )
    launchFragmentInContainerWithPredefinedArgs(keysPaths)

    checkKeysTitleAtStart(2)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
  }

  /**
   * There are two keys (the identical keys with different pass phrases). A key will be imported using
   * [TestConstants.DEFAULT_PASSWORD].
   */
  @Test
  fun testUseTwoKeysFourthCombination() {
    val keysPaths = arrayOf(
      "pgp/key_testing@flowcrypt.test_keyC_default.asc",
      "pgp/key_testing@flowcrypt.test_keyC_strong.asc"
    )
    launchFragmentInContainerWithPredefinedArgs(keysPaths)

    checkKeysTitleAtStart(1)
    typePassword(TestConstants.DEFAULT_PASSWORD)
  }

  /**
   * There are two keys (the identical keys with different pass phrases). A key will be imported using
   * [TestConstants.DEFAULT_STRONG_PASSWORD]
   */
  @Test
  fun testUseTwoKeysFifthCombination() {
    val keysPaths = arrayOf(
      "pgp/key_testing@flowcrypt.test_keyC_default.asc",
      "pgp/key_testing@flowcrypt.test_keyC_strong.asc"
    )
    launchFragmentInContainerWithPredefinedArgs(keysPaths)

    checkKeysTitleAtStart(1)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
  }

  /**
   * There are three keys (all keys are different, two keys have the identical pass phrase). Will be used only one
   * key with a unique pass phrase.
   */
  @Test
  fun testUseThreeFirstCombination() {
    val keysPaths = arrayOf(
      "pgp/key_testing@flowcrypt.test_keyA_strong.asc",
      "pgp/key_testing@flowcrypt.test_keyB_default.asc",
      "pgp/key_testing@flowcrypt.test_keyC_default.asc"
    )
    launchFragmentInContainerWithPredefinedArgs(keysPaths)

    checkKeysTitleAtStart(3)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
    checkKeysTitle(1, 3, 2)
    checkIsSkipRemainingBackupsButtonDisplayed()
  }

  /**
   * There are three keys (all keys are different, two keys have the identical pass phrase). Will be used two keys
   * with the same pass phrase.
   */
  @Test
  fun testUseThreeKeysSecondCombination() {
    val keysPaths = arrayOf(
      "pgp/key_testing@flowcrypt.test_keyA_strong.asc",
      "pgp/key_testing@flowcrypt.test_keyB_default.asc",
      "pgp/key_testing@flowcrypt.test_keyC_strong.asc"
    )
    launchFragmentInContainerWithPredefinedArgs(keysPaths)

    checkKeysTitleAtStart(3)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
    checkKeysTitle(2, 3, 1)
    checkIsSkipRemainingBackupsButtonDisplayed()
  }

  /**
   * There are three keys (all keys are different, two keys have the identical pass phrase). First will be used a key
   * with a unique pass phrase, and then the remaining keys.
   */
  @Test
  fun testUseThreeKeysThirdCombination() {
    val keysPaths = arrayOf(
      "pgp/key_testing@flowcrypt.test_keyA_strong.asc",
      "pgp/key_testing@flowcrypt.test_keyB_default.asc",
      "pgp/key_testing@flowcrypt.test_keyC_default.asc"
    )
    launchFragmentInContainerWithPredefinedArgs(keysPaths)

    checkKeysTitleAtStart(3)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
    checkKeysTitle(1, 3, 2)
    typePassword(TestConstants.DEFAULT_PASSWORD)
  }

  /**
   * There are three keys (all keys are different, two keys have the identical pass phrase). First will be used two
   * keys with the same pass phrase, and then the remaining key.
   */
  @Test
  fun testUseThreeKeysFourthCombination() {
    val keysPaths = arrayOf(
      "pgp/key_testing@flowcrypt.test_keyA_strong.asc",
      "pgp/key_testing@flowcrypt.test_keyB_default.asc",
      "pgp/key_testing@flowcrypt.test_keyC_strong.asc"
    )
    launchFragmentInContainerWithPredefinedArgs(keysPaths)

    checkKeysTitleAtStart(3)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
    checkKeysTitle(2, 3, 1)
    typePassword(TestConstants.DEFAULT_PASSWORD)
  }

  /**
   * There are three keys (one unique and two identical, the unique key and the identical key have the same
   * pass phrase). Will be used one of the identical keys with a unique pass phrase.
   */
  @Test
  fun testUseThreeKeysFifthCombination() {
    val keysPaths = arrayOf(
      "pgp/key_testing@flowcrypt.test_keyB_default.asc",
      "pgp/key_testing@flowcrypt.test_keyC_default.asc",
      "pgp/key_testing@flowcrypt.test_keyC_strong.asc"
    )
    launchFragmentInContainerWithPredefinedArgs(keysPaths)

    checkKeysTitleAtStart(2)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
    checkKeysTitle(1, 2, 1)
    checkIsSkipRemainingBackupsButtonDisplayed()
  }

  /**
   * There are three keys (one unique and two identical, the unique key and the identical key have the same
   * pass phrase). All keys will be imported per one transaction using [TestConstants.DEFAULT_STRONG_PASSWORD].
   */
  @Test
  fun testUseThreeKeysSixthCombination() {
    val keysPaths = arrayOf(
      "pgp/key_testing@flowcrypt.test_keyA_strong.asc",
      "pgp/key_testing@flowcrypt.test_keyC_default.asc",
      "pgp/key_testing@flowcrypt.test_keyC_strong.asc"
    )
    launchFragmentInContainerWithPredefinedArgs(keysPaths)

    checkKeysTitleAtStart(2)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
  }

  /**
   * There are three keys (one unique and two identical, the unique key and the identical key have the same
   * pass phrase). First will be used one key of the identical keys with a unique passphrase, and then the other keys.
   */
  @Test
  fun testUseThreeKeysSeventhCombination() {
    val keysPaths = arrayOf(
      "pgp/key_testing@flowcrypt.test_keyB_default.asc",
      "pgp/key_testing@flowcrypt.test_keyC_default.asc",
      "pgp/key_testing@flowcrypt.test_keyC_strong.asc"
    )
    launchFragmentInContainerWithPredefinedArgs(keysPaths)

    checkKeysTitleAtStart(2)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
    checkKeysTitle(1, 2, 1)
    typePassword(TestConstants.DEFAULT_PASSWORD)
  }

  /**
   * There are four keys (three keys are different and one is the same as one of the other; identical keys have
   * different pass phrases; each of the keys with identical pass phrase are unique). Will be used only
   * two keys with the same pass phrase.
   */
  @Test
  fun testUseFourKeysFirstCombination() {
    val keysPaths = arrayOf(
      "pgp/key_testing@flowcrypt.test_keyA_strong.asc",
      "pgp/key_testing@flowcrypt.test_keyB_default.asc",
      "pgp/key_testing@flowcrypt.test_keyC_default.asc",
      "pgp/key_testing@flowcrypt.test_keyC_strong.asc"
    )
    launchFragmentInContainerWithPredefinedArgs(keysPaths)

    checkKeysTitleAtStart(3)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
    checkKeysTitle(2, 3, 1)
    checkIsSkipRemainingBackupsButtonDisplayed()
  }

  /**
   * There are four keys (three keys are different and one is the same as one of the other; identical keys have
   * different pass phrases; each of the keys with identical pass phrase are unique). Will be used all keys (two
   * keys per one pass phrase typing).
   */
  @Test
  fun testUseFourKeysSecondCombination() {
    val keysPaths = arrayOf(
      "pgp/key_testing@flowcrypt.test_keyA_strong.asc",
      "pgp/key_testing@flowcrypt.test_keyB_default.asc",
      "pgp/key_testing@flowcrypt.test_keyC_default.asc",
      "pgp/key_testing@flowcrypt.test_keyC_strong.asc"
    )
    launchFragmentInContainerWithPredefinedArgs(keysPaths)

    checkKeysTitleAtStart(3)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
    checkKeysTitle(2, 3, 1)
    typePassword(TestConstants.DEFAULT_PASSWORD)
  }

  /**
   * There one armored private key.
   */
  @Test
  fun testSubTitleSingleBinaryKeyFromFile() {
    val keysPaths = arrayOf("pgp/keys/single_prv_key_binary.key")
    launchFragmentInContainerWithPredefinedArgs(keysPaths, KeyImportDetails.SourceType.FILE)

    checkKeysTitleAtStart(1, keysPaths, KeyImportDetails.SourceType.FILE)
  }

  /**
   * Many binary private keys
   */
  @Test
  fun testSubTitleManyBinaryKeysFromFile() {
    val keysPaths = arrayOf("pgp/keys/10_prv_keys_binary.key")
    launchFragmentInContainerWithPredefinedArgs(keysPaths, KeyImportDetails.SourceType.FILE)

    checkKeysTitleAtStart(10, keysPaths, KeyImportDetails.SourceType.FILE)
  }

  /**
   * Many binary keys(private and public)
   */
  @Test
  fun testSubTitleManyBinaryKeysPrvAndPubFromFile() {
    val keysPaths = arrayOf("pgp/keys/10_prv_and_pub_keys_binary.key")
    launchFragmentInContainerWithPredefinedArgs(keysPaths, KeyImportDetails.SourceType.FILE)

    checkKeysTitleAtStart(5, keysPaths, KeyImportDetails.SourceType.FILE)
  }

  /**
   * There one armored private key.
   */
  @Test
  fun testSubTitleSingleArmoredKeyFromFile() {
    val keysPaths = arrayOf("pgp/keys/single_prv_key_armored.asc")
    launchFragmentInContainerWithPredefinedArgs(keysPaths, KeyImportDetails.SourceType.FILE)

    checkKeysTitleAtStart(1, keysPaths, KeyImportDetails.SourceType.FILE)
  }

  /**
   * Many armored private keys where each has own
   * -----BEGIN PGP PRIVATE KEY BLOCK-----...-----END PGP PRIVATE KEY BLOCK-----
   */
  @Test
  fun testSubTitleManyArmoredKeysFromFileOwnHeader() {
    val keysPaths = arrayOf("pgp/keys/10_prv_keys_armored_own_header.asc")
    launchFragmentInContainerWithPredefinedArgs(keysPaths, KeyImportDetails.SourceType.FILE)

    checkKeysTitleAtStart(10, keysPaths, KeyImportDetails.SourceType.FILE)
  }

  /**
   * Many armored private keys with a single
   * -----BEGIN PGP PRIVATE KEY BLOCK-----...-----END PGP PRIVATE KEY BLOCK-----
   */
  @Test
  fun testSubTitleManyArmoredKeysFromFileSingleHeader() {
    val keysPaths = arrayOf("pgp/keys/10_prv_keys_armored_single_header.asc")
    launchFragmentInContainerWithPredefinedArgs(keysPaths, KeyImportDetails.SourceType.FILE)

    checkKeysTitleAtStart(10, keysPaths, KeyImportDetails.SourceType.FILE)
  }

  /**
   * Many armored private keys where each has own
   * -----BEGIN PGP PRIVATE KEY BLOCK-----...-----END PGP PRIVATE KEY BLOCK-----.
   * Each of those blocks can have a different count of keys.
   */
  @Test
  fun testSubTitleManyArmoredKeysFromFileOwnWithSingleHeader() {
    val keysPaths = arrayOf("pgp/keys/10_prv_keys_armored_own_with_single_header.asc")
    launchFragmentInContainerWithPredefinedArgs(keysPaths, KeyImportDetails.SourceType.FILE)

    checkKeysTitleAtStart(10, keysPaths, KeyImportDetails.SourceType.FILE)
  }

  /**
   * Many armored keys(private or pub) where each has own
   * -----BEGIN PGP ... KEY BLOCK-----...-----END PGP ... KEY BLOCK-----
   */
  @Test
  fun testSubTitleManyArmoredPrvPubKeysFromFileOwnHeader() {
    val keysPaths = arrayOf("pgp/keys/10_prv_and_pub_keys_armored_own_header.asc")
    launchFragmentInContainerWithPredefinedArgs(keysPaths, KeyImportDetails.SourceType.FILE)

    checkKeysTitleAtStart(6, keysPaths, KeyImportDetails.SourceType.FILE)
  }

  /**
   * Many armored keys(private or pub) where each has own
   * -----BEGIN PGP ... KEY BLOCK-----...-----END PGP ... KEY BLOCK-----
   * Each of those blocks can have a different count of keys.
   */
  @Test
  fun testSubTitleManyArmoredPrvPubKeysFromFileOwnWithSingleHeaderdd() {
    val keysPaths = arrayOf("pgp/keys/10_prv_and_pub_keys_armored_own_with_single_header.asc")
    launchFragmentInContainerWithPredefinedArgs(keysPaths, KeyImportDetails.SourceType.FILE)

    checkKeysTitleAtStart(4, keysPaths, KeyImportDetails.SourceType.FILE)
  }

  private fun launchFragmentInContainerWithPredefinedArgs(
    keysPaths: Array<String>,
    sourceType: KeyImportDetails.SourceType = KeyImportDetails.SourceType.EMAIL
  ) {
    val keyDetailsList = PrivateKeysManager.getKeysFromAssets(keysPaths, true)

    val bottomTitle: String
    when (sourceType) {
      KeyImportDetails.SourceType.FILE -> {
        assert(keysPaths.size == 1)
        val fileName = FilenameUtils.getName(keysPaths.first())
        bottomTitle = getQuantityString(
          R.plurals.file_contains_some_amount_of_keys,
          keyDetailsList.size, fileName, keyDetailsList.size
        )
      }
      else -> {
        bottomTitle = getQuantityString(
          R.plurals.found_backup_of_your_account_key,
          keyDetailsList.size, keyDetailsList.size
        )
      }
    }

    launchFragmentInContainer<CheckKeysFragment>(
      fragmentArgs = CheckKeysFragmentArgs(
        requestKey = UUID.randomUUID().toString(),
        privateKeys = keyDetailsList.toTypedArray(),
        positiveBtnTitle = getTargetContext().getString(R.string.continue_),
        negativeBtnTitle = getTargetContext().getString(R.string.choose_another_key),
        subTitle = bottomTitle,
        initSubTitlePlurals = if (sourceType == KeyImportDetails.SourceType.FILE) {
          0
        } else {
          R.plurals.found_backup_of_your_account_key
        },
        sourceType = sourceType,
        isExtraImportOpt = sourceType != KeyImportDetails.SourceType.EMAIL
      ).toBundle()
    )
  }

  private fun checkIsSkipRemainingBackupsButtonDisplayed() {
    onView(withId(R.id.buttonSkipRemainingBackups))
      .check(matches(isDisplayed()))
  }

  /**
   * Type a password and click on the "CONTINUE" button.
   *
   * @param password The input password.
   */
  private fun typePassword(password: String) {
    onView(withId(R.id.editTextKeyPassword))
      .perform(scrollTo(), typeText(password), closeSoftKeyboard())
    onView(withId(R.id.buttonPositiveAction))
      .perform(scrollTo(), click())
  }

  private fun checkKeysTitle(
    quantityOfKeysUsed: Int,
    totalQuantityOfKeys: Int,
    quantityOfRemainingKeys: Int
  ) {
    onView(withId(R.id.textViewSubTitle))
      .check(matches(isDisplayed()))
      .check(
        matches(
          withText(
            getQuantityString(
              R.plurals.not_recovered_all_keys,
              quantityOfRemainingKeys,
              quantityOfKeysUsed,
              totalQuantityOfKeys,
              quantityOfRemainingKeys
            )
          )
        )
      )
  }

  private fun checkKeysTitleAtStart(
    expectedKeyCount: Int, keysPaths: Array<String>? = null,
    sourceType: KeyImportDetails.SourceType = KeyImportDetails.SourceType.EMAIL
  ) {
    val text: String
    when (sourceType) {
      KeyImportDetails.SourceType.FILE -> {
        assert(keysPaths?.size == 1)
        val fileName = FilenameUtils.getName(keysPaths?.first())
        text = getQuantityString(
          R.plurals.file_contains_some_amount_of_keys,
          expectedKeyCount, fileName, expectedKeyCount
        )
      }
      else -> {
        text = getQuantityString(
          R.plurals.found_backup_of_your_account_key,
          expectedKeyCount, expectedKeyCount
        )
      }
    }

    onView(withId(R.id.textViewSubTitle))
      .check(matches(isDisplayed()))
      .check(matches(withText(text)))
  }
}
