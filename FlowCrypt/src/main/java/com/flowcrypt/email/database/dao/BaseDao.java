/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
