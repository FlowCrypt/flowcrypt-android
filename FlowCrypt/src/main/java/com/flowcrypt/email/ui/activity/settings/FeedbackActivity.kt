/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageButton
import androidx.constraintlayout.widget.Group
import com.flowcrypt.email.R
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.ui.activity.base.BaseBackStackSyncActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil

/**
 * The feedback activity. Anywhere there is a question mark, it should take the user to this
 * screen.
 *
 * @author DenBond7
 * Date: 30.05.2017
 * Time: 9:56
 * E-mail: DenBond7@gmail.com
 */

class FeedbackActivity : BaseBackStackSyncActivity(), CompoundButton.OnCheckedChangeListener {
  override lateinit var rootView: View
  private lateinit var editTextUserMsg: EditText
  private lateinit var imageButtonScreenshot: ImageButton
  private lateinit var screenShotGroup: Group
  private lateinit var checkBoxScreenshot: CheckBox

  private var account: AccountDao? = null
  private var bitmap: Bitmap? = null

  override val contentViewResourceId: Int
    get() = R.layout.activity_feedback

  private val isInformationValid: Boolean
    get() {
      return if (TextUtils.isEmpty(editTextUserMsg.text.toString())) {
        UIUtil.showInfoSnackbar(editTextUserMsg, getString(R.string.your_message_must_be_non_empty))
        false
      } else {
        true
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    account = intent?.getParcelableExtra(KEY_ACCOUNT)
    val bitmapRaw = intent?.getByteArrayExtra(KEY_BITMAP)
    bitmapRaw?.let { bitmap = BitmapFactory.decodeByteArray(bitmapRaw, 0, bitmapRaw.size) }

    initViews()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.activity_feedback, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menuActionSend -> {
        return true
      }

      else -> return super.onOptionsItemSelected(item)
    }
  }

  override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
    buttonView?.let {
      when (buttonView.id) {
        R.id.checkBoxScreenshot -> {
          screenShotGroup.visibility = if (isChecked) {
            View.VISIBLE
          } else {
            View.GONE
          }
        }
      }
    }
  }

  private fun initViews() {
    editTextUserMsg = findViewById(R.id.editTextUserMessage)
    imageButtonScreenshot = findViewById(R.id.imageButtonScreenshot)
    rootView = findViewById(R.id.layoutContent)
    screenShotGroup = findViewById(R.id.screenShotGroup)
    checkBoxScreenshot = findViewById(R.id.checkBoxScreenshot)
    checkBoxScreenshot.setOnCheckedChangeListener(this)

    bitmap?.let { imageButtonScreenshot.setImageBitmap(it) }
  }

  companion object {
    private val KEY_ACCOUNT =
        GeneralUtil.generateUniqueExtraKey("KEY_ACCOUNT", FeedbackActivity::class.java)
    private val KEY_BITMAP =
        GeneralUtil.generateUniqueExtraKey("KEY_BITMAP", FeedbackActivity::class.java)

    fun show(activity: Activity) {
      val account = AccountDaoSource().getActiveAccountInformation(activity)
      val screenShotByteArray = UIUtil.getScreenShotByteArray(activity)
      val intent = Intent(activity, FeedbackActivity::class.java)
      intent.putExtra(KEY_ACCOUNT, account)
      intent.putExtra(KEY_BITMAP, screenShotByteArray)
      activity.startActivity(intent)
    }
  }
}
