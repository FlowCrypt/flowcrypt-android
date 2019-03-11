/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js.core;

import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpKey;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * This test can be used for the public keys generation.
 * Before run it copy FlowCrypt/src/main/assets/js to FlowCrypt/src/test/resources
 *
 * @author Denis Bondarenko
 * Date: 3/7/19
 * Time: 11:58 AM
 * E-mail: DenBond7@gmail.com
 */
public class JsTest {
  private Js js;

  @Before
  public void setUp() throws Exception {
    js = new Js();
  }

  @Test
  public void createKey() throws IOException {
    File filePrv = new File("prv.asc");
    File filePub = new File("pub.asc");

    if (!filePrv.exists()) {
      filePrv.createNewFile();
    }

    if (!filePub.exists()) {
      filePub.createNewFile();
    }

    String userName = "key_C";
    String email = "key_testing@denbond7.com";

    PgpContact pgpContactMain = new PgpContact(email, userName);
    PgpContact[] pgpContacts = new PgpContact[]{pgpContactMain};

    PgpKey pgpKey = js.crypto_key_create(pgpContacts, 2048, "android");
    FileUtils.write(filePrv, pgpKey.armor(), StandardCharsets.UTF_8, false);
    FileUtils.write(filePub, pgpKey.toPublic().armor(), StandardCharsets.UTF_8, false);
    System.out.println("Done!");
  }

  @Test
  public void createKeys() throws IOException {
    File file = new File("pub_keys.asc");

    if (!file.exists()) {
      file.createNewFile();
    }

    int keysCount = 10;

    int[] keySizes = {2048};
    String[] domains = {"test.com", "example.com", "mail.com", "imap.com", "smtp.com"};

    for (int i = 0; i < keysCount; i++) {
      String userName = "user_" + new Random().nextInt(5000);
      String email = userName + "@" + domains[new Random().nextInt(domains.length)];

      PgpContact pgpContactMain = new PgpContact(email, userName);
      PgpContact[] pgpContacts = new PgpContact[]{pgpContactMain};

      PgpKey pgpKey = js.crypto_key_create(pgpContacts, keySizes[new Random().nextInt(keySizes.length)], "android");
      FileUtils.write(file, pgpKey.toPublic().armor(), StandardCharsets.UTF_8, true);
      System.out.println("Key = " + (i + 1));
    }
  }
}