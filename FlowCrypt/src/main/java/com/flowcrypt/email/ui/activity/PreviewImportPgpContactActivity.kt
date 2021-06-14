/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity
import com.flowcrypt.email.ui.activity.fragment.PreviewImportPgpContactFragment
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity
import com.flowcrypt.email.util.GeneralUtil

/**
 * This activity displays information about public keys owners and information about keys.
 *
 * @author Denis Bondarenko
 * Date: 10.05.2018
 * Time: 18:01
 * E-mail: DenBond7@gmail.com
 */
class PreviewImportPgpContactActivity : BaseBackStackActivity() {

  override val contentViewResourceId: Int
    get() = R.layout.activity_preview_import_pgp_contact

  override val rootView: View
    get() = findViewById(R.id.layoutContent)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (intent == null || !intent.hasExtra(KEY_EXTRA_PUBLIC_KEY_STRING)
      && !intent.hasExtra(KEY_EXTRA_PUBLIC_KEYS_FILE_URI)
    ) {
      setResult(Activity.RESULT_CANCELED)
      finish()
    }

    val publicKeysString = intent.getStringExtra(KEY_EXTRA_PUBLIC_KEY_STRING)
    val publicKeysFileUri = intent.getParcelableExtra<Uri>(KEY_EXTRA_PUBLIC_KEYS_FILE_URI)

    val fragmentManager = supportFragmentManager
    var fragment =
      fragmentManager.findFragmentById(R.id.layoutContent) as PreviewImportPgpContactFragment?

    if (fragment == null) {
      fragment = PreviewImportPgpContactFragment.newInstance(publicKeysString, publicKeysFileUri)
      fragmentManager.beginTransaction().add(R.id.layoutContent, fragment).commit()
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.activity_preview_import_pgp_contact, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menuActionHelp -> {
        FeedbackActivity.show(this)
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }

  companion object {
    val KEY_EXTRA_PUBLIC_KEY_STRING =
      GeneralUtil.generateUniqueExtraKey(
        "KEY_EXTRA_PUBLIC_KEY_STRING",
        PreviewImportPgpContactActivity::class.java
      )

    val KEY_EXTRA_PUBLIC_KEYS_FILE_URI = GeneralUtil.generateUniqueExtraKey(
      "KEY_EXTRA_PUBLIC_KEYS_FILE_URI",
      PreviewImportPgpContactActivity::class.java
    )

    fun newIntent(context: Context, publicKeysString: String?): Intent {
      val intent = Intent(context, PreviewImportPgpContactActivity::class.java)
      intent.putExtra(KEY_EXTRA_PUBLIC_KEY_STRING, publicKeysString)
      return intent
    }

    fun newIntent(context: Context, publicKeysFileUri: Uri): Intent {
      val intent = Intent(context, PreviewImportPgpContactActivity::class.java)
      intent.putExtra(KEY_EXTRA_PUBLIC_KEYS_FILE_URI, publicKeysFileUri)
      return intent
    }
  }
}
