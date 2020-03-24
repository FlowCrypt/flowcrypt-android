/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.jetpack.viewmodel.CheckPrivateKeysViewModel
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.WebViewInfoDialogFragment
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.idling.SingleIdlingResources
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * This class describes checking the incoming private keys. As a result will be returned encrypted
 * keys.
 *
 * @author Denis Bondarenko
 * Date: 21.07.2017
 * Time: 9:59
 * E-mail: DenBond7@gmail.com
 */

class CheckKeysActivity : BaseNodeActivity(), View.OnClickListener {
  private var originalKeys: List<NodeKeyDetails>? = null
  private val unlockedKeys: ArrayList<NodeKeyDetails> = ArrayList()
  private val remainingKeys: ArrayList<NodeKeyDetails> = ArrayList()
  private var keyDetailsAndLongIdsMap: MutableMap<NodeKeyDetails, String>? = null
  private lateinit var checkPrivateKeysViewModel: CheckPrivateKeysViewModel
  @VisibleForTesting
  val idlingForKeyChecking: SingleIdlingResources = SingleIdlingResources()

  private var editTextKeyPassword: EditText? = null
  private var textViewSubTitle: TextView? = null
  private var progressBar: View? = null

  private var subTitle: String? = null
  private var positiveBtnTitle: String? = null
  private var negativeBtnTitle: String? = null
  private var uniqueKeysCount: Int = 0
  private var type: KeyDetails.Type? = null

  override val isDisplayHomeAsUpEnabled: Boolean
    get() = false

  override val contentViewResourceId: Int
    get() = R.layout.activity_check_keys

  override val rootView: View
    get() = findViewById(R.id.layoutContent)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (intent != null) {
      getExtras()

      if (originalKeys != null) {
        this.keyDetailsAndLongIdsMap = prepareMapFromKeyDetailsList(originalKeys)
        this.uniqueKeysCount = getUniqueKeysLongIdsCount(keyDetailsAndLongIdsMap)

        if (!intent.getBooleanExtra(KEY_EXTRA_IS_EXTRA_IMPORT_OPTION, false)) {
          removeAlreadyImportedKeys()
          this.uniqueKeysCount = getUniqueKeysLongIdsCount(keyDetailsAndLongIdsMap)
          this.originalKeys = ArrayList(keyDetailsAndLongIdsMap?.keys ?: emptyList())

          when (uniqueKeysCount) {
            0 -> {
              setResult(RESULT_NO_NEW_KEYS)
              finish()
            }

            1 -> {
              this.subTitle = resources.getQuantityString(
                  R.plurals.found_backup_of_your_account_key, uniqueKeysCount, uniqueKeysCount)
            }

            else -> {
              if (originalKeys?.size != keyDetailsAndLongIdsMap?.size) {
                val map = prepareMapFromKeyDetailsList(originalKeys)
                val remainingKeyCount = getUniqueKeysLongIdsCount(map)

                this.subTitle = resources.getQuantityString(R.plurals.not_recovered_all_keys, remainingKeyCount,
                    uniqueKeysCount - remainingKeyCount, uniqueKeysCount, remainingKeyCount)
              } else {
                this.subTitle = resources.getQuantityString(
                    R.plurals.found_backup_of_your_account_key, uniqueKeysCount, uniqueKeysCount)
              }
            }
          }
        }

        originalKeys?.let { remainingKeys.addAll(it) }
      } else {
        setResult(Activity.RESULT_CANCELED)
        finish()
      }
    } else {
      finish()
    }

    if (originalKeys?.isNotEmpty() == true) {
      initViews()
      setupCheckPrivateKeysViewModel()
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.buttonPositiveAction -> {
        UIUtil.hideSoftInput(this, editTextKeyPassword)
        if (TextUtils.isEmpty(editTextKeyPassword?.text.toString())) {
          showInfoSnackbar(editTextKeyPassword, getString(R.string.passphrase_must_be_non_empty))
        } else {
          snackBar?.dismiss()
          checkPrivateKeysViewModel.checkKeys(remainingKeys, listOf(editTextKeyPassword?.text.toString()))
        }
      }

      R.id.buttonSkipRemainingBackups -> {
        returnUnlockedKeys(RESULT_SKIP_REMAINING_KEYS)
      }

      R.id.buttonUseExistingKeys -> {
        setResult(RESULT_USE_EXISTING_KEYS)
        finish()
      }

      R.id.buttonNegativeAction -> {
        setResult(RESULT_NEGATIVE)
        finish()
      }

      R.id.imageButtonHint -> {
        val infoDialogFragment = InfoDialogFragment.newInstance(dialogMsg =
        getString(R.string.hint_when_found_keys_in_email))
        infoDialogFragment.show(supportFragmentManager, InfoDialogFragment::class.java.simpleName)
      }

      R.id.imageButtonPasswordHint -> try {
        val webViewInfoDialogFragment = WebViewInfoDialogFragment.newInstance("",
            IOUtils.toString(assets.open("html/forgotten_pass_phrase_hint.htm"), StandardCharsets.UTF_8))
        webViewInfoDialogFragment.show(supportFragmentManager, WebViewInfoDialogFragment::class.java.simpleName)
      } catch (e: IOException) {
        e.printStackTrace()
      }
    }
  }

  private fun getExtras() {
    this.originalKeys = intent.getParcelableArrayListExtra(KEY_EXTRA_PRIVATE_KEYS)
    this.type = intent.getParcelableExtra(KEY_EXTRA_TYPE)
    this.subTitle = intent.getStringExtra(KEY_EXTRA_SUB_TITLE)
    this.positiveBtnTitle = intent.getStringExtra(KEY_EXTRA_POSITIVE_BUTTON_TITLE)
    this.negativeBtnTitle = intent.getStringExtra(KEY_EXTRA_NEGATIVE_BUTTON_TITLE)
  }

  private fun initViews() {
    initButton(R.id.buttonPositiveAction, View.VISIBLE, positiveBtnTitle)
    initButton(R.id.buttonNegativeAction, View.VISIBLE, negativeBtnTitle)

    if (KeysStorageImpl.getInstance(application).hasKeys() && intent?.getBooleanExtra
        (KEY_EXTRA_IS_USE_EXISTING_KEYS_ENABLED, false) == true) {
      initButton(R.id.buttonUseExistingKeys, View.VISIBLE, getString(R.string.use_existing_keys))
    }

    val imageButtonHint = findViewById<View>(R.id.imageButtonHint)
    if (originalKeys?.isNotEmpty() == true && type === KeyDetails.Type.EMAIL) {
      imageButtonHint?.visibility = View.VISIBLE
      imageButtonHint?.setOnClickListener(this)
    } else {
      imageButtonHint?.visibility = View.GONE
    }

    findViewById<View>(R.id.imageButtonPasswordHint)?.setOnClickListener(this)

    textViewSubTitle = findViewById(R.id.textViewSubTitle)
    textViewSubTitle?.text = subTitle

    editTextKeyPassword = findViewById(R.id.editTextKeyPassword)
    progressBar = findViewById(R.id.progressBar)

    if (intent.getBooleanExtra(KEY_EXTRA_IS_EXTRA_IMPORT_OPTION, false)) {
      val textViewTitle = findViewById<TextView>(R.id.textViewTitle)
      textViewTitle.setText(R.string.import_private_key)
    }
  }

  private fun initButton(buttonViewId: Int, visibility: Int, text: String?) {
    val button = findViewById<Button>(buttonViewId)
    button?.visibility = visibility
    button?.text = text
    button?.setOnClickListener(this)
  }

  private fun setupCheckPrivateKeysViewModel() {
    checkPrivateKeysViewModel = ViewModelProvider(this).get(CheckPrivateKeysViewModel::class.java)
    val observer = Observer<Result<List<NodeKeyDetails>>> {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            idlingForKeyChecking.setIdleState(false)
            progressBar?.visibility = View.VISIBLE
          }

          else -> {
            idlingForKeyChecking.setIdleState(true)
            progressBar?.visibility = View.GONE
            when (it.status) {
              Result.Status.SUCCESS -> {
                val resultKeys = it.data ?: emptyList()
                val sessionUnlockedKeys = resultKeys.filter { keyDetails -> keyDetails.passphrase?.isNotEmpty() == true }
                if (sessionUnlockedKeys.isNotEmpty()) {
                  unlockedKeys.addAll(sessionUnlockedKeys)

                  for (key in sessionUnlockedKeys) {
                    remainingKeys.removeAll(remainingKeys.filter { details ->
                      (details.longId == key.longId)
                    })
                  }

                  if (remainingKeys.isNotEmpty()) {
                    initButton(R.id.buttonSkipRemainingBackups, View.VISIBLE, getString(R.string.skip_remaining_backups))
                    findViewById<View>(R.id.buttonUseExistingKeys)?.visibility = View.GONE
                    editTextKeyPassword!!.text = null
                    val mapOfRemainingBackups = prepareMapFromKeyDetailsList(remainingKeys)
                    val remainingKeyCount = getUniqueKeysLongIdsCount(mapOfRemainingBackups)

                    textViewSubTitle!!.text = resources.getQuantityString(
                        R.plurals.not_recovered_all_keys, remainingKeyCount,
                        uniqueKeysCount - remainingKeyCount, uniqueKeysCount, remainingKeyCount)
                  } else {
                    returnUnlockedKeys(Activity.RESULT_OK)
                  }

                } else {
                  if (resultKeys.size == 1) {
                    showInfoSnackbar(rootView, resultKeys.first().errorMsg)
                  } else {
                    showInfoSnackbar(rootView, getString(R.string.password_is_incorrect))
                  }
                }
              }

              Result.Status.ERROR -> {
                showInfoSnackbar(rootView, getString(R.string.unknown_error))
              }

              else -> {
              }
            }
          }
        }
      }
    }

    checkPrivateKeysViewModel.liveData.observe(this, observer)
  }

  private fun returnUnlockedKeys(resultCode: Int) {
    val intent = Intent()
    intent.putExtra(KEY_EXTRA_UNLOCKED_PRIVATE_KEYS, unlockedKeys)
    setResult(resultCode, intent)
    finish()
  }

  /**
   * Remove the already imported keys from the list of found backups.
   */
  private fun removeAlreadyImportedKeys() {
    val longIds = getUniqueKeysLongIds(keyDetailsAndLongIdsMap!!)
    val keysStorage = KeysStorageImpl.getInstance(this)

    for (longId in longIds) {
      if (keysStorage.getPgpPrivateKey(longId) != null) {
        val iterator = keyDetailsAndLongIdsMap!!.entries.iterator()
        while (iterator.hasNext()) {
          val entry = iterator.next()
          if (longId == entry.value) {
            iterator.remove()
          }
        }
      }
    }
  }

  /**
   * Get a count of unique longIds.
   *
   * @param mapOfKeyDetailsAndLongIds An input map of [NodeKeyDetails].
   * @return A count of unique longIds.
   */
  private fun getUniqueKeysLongIdsCount(mapOfKeyDetailsAndLongIds: Map<NodeKeyDetails, String>?): Int {
    return HashSet(mapOfKeyDetailsAndLongIds?.values ?: emptyList()).size
  }

  /**
   * Get a set of unique longIds.
   *
   * @param mapOfKeyDetailsAndLongIds An input map of [NodeKeyDetails].
   * @return A list of unique longIds.
   */
  private fun getUniqueKeysLongIds(mapOfKeyDetailsAndLongIds: Map<NodeKeyDetails, String>): Set<String> {
    return HashSet(mapOfKeyDetailsAndLongIds.values)
  }

  /**
   * Generate a map of incoming list of [NodeKeyDetails] objects where values will be a [NodeKeyDetails]
   * longId.
   *
   * @param keys An incoming list of [NodeKeyDetails] objects.
   * @return A generated map.
   */
  private fun prepareMapFromKeyDetailsList(keys: List<NodeKeyDetails>?): MutableMap<NodeKeyDetails, String> {
    val map = HashMap<NodeKeyDetails, String>()

    keys?.let {
      for (keyDetails in it) {
        map[keyDetails] = keyDetails.longId ?: ""
      }
    }
    return map
  }

  companion object {

    const val RESULT_NEGATIVE = 10
    const val RESULT_SKIP_REMAINING_KEYS = 11
    const val RESULT_USE_EXISTING_KEYS = 12
    const val RESULT_NO_NEW_KEYS = 13

    val KEY_EXTRA_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
        "KEY_EXTRA_PRIVATE_KEYS", CheckKeysActivity::class.java)
    val KEY_EXTRA_TYPE = GeneralUtil.generateUniqueExtraKey(
        "KEY_EXTRA_TYPE", CheckKeysActivity::class.java)
    val KEY_EXTRA_SUB_TITLE = GeneralUtil.generateUniqueExtraKey(
        "KEY_EXTRA_SUB_TITLE", CheckKeysActivity::class.java)
    val KEY_EXTRA_POSITIVE_BUTTON_TITLE = GeneralUtil.generateUniqueExtraKey(
        "KEY_EXTRA_POSITIVE_BUTTON_TITLE", CheckKeysActivity::class.java)
    val KEY_EXTRA_NEGATIVE_BUTTON_TITLE =
        GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_NEGATIVE_BUTTON_TITLE", CheckKeysActivity::class.java)
    val KEY_EXTRA_IS_EXTRA_IMPORT_OPTION =
        GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_IS_EXTRA_IMPORT_OPTION", CheckKeysActivity::class.java)
    val KEY_EXTRA_UNLOCKED_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
        "KEY_EXTRA_UNLOCKED_PRIVATE_KEYS", CheckKeysActivity::class.java)

    val KEY_EXTRA_IS_USE_EXISTING_KEYS_ENABLED = GeneralUtil.generateUniqueExtraKey(
        "KEY_EXTRA_IS_USE_EXISTING_KEYS_ENABLED", CheckKeysActivity::class.java)

    fun newIntent(context: Context, privateKeys: ArrayList<NodeKeyDetails>,
                  type: KeyDetails.Type? = null, subTitle: String? = null, positiveBtnTitle:
                  String? = null, negativeBtnTitle: String? = null,
                  isExtraImportOpt: Boolean = false, isUseExistingKeysEnabled: Boolean = true): Intent {
      val intent = Intent(context, CheckKeysActivity::class.java)
      intent.putExtra(KEY_EXTRA_PRIVATE_KEYS, privateKeys)
      intent.putExtra(KEY_EXTRA_TYPE, type as Parcelable)
      intent.putExtra(KEY_EXTRA_SUB_TITLE, subTitle)
      intent.putExtra(KEY_EXTRA_POSITIVE_BUTTON_TITLE, positiveBtnTitle)
      intent.putExtra(KEY_EXTRA_NEGATIVE_BUTTON_TITLE, negativeBtnTitle)
      intent.putExtra(KEY_EXTRA_IS_EXTRA_IMPORT_OPTION, isExtraImportOpt)
      intent.putExtra(KEY_EXTRA_IS_USE_EXISTING_KEYS_ENABLED, isUseExistingKeysEnabled)
      return intent
    }
  }
}
