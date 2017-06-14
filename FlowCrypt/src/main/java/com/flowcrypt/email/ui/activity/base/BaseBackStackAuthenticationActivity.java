/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.view.MenuItem;

/**
 * The base back stack authentication activity.
 *
 * @author DenBond7
 *         Date: 09.05.2017
 *         Time: 16:27
 *         E-mail: DenBond7@gmail.com
 */

public abstract class BaseBackStackAuthenticationActivity extends BaseAuthenticationActivity {

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
