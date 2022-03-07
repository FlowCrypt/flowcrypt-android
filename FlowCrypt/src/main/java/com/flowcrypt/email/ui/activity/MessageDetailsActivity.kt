/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.NavHostFragment
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.ui.activity.base.BaseBackStackSyncActivity
import com.flowcrypt.email.util.idling.SingleIdlingResources

/**
 * This activity describe details of some message.
 *
 * @author DenBond7
 * Date: 03.05.2017
 * Time: 16:29
 * E-mail: DenBond7@gmail.com
 */
class MessageDetailsActivity : BaseBackStackSyncActivity() {
  override val rootView: View
    get() = View(this)

  override val contentViewResourceId: Int
    get() = R.layout.activity_message_details

  val idlingForWebView: SingleIdlingResources = SingleIdlingResources(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    (supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as? NavHostFragment)
      ?.navController?.setGraph(R.navigation.msg_details_graph, intent.extras)
  }

  companion object {
    fun getIntent(context: Context?, localFolder: LocalFolder?, msgEntity: MessageEntity?): Intent {
      val intent = Intent(context, MessageDetailsActivity::class.java)
      intent.putExtra("localFolder", localFolder)
      intent.putExtra("messageEntity", msgEntity)
      return intent
    }
  }
}
