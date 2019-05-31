/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email;

/**
 * The application class for FlowCrypt. Base class for maintaining global application state. The development version.
 *
 * @author DenBond7
 * Date: 25.04.2017
 * Time: 11:34
 * E-mail: DenBond7@gmail.com
 */
public class FlowCryptApplication extends BaseApplication {

  /**
   * We disabled ACRA for faster development because ACRA doesn't allow us to use Instant Run.
   * <a href="https://github.com/ACRA/acra/wiki/Troubleshooting-Guide#applicationoncreate">It uses a separate process</a>
   */
  @Override
  public void initAcra() {
    //Don't modify it
  }

  /**
   * We disabled LeakCanary for faster development because LeakCanary doesn't allow us to use Instant Run.
   * <a href="https://github.com/square/leakcanary/wiki/FAQ#how-does-it-work">It uses a separate process</a>
   */
  @Override
  public void initLeakCanary() {
    //Don't modify it
  }
}
