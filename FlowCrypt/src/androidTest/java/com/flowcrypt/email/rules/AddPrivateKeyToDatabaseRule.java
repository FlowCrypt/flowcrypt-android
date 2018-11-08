/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules;

import com.flowcrypt.email.TestConstants;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.util.TestGeneralUtil;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import androidx.test.InstrumentationRegistry;

/**
 * @author Denis Bondarenko
 * Date: 21.02.2018
 * Time: 17:54
 * E-mail: DenBond7@gmail.com
 */
public class AddPrivateKeyToDatabaseRule implements TestRule {

  private String keyPath;
  private String passphrase;
  private KeyDetails.Type keyDetailsType;

  public AddPrivateKeyToDatabaseRule(String keyPath, String passphrase, KeyDetails.Type keyDetailsType) {
    this.keyPath = keyPath;
    this.passphrase = passphrase;
    this.keyDetailsType = keyDetailsType;
  }

  public AddPrivateKeyToDatabaseRule() {
    this.keyPath = "pgp/default@denbond7.com_strong_password-sec.key";
    this.passphrase = TestConstants.DEFAULT_STRONG_PASSWORD;
    this.keyDetailsType = KeyDetails.Type.EMAIL;
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        TestGeneralUtil.saveKeyToDatabase(TestGeneralUtil.readFileFromAssetsAsString
            (InstrumentationRegistry.getContext(), keyPath), passphrase, keyDetailsType);
        base.evaluate();
      }
    };
  }
}
