/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.base;

import android.net.Uri;
import android.support.test.InstrumentationRegistry;

import com.flowcrypt.email.database.provider.FlowcryptContract;
import com.flowcrypt.email.util.SharedPreferencesHelper;

import org.apache.commons.io.FileUtils;

import java.io.IOException;

/**
 * The base test implementation.
 *
 * @author Denis Bondarenko
 *         Date: 26.12.2017
 *         Time: 16:37
 *         E-mail: DenBond7@gmail.com
 */

public class BaseTest {

    /**
     * Clear the all application settings.
     *
     * @param email The account email.
     * @throws IOException Different errors can be occurred.
     */
    protected void clearApp(String email) throws IOException {
        SharedPreferencesHelper.clear(InstrumentationRegistry.getTargetContext());
        FileUtils.cleanDirectory(InstrumentationRegistry.getTargetContext().getCacheDir());
        InstrumentationRegistry.getTargetContext().getContentResolver()
                .delete(Uri.parse(FlowcryptContract.AUTHORITY_URI + "/" + FlowcryptContract.CLEAN_DATABASE),
                        null, new String[]{email});
    }
}
