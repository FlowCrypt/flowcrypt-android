/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import com.flowcrypt.email.TestConstants;
import com.flowcrypt.email.js.core.Js;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;

/**
 * @author Denis Bondarenko
 * Date: 13.12.2017
 * Time: 15:01
 * E-mail: DenBond7@gmail.com
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class JsTest {

  @Test
  public void createKey() throws Exception {
    Js js = new Js(InstrumentationRegistry.getInstrumentation().getTargetContext(), null);
    PgpContact pgpContactMain = new PgpContact("default@denbond7.com", "FlowCrypt_C");
    PgpContact[] pgpContacts = new PgpContact[]{pgpContactMain};

    PgpKey pgpKey = js.crypto_key_create(pgpContacts, 2048, TestConstants.DEFAULT_STRONG_PASSWORD);
    System.out.println(pgpKey.armor());
    System.out.println(pgpKey.toPublic().armor());
  }

  @Test
  public void changePasswordOfKey() {
    /*Js js = new Js(InstrumentationRegistry.getInstrumentation().getTargetContext(), null);
    PgpKey pgpKey =
        js.crypto_key_read(readFileFromAssetsAsString(InstrumentationRegistry.getInstrumentation().getContext(),
            "pgp/default@denbond7.com_keyC_default.key"));
    js.crypto_key_decrypt(pgpKey, TestConstants.DEFAULT_PASSWORD);
    System.out.println(pgpKey.armor());
    System.out.println(pgpKey.toPublic().armor());

    pgpKey.encrypt(TestConstants.DEFAULT_STRONG_PASSWORD);
    System.out.println(pgpKey.armor());
    System.out.println(pgpKey.toPublic().armor());*/
  }
}
