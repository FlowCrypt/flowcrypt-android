/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.flowcrypt.email.R

/**
 * This [android.app.Activity] handles situations when the app storage space was corrupted. This can
 * happen on certain rooted or unofficial systems, due to user intervention, etc.
 *
 * @author DenBond7
 * Date: 12/14/2018
 * Time: 12:20
 * E-mail: DenBond7@gmail.com
 */
class CorruptedStorageActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_corrupted_storage)

    val toolbar = findViewById<Toolbar>(R.id.toolbar)
    if (toolbar != null) {
      setSupportActionBar(toolbar)
    }
  }
}
