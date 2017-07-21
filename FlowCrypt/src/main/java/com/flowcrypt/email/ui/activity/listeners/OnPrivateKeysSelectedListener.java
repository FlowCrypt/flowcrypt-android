/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.listeners;

import java.util.ArrayList;

/**
 * This listener handles a situation when private keys selected from the file system.
 *
 * @author Denis Bondarenko
 *         Date: 20.07.2017
 *         Time: 17:30
 *         E-mail: DenBond7@gmail.com
 */

public interface OnPrivateKeysSelectedListener {
    void onPrivateKeysSelected(ArrayList<String> privateKeys);
}
