/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.view.MenuItem;

/**
 * The base back stack sync {@link android.app.Activity}
 *
 * @author DenBond7
 *         Date: 27.06.2017
 *         Time: 13:26
 *         E-mail: DenBond7@gmail.com
 */

public abstract class BaseBackStackSyncActivity extends BaseSyncActivity {
    @Override
    public boolean isDisplayHomeAsUpEnabled() {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }
}
