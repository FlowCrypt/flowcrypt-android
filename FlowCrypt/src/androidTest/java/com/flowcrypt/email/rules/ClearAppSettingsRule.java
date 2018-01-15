/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules;

import android.net.Uri;
import android.support.test.InstrumentationRegistry;

import com.flowcrypt.email.database.provider.FlowcryptContract;
import com.flowcrypt.email.util.SharedPreferencesHelper;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;

/**
 * The rule which clears the application settings.
 *
 * @author Denis Bondarenko
 *         Date: 27.12.2017
 *         Time: 11:57
 *         E-mail: DenBond7@gmail.com
 */

public class ClearAppSettingsRule implements TestRule {

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                clearApp();
                base.evaluate();
            }
        };
    }

    /**
     * Clear the all application settings.
     *
     * @throws IOException Different errors can be occurred.
     */
    private void clearApp() throws IOException {
        SharedPreferencesHelper.clear(InstrumentationRegistry.getTargetContext());
        FileUtils.cleanDirectory(InstrumentationRegistry.getTargetContext().getCacheDir());
        InstrumentationRegistry.getTargetContext().getContentResolver().delete(Uri.parse(FlowcryptContract
                .AUTHORITY_URI + "/" + FlowcryptContract.ERASE_DATABASE), null, null);
    }
}
