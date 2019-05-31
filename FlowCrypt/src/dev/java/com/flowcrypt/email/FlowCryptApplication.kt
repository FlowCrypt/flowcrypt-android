/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email

/**
 * The application class for FlowCrypt. Base class for maintaining global application state. The development version.
 *
 * @author DenBond7
 * Date: 25.04.2017
 * Time: 11:34
 * E-mail: DenBond7@gmail.com
 */
class FlowCryptApplication : BaseApplication() {

  /**
   * We disabled ACRA for faster development because ACRA doesn't allow us to use Instant Run.
   * [It uses a separate process](https://github.com/ACRA/acra/wiki/Troubleshooting-Guide#applicationoncreate)
   */
  override fun initAcra() {
    //Don't modify it
  }

  /**
   * We disabled LeakCanary for faster development because LeakCanary doesn't allow us to use Instant Run.
   * [It uses a separate process](https://github.com/square/leakcanary/wiki/FAQ#how-does-it-work)
   */
  override fun initLeakCanary() {
    //Don't modify it
  }
}
