/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service;

import android.app.Service;

/**
 * The base {@link Service} class for a between threads communication.
 *
 * @author Denis Bondarenko
 *         Date: 16.02.2018
 *         Time: 16:30
 *         E-mail: DenBond7@gmail.com
 */

public abstract class BaseService extends Service {
    public static final int REPLY_OK = 0;
    public static final int REPLY_ERROR = 1;
    public static final int REPLY_ACTION_PROGRESS = 2;
}
