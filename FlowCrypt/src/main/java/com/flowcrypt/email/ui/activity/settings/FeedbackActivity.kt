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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.Fragment
import com.flowcrypt.email.R
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.service.FeedbackJobIntentService
import com.flowcrypt.email.ui.activity.base.BaseBackStackSyncActivity
import com.flowcrypt.email.ui.activity.fragment.dialog.EditScreenshotDialogFragment
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.google.android.material.textfield.TextInputLayout

/**
 * The feedback activity. Anywhere there is a question mark, it should take the user to this
 * screen.
 *
 * @author DenBond7
 * Date: 30.05.2017
 * Time: 9:56
 * E-mail: DenBond7@gmail.com
 */

class FeedbackActivity : BaseBackStackSyncActivity(), CompoundButton.OnCheckedChangeListener,
    View.OnClickListener, EditScreenshotDialogFragment.OnScreenshotChangeListener {
  override lateinit var rootView: View
  private lateinit var inputLayoutMsg: TextInputLayout
  private lateinit var editTextUserMsg: EditText
  private lateinit var imageButtonScreenshot: ImageButton
  private lateinit var screenShotGroup: Group
  private lateinit var checkBoxScreenshot: CheckBox

  private var account: AccountDao? = null
  private var bitmapRaw: ByteArray? = null
  private var bitmap: Bitmap? = null

  override val contentViewResourceId: Int
    get() = R.layout.activity_feedback

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    account = intent?.getParcelableExtra(KEY_ACCOUNT)
    bitmapRaw = intent?.getByteArrayExtra(KEY_BITMAP)
    bitmapRaw?.let { bitmap = BitmapFactory.decodeByteArray(bitmapRaw, 0, bitmapRaw!!.size) }

    initViews()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.activity_feedback, menu)
    return true
  }

  override fun onAttachFragment(fragment: Fragment?) {
    super.onAttachFragment(fragment)

    if (fragment is EditScreenshotDialogFragment) {
      fragment.setOnScreenshotChangeListener(this)
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menuActionSend -> {
        if (editTextUserMsg.text.isEmpty()) {
          editTextUserMsg.requestFocus()
          Toast.makeText(this, getString(R.string.your_message_must_be_non_empty), Toast
              .LENGTH_SHORT).show()
        } else {
          val account = AccountDaoSource().getActiveAccountInformation(this)
          val screenShotBytes = UIUtil.getCompressedByteArrayOfBitmap(
              if (checkBoxScreenshot.isChecked) {
                bitmap
              } else {
                null
              }, 100)
          FeedbackJobIntentService.enqueueWork(this, account,
              editTextUserMsg.text.toString(), screenShotBytes)
          finish()
        }
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

  override fun onClick(v: View?) {
    when (v?.id) {
      R.id.imageButtonScreenshot -> {
        val editScreenShotDialogFragment = EditScreenshotDialogFragment.newInstance(bitmapRaw)
        editScreenShotDialogFragment.show(supportFragmentManager, EditScreenshotDialogFragment::class.java.simpleName)
      }
    }
  }

  override fun onScreenshotChanged(byteArray: ByteArray?) {
    bitmapRaw = byteArray
    bitmapRaw?.let { bitmap = BitmapFactory.decodeByteArray(bitmapRaw, 0, bitmapRaw!!.size) }
    bitmap?.let { imageButtonScreenshot.setImageBitmap(it) }
  }

  private fun initViews() {
    inputLayoutMsg = findViewById(R.id.textInputLayoutUserMessage)
    editTextUserMsg = findViewById(R.id.editTextUserMessage)
    imageButtonScreenshot = findViewById(R.id.imageButtonScreenshot)
    imageButtonScreenshot.setOnClickListener(this)
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
