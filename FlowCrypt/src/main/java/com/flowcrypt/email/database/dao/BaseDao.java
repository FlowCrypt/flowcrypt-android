/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/tree/master/src/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao;

import android.os.Parcelable;

import com.flowcrypt.email.database.dao.source.BaseDaoSource;


/**
 * The base DAO class.
 *
 * @author DenBond7
 *         Date: 13.05.2017
 *         Time: 12:48
 *         E-mail: DenBond7@gmail.com
 */

public abstract class BaseDao implements Parcelable {
    public abstract BaseDaoSource getDaoSource();
}
