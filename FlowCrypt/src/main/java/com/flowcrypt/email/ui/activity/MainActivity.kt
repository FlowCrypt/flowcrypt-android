/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */
package com.flowcrypt.email.ui.activity

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.flowcrypt.email.R
import com.flowcrypt.email.jetpack.viewmodel.LauncherViewModel
import com.flowcrypt.email.ui.activity.base.BaseActivity

/**
 * @author Denis Bondarenko
 * Date: 3/8/22
 * Time: 11:13 AM
 * E-mail: DenBond7@gmail.com
 */
class MainActivity : BaseActivity() {
  private val launcherViewModel: LauncherViewModel by viewModels()

  override val rootView: View
    get() = findViewById(R.id.fragmentContainerView)

  override val isDisplayHomeAsUpEnabled: Boolean
    get() = false

  override val contentViewResourceId: Int
    get() = R.layout.activity_main

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen().apply {
      setKeepOnScreenCondition {
        launcherViewModel.isLoadingStateFlow.value
      }
    }

    super.onCreate(savedInstanceState)
  }
}
