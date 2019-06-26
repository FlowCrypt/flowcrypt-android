/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.WebViewInfoDialogFragment
import com.flowcrypt.email.ui.loader.EncryptAndSavePrivateKeysAsyncTaskLoader
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.google.android.gms.common.util.CollectionUtils
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * This class describes checking the received private keys. Here we validate and save encrypted
 * via [KeyStoreCryptoManager] keys to the database. If one of received private keys is
 * valid, we will return [Activity.RESULT_OK].
 *
 * @author Denis Bondarenko
 * Date: 21.07.2017
 * Time: 9:59
 * E-mail: DenBond7@gmail.com
 */

class CheckKeysActivity : BaseNodeActivity(), View.OnClickListener, LoaderManager.LoaderCallbacks<LoaderResult> {

  private var keyDetailsList: ArrayList<NodeKeyDetails>? = null
  private var keyDetailsAndLongIdsMap: MutableMap<NodeKeyDetails, String>? = null

  private var editTextKeyPassword: EditText? = null
  private var textViewSubTitle: TextView? = null
  private var progressBar: View? = null

  private var subTitle: String? = null
  private var positiveBtnTitle: String? = null
  private var neutralBtnTitle: String? = null
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

      if (keyDetailsList != null) {
        this.keyDetailsAndLongIdsMap = prepareMapFromKeyDetailsList(keyDetailsList!!)
        this.uniqueKeysCount = getUniqueKeysLongIdsCount(keyDetailsAndLongIdsMap!!)

        if (!intent.getBooleanExtra(KEY_EXTRA_IS_EXTRA_IMPORT_OPTION, false)) {
          removeAlreadyImportedKeys()

          if (keyDetailsList!!.size != keyDetailsAndLongIdsMap!!.size && uniqueKeysCount > 1) {
            this.keyDetailsList = ArrayList(keyDetailsAndLongIdsMap!!.keys)
            if (keyDetailsList!!.isEmpty()) {
              setResult(Activity.RESULT_OK)
              finish()
            } else {
              val map = prepareMapFromKeyDetailsList(keyDetailsList!!)
              val remainingKeyCount = getUniqueKeysLongIdsCount(map)

              this.subTitle = resources.getQuantityString(R.plurals.not_recovered_all_keys, remainingKeyCount,
                  uniqueKeysCount - remainingKeyCount, uniqueKeysCount, remainingKeyCount)
            }
          } else {
            this.subTitle = resources.getQuantityString(
                R.plurals.found_backup_of_your_account_key, uniqueKeysCount, uniqueKeysCount)
          }
        }
      } else {
        setResult(Activity.RESULT_CANCELED)
        finish()
      }
    } else {
      finish()
    }

    if (keyDetailsList!!.isNotEmpty()) {
      initViews()
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.buttonPositiveAction -> {
        UIUtil.hideSoftInput(this, editTextKeyPassword)
        if (!CollectionUtils.isEmpty(keyDetailsList)) {
          if (TextUtils.isEmpty(editTextKeyPassword!!.text.toString())) {
            showInfoSnackbar(editTextKeyPassword!!, getString(R.string.passphrase_must_be_non_empty))
          } else {
            if (snackBar != null) {
              snackBar!!.dismiss()
            }

            LoaderManager.getInstance(this).restartLoader(R.id.loader_id_encrypt_and_save_private_keys_infos, null,
                this)
          }
        }
      }

      R.id.buttonNeutralAction -> {
        setResult(RESULT_NEUTRAL)
        finish()
      }

      R.id.buttonNegativeAction -> {
        setResult(RESULT_NEGATIVE)
        finish()
      }

      R.id.imageButtonHint -> {
        val infoDialogFragment = InfoDialogFragment.newInstance("",
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

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<LoaderResult> {
    return when (id) {
      R.id.loader_id_encrypt_and_save_private_keys_infos -> {
        progressBar!!.visibility = View.VISIBLE
        val passphrase = editTextKeyPassword!!.text.toString()
        EncryptAndSavePrivateKeysAsyncTaskLoader(this, keyDetailsList!!, type!!, passphrase)
      }

      else -> Loader(this)
    }
  }

  override fun onLoadFinished(loader: Loader<LoaderResult>, loaderResult: LoaderResult) {
    handleLoaderResult(loader, loaderResult)
  }

  override fun onLoaderReset(loader: Loader<LoaderResult>) {

  }

  override fun onError(loaderId: Int, e: Exception?) {
    when (loaderId) {
      R.id.loader_id_encrypt_and_save_private_keys_infos -> {
        progressBar!!.visibility = View.GONE
        showInfoSnackbar(rootView, if (TextUtils.isEmpty(e!!.message))
          getString(R.string.can_not_read_this_private_key)
        else
          e.message)
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun onSuccess(loaderId: Int, result: Any?) {
    when (loaderId) {
      R.id.loader_id_encrypt_and_save_private_keys_infos -> {
        progressBar!!.visibility = View.GONE
        val savedKeyDetailsList = result as ArrayList<NodeKeyDetails>?
        if (savedKeyDetailsList != null && savedKeyDetailsList.isNotEmpty()) {
          KeysStorageImpl.getInstance(this).refresh(this)

          val map = prepareMapFromKeyDetailsList(savedKeyDetailsList)
          keyDetailsList!!.removeAll(generateMatchedKeyDetailsList(map))
          if (keyDetailsList!!.isEmpty()) {
            setResult(Activity.RESULT_OK)
            finish()
          } else {
            initButton(R.id.buttonNeutralAction, View.VISIBLE, getString(R.string.skip_remaining_backups))
            editTextKeyPassword!!.text = null
            val mapOfRemainingBackups = prepareMapFromKeyDetailsList(keyDetailsList!!)
            val remainingKeyCount = getUniqueKeysLongIdsCount(mapOfRemainingBackups)

            textViewSubTitle!!.text = resources.getQuantityString(
                R.plurals.not_recovered_all_keys, remainingKeyCount,
                uniqueKeysCount - remainingKeyCount,
                uniqueKeysCount, remainingKeyCount)
          }
        } else {
          showInfoSnackbar(rootView, getString(R.string.password_is_incorrect))
        }
      }
    }
  }

  private fun getExtras() {
    this.keyDetailsList = intent.getParcelableArrayListExtra(KEY_EXTRA_PRIVATE_KEYS)
    this.type = intent.getParcelableExtra(KEY_EXTRA_TYPE)
    this.subTitle = intent.getStringExtra(KEY_EXTRA_SUB_TITLE)
    this.positiveBtnTitle = intent.getStringExtra(KEY_EXTRA_POSITIVE_BUTTON_TITLE)
    this.neutralBtnTitle = intent.getStringExtra(KEY_EXTRA_NEUTRAL_BUTTON_TITLE)
    this.negativeBtnTitle = intent.getStringExtra(KEY_EXTRA_NEGATIVE_BUTTON_TITLE)
  }

  private fun initViews() {
    if (findViewById<View>(R.id.buttonPositiveAction) != null) {
      initButton(R.id.buttonPositiveAction, View.VISIBLE, positiveBtnTitle)
    }

    if (!TextUtils.isEmpty(neutralBtnTitle) && findViewById<View>(R.id.buttonNeutralAction) != null) {
      initButton(R.id.buttonNeutralAction, View.VISIBLE, neutralBtnTitle)
    }

    if (findViewById<View>(R.id.buttonNegativeAction) != null) {
      initButton(R.id.buttonNegativeAction, View.VISIBLE, negativeBtnTitle)
    }

    if (findViewById<View>(R.id.imageButtonHint) != null) {
      val imageButtonHint = findViewById<View>(R.id.imageButtonHint)
      if (keyDetailsList != null && keyDetailsList!!.isNotEmpty() && type === KeyDetails.Type.EMAIL) {
        imageButtonHint.visibility = View.VISIBLE
        imageButtonHint.setOnClickListener(this)
      } else {
        imageButtonHint.visibility = View.GONE
      }
    }

    if (findViewById<View>(R.id.imageButtonPasswordHint) != null) {
      findViewById<View>(R.id.imageButtonPasswordHint).setOnClickListener(this)
    }

    textViewSubTitle = findViewById(R.id.textViewSubTitle)
    if (textViewSubTitle != null) {
      textViewSubTitle!!.text = subTitle
    }

    editTextKeyPassword = findViewById(R.id.editTextKeyPassword)
    progressBar = findViewById(R.id.progressBar)

    if (intent.getBooleanExtra(KEY_EXTRA_IS_EXTRA_IMPORT_OPTION, false)) {
      val textViewTitle = findViewById<TextView>(R.id.textViewTitle)
      textViewTitle.setText(R.string.import_private_key)
    }
  }

  private fun initButton(buttonViewId: Int, visibility: Int, text: String?) {
    val buttonNeutralAction = findViewById<Button>(buttonViewId)
    buttonNeutralAction.visibility = visibility
    buttonNeutralAction.text = text
    buttonNeutralAction.setOnClickListener(this)
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
  private fun getUniqueKeysLongIdsCount(mapOfKeyDetailsAndLongIds: Map<NodeKeyDetails, String>): Int {
    return HashSet(mapOfKeyDetailsAndLongIds.values).size
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
   * @param privateKeyDetailsList An incoming list of [NodeKeyDetails] objects.
   * @return A generated map.
   */
  private fun prepareMapFromKeyDetailsList(privateKeyDetailsList: ArrayList<NodeKeyDetails>): MutableMap<NodeKeyDetails, String> {
    val keyDetailsStringMap = HashMap<NodeKeyDetails, String>()

    for (keyDetails in privateKeyDetailsList) {
      keyDetailsStringMap[keyDetails] = keyDetails.longId!!
    }
    return keyDetailsStringMap
  }

  /**
   * Generate a matched list of the existing keys. It will contain all [NodeKeyDetails] which has a right longId.
   *
   * @param mapOfSavedKeyDetailsAndLongIds An incoming map of [NodeKeyDetails] objects.
   * @return A matched list.
   */
  private fun generateMatchedKeyDetailsList(mapOfSavedKeyDetailsAndLongIds: Map<NodeKeyDetails, String>): ArrayList<NodeKeyDetails> {
    val matchedKeyDetails = ArrayList<NodeKeyDetails>()
    for ((_, value) in mapOfSavedKeyDetailsAndLongIds) {
      for ((key, value1) in keyDetailsAndLongIdsMap!!) {
        if (value1 == value) {
          matchedKeyDetails.add(key)
        }
      }
    }

    return matchedKeyDetails
  }

  companion object {

    const val RESULT_NEGATIVE = 10
    const val RESULT_NEUTRAL = 11

    val KEY_EXTRA_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
        "KEY_EXTRA_PRIVATE_KEYS", CheckKeysActivity::class.java)
    val KEY_EXTRA_TYPE = GeneralUtil.generateUniqueExtraKey(
        "KEY_EXTRA_TYPE", CheckKeysActivity::class.java)
    val KEY_EXTRA_SUB_TITLE = GeneralUtil.generateUniqueExtraKey(
        "KEY_EXTRA_SUB_TITLE", CheckKeysActivity::class.java)
    val KEY_EXTRA_POSITIVE_BUTTON_TITLE = GeneralUtil.generateUniqueExtraKey(
        "KEY_EXTRA_POSITIVE_BUTTON_TITLE", CheckKeysActivity::class.java)
    val KEY_EXTRA_NEUTRAL_BUTTON_TITLE = GeneralUtil.generateUniqueExtraKey(
        "KEY_EXTRA_NEUTRAL_BUTTON_TITLE", CheckKeysActivity::class.java)
    val KEY_EXTRA_NEGATIVE_BUTTON_TITLE = GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_NEGATIVE_BUTTON_TITLE", CheckKeysActivity::class.java)
    val KEY_EXTRA_IS_EXTRA_IMPORT_OPTION = GeneralUtil.generateUniqueExtraKey(
        "KEY_EXTRA_IS_EXTRA_IMPORT_OPTION", CheckKeysActivity::class.java)

    fun newIntent(context: Context, privateKeys: ArrayList<NodeKeyDetails>, type: KeyDetails.Type?,
                  bottomTitle: String?, positiveBtnTitle: String?, negativeBtnTitle: String?): Intent {
      return newIntent(context, privateKeys, type, bottomTitle, positiveBtnTitle, null, negativeBtnTitle, false)
    }

    fun newIntent(context: Context, privateKeys: ArrayList<NodeKeyDetails>, type: KeyDetails.Type?,
                  bottomTitle: String?, positiveBtnTitle: String?, neutralBtnTitle: String?,
                  negativeBtnTitle: String?): Intent {
      return newIntent(context, privateKeys, type, bottomTitle, positiveBtnTitle, neutralBtnTitle, negativeBtnTitle,
          false)
    }

    fun newIntent(context: Context, privateKeys: ArrayList<NodeKeyDetails>, type: KeyDetails.Type?,
                  subTitle: String?, positiveBtnTitle: String?, neutralBtnTitle: String?,
                  negativeBtnTitle: String?, isExtraImportOpt: Boolean): Intent {
      val intent = Intent(context, CheckKeysActivity::class.java)
      intent.putExtra(KEY_EXTRA_PRIVATE_KEYS, privateKeys)
      intent.putExtra(KEY_EXTRA_TYPE, type as Parcelable)
      intent.putExtra(KEY_EXTRA_SUB_TITLE, subTitle)
      intent.putExtra(KEY_EXTRA_POSITIVE_BUTTON_TITLE, positiveBtnTitle)
      intent.putExtra(KEY_EXTRA_NEUTRAL_BUTTON_TITLE, neutralBtnTitle)
      intent.putExtra(KEY_EXTRA_NEGATIVE_BUTTON_TITLE, negativeBtnTitle)
      intent.putExtra(KEY_EXTRA_IS_EXTRA_IMPORT_OPTION, isExtraImportOpt)
      return intent
    }
  }
}
