/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.listeners;

import java.util.ArrayList;

/**
 * This listener handles a situation when private keys selected from the file system.
 *
 * @author Denis Bondarenko
 * Date: 20.07.2017
 * Time: 17:30
 * E-mail: DenBond7@gmail.com
 */

public interface OnPrivateKeysSelectedListener {
  void onPrivateKeysSelected(ArrayList<String> privateKeys);
}
