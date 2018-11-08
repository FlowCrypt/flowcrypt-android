/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules;

import android.net.Uri;

import com.flowcrypt.email.database.provider.FlowcryptContract;
import com.flowcrypt.email.js.JsForUiManager;
import com.flowcrypt.email.util.FileAndDirectoryUtils;
import com.flowcrypt.email.util.SharedPreferencesHelper;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;

import androidx.test.InstrumentationRegistry;
import androidx.test.internal.runner.junit4.statement.UiThreadStatement;

/**
 * The rule which clears the application settings.
 *
 * @author Denis Bondarenko
 * Date: 27.12.2017
 * Time: 11:57
 * E-mail: DenBond7@gmail.com
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
  private void clearApp() throws Throwable {
    SharedPreferencesHelper.clear(InstrumentationRegistry.getTargetContext());
    FileAndDirectoryUtils.cleanDirectory(InstrumentationRegistry.getTargetContext().getCacheDir());
    InstrumentationRegistry.getTargetContext().getContentResolver().delete(Uri.parse(FlowcryptContract
        .AUTHORITY_URI + "/" + FlowcryptContract.ERASE_DATABASE), null, null);
    UiThreadStatement.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        JsForUiManager.getInstance(InstrumentationRegistry.getTargetContext())
            .getJs()
            .getStorageConnector()
            .refresh(InstrumentationRegistry.getTargetContext());
      }
    });
    Thread.sleep(1000);// Added timeout for a better sync between threads.
  }
}
