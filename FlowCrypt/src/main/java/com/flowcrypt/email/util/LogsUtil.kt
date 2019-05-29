/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import android.util.Log;

/**
 * @author Denis Bondarenko
 * Date: 4/26/19
 * Time: 9:33 PM
 * E-mail: DenBond7@gmail.com
 */
public class LogsUtil {
  public static void d(String tag, String msg) {
    if (GeneralUtil.isDebugBuild()) {
      Log.d(tag, msg);
    }
  }
}
