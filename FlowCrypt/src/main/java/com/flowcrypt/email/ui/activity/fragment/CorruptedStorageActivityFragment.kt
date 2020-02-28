/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.CorruptedStorageActivity

/**
 * It's a root fragment of [CorruptedStorageActivity]
 *
 * @author DenBond7
 * Date: 12/14/2018
 * Time: 12:20
 * E-mail: DenBond7@gmail.com
 */
class CorruptedStorageActivityFragment : Fragment(), View.OnClickListener {

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_corrupted_storage, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val textViewHeader = view.findViewById<TextView>(R.id.textViewHeader)
    textViewHeader.text = getString(R.string.store_space_was_corrupted, getString(R.string.support_email))

    val textViewFooter = view.findViewById<TextView>(R.id.textViewFooter)
    textViewFooter.text = getString(R.string.wipe_app_settings, getString(R.string.app_name))

    val btnResetAppSettings = view.findViewById<View>(R.id.btnResetAppSettings)
    btnResetAppSettings?.setOnClickListener(this)
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.btnResetAppSettings -> {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", requireActivity().packageName, null)
        intent.data = uri
        startActivity(intent)
      }
    }
  }
}
