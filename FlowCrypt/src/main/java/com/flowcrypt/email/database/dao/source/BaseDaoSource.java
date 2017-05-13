package com.flowcrypt.email.database.dao.source;

import android.net.Uri;
import android.provider.BaseColumns;

import com.flowcrypt.email.database.provider.FlowcryptContract;

/**
 * The base data source class.
 *
 * @author DenBond7
 *         Date: 13.05.2017
 *         Time: 14:52
 *         E-mail: DenBond7@gmail.com
 */

public abstract class BaseDaoSource implements BaseColumns {
    public static final String URI_PATH_GROUPED = "Grouped";

    public abstract String getTableName();

    public final String getRowsContentType() {
        return "vnd.android.cursor.dir/vnd." + FlowcryptContract.AUTHORITY + "." + getTableName();
    }

    public final String getSingleRowContentType() {
        return "vnd.android.cursor.item/vnd." + FlowcryptContract.AUTHORITY + "." + getTableName();
    }

    public Uri getBaseContentUri() {
        return Uri.parse(FlowcryptContract.AUTHORITY_URI + "/" + getTableName());
    }
}
