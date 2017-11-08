/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source;

import android.net.Uri;
import android.provider.BaseColumns;

import com.flowcrypt.email.database.provider.FlowcryptContract;

import java.util.List;

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

    /**
     * Generate a selection {@link String} for a database query.
     *
     * @param strings The list of {@link String} objects for which need to generate a selection.
     * @return <tt>{@link String}</tt> A generated selection.
     */
    public String prepareSelection(List<String> strings) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("( ?");


        if (strings.size() > 1) {
            for (int i = 1; i < strings.size(); i++) {
                stringBuilder.append(", ?");
            }
        }

        stringBuilder.append(")");
        return stringBuilder.toString();
    }
}
