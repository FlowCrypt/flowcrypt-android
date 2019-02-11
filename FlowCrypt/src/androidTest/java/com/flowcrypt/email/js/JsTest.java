/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import android.content.Context;
import android.util.Log;

import com.flowcrypt.email.js.core.Js;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.util.FileAndDirectoryUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;

/**
 * @author Denis Bondarenko
 * Date: 13.12.2017
 * Time: 15:01
 * E-mail: DenBond7@gmail.com
 */
@Ignore
@RunWith(AndroidJUnit4.class)
@MediumTest
public class JsTest {
  private static final String ASSETS_PATH_BEN_SEC_ASC = "pgp/ben@flowcrypt.com-sec.asc";
  private static final String ASSETS_PATH_DEN_SEC_ASC = "pgp/den@flowcrypt.com-sec.asc";
  private static final String PGP_PASSWORD_ANDROID = "android";
  private static final String BEN_EMAIL = "ben@flowcrypt.com";
  private static final String DEN_EMAIL = "den@flowcrypt.com";
  private static final String BEN_LONG_ID = "018C0A1F26A6313A";
  private static final String DEN_LONG_ID = "1CC1B4641C0652CC";
  private static final String BEN_FINGERPRINT = "39A506483E49FBF1B4CCD03E018C0A1F26A6313A";
  private static final String DEN_FINGERPRINT = "3A5BAF9244046885722B82B71CC1B4641C0652CC";
  private static final String BEN_KEYWORDS = "ACCOUNT GATE MARCH ESSENCE GLIDE CHEF";
  private static final String DEN_KEYWORDS = "BROOM ASSET BOIL DAY GOWN BOOK";
  private static final String TAG = JsTest.class.getSimpleName();
  private static final String TESTS_DIRECTORY = "tests";

  private static Js js;
  private static StorageConnectorInterface storageConnectorInterface;
  private static PgpKey pgpKeyPrivateBen;
  private static PgpKey pgpKeyPrivateDen;
  private static PgpKey pgpKeyPublicBen;
  private static PgpKey pgpKeyPublicDen;
  private static File encryptedImage1Mb;
  private static File image1Mb;
  private static File parentDir;

  @AfterClass
  public static void cleanCacheDir() throws Exception {
    if (parentDir != null && parentDir.exists()) {
      FileAndDirectoryUtils.cleanDir(parentDir);
    }
  }

  @BeforeClass
  public static void initCacheDir() throws Exception {
    parentDir = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),
        TESTS_DIRECTORY);
    if (parentDir.exists()) {
      FileAndDirectoryUtils.cleanDir(parentDir);
    } else if (!parentDir.mkdirs()) {
      Log.d(TAG, "Create cache directory " + parentDir.getName() + " filed!");
    }

    storageConnectorInterface = prepareStoreConnectorInterface();
    js = new Js(InstrumentationRegistry.getInstrumentation().getTargetContext(), storageConnectorInterface);
    pgpKeyPrivateBen = generatePgpKey(js, ASSETS_PATH_BEN_SEC_ASC);
    pgpKeyPublicBen = pgpKeyPrivateBen.toPublic();
    pgpKeyPrivateDen = generatePgpKey(js, ASSETS_PATH_DEN_SEC_ASC);
    pgpKeyPublicDen = pgpKeyPrivateDen.toPublic();
    loadImages();
  }

  @Test
  public void initSecurityStorageConnector() {
    new SecurityStorageConnector(InstrumentationRegistry.getInstrumentation().getTargetContext());
  }

  @Test
  public void initJs() throws Exception {
    new Js(InstrumentationRegistry.getInstrumentation().getTargetContext(), null);
  }

  @Test
  public void testReadFileFromAssetsToString() throws Exception {
    readFileFromAssetsAsString(InstrumentationRegistry.getInstrumentation().getContext(),
        "pgp/ben_to_den_pgp_short_mime_message.acs");
  }

  @Test
  public void testMimeDecode() throws Exception {
    js.mime_decode(readFileFromAssetsAsString(InstrumentationRegistry.getInstrumentation().getContext(),
        "pgp/ben_to_den_pgp_short_mime_message.acs"));
  }

  @Test
  public void testDecryptText() throws Exception {
    MimeMessage mimeMsg = js.mime_decode(readFileFromAssetsAsString(InstrumentationRegistry.getInstrumentation()
            .getContext(),
        "pgp/ben_to_den_pgp_short_mime_message.acs"));
    String decryptedText = js.crypto_message_decrypt(mimeMsg.getText()).getString();
    Assert.assertEquals("This is a very security encrypted text.", decryptedText);
  }

  @Test
  public void testArmor() {
    pgpKeyPrivateBen.armor();
  }

  @Test
  public void testToPublic() {
    pgpKeyPrivateBen.toPublic();
  }

  @Test
  public void testCryptoKeyFingerprint() {
    Assert.assertTrue(BEN_FINGERPRINT.equals(js.crypto_key_fingerprint(pgpKeyPrivateBen)));
  }

  @Test
  public void testCryptoKeyLongidFromPgpKey() {
    Assert.assertTrue(BEN_LONG_ID.equals(js.crypto_key_longid(pgpKeyPrivateBen)));
  }

  @Test
  public void testCryptoKeyLongidFromFingerprint() {
    Assert.assertTrue(BEN_LONG_ID.equals(js.crypto_key_longid(BEN_FINGERPRINT)));
  }

  @Test
  public void testMnemonic() {
    Assert.assertTrue(BEN_KEYWORDS.equals(js.mnemonic(BEN_LONG_ID)));
  }

  @Test
  public void testCryptoKeyRead() throws Exception {
    js.crypto_key_read(readFileFromAssetsAsString(InstrumentationRegistry.getInstrumentation().getContext(),
        ASSETS_PATH_BEN_SEC_ASC));
  }

  @Test
  public void testGetPrimaryUserId() {
    PgpContact primaryUserId = pgpKeyPrivateBen.getPrimaryUserId();
    Assert.assertTrue(primaryUserId.getEmail().equalsIgnoreCase(BEN_EMAIL));
  }

  @Test
  public void testLoadFileFromAssets() throws Exception {
    File originalImage1Mb = createTempFile();
    FileUtils.copyInputStreamToFile(InstrumentationRegistry.getInstrumentation().getContext().getAssets()
        .open("pgp/1_mb_image.jpg"), originalImage1Mb);
  }

  @Test
  public void testDecryptFileWithCompareResults() throws Exception {
    File decryptedFile = decryptFile(encryptedImage1Mb);
    File originalImage1Mb = createTempFile();
    FileUtils.copyInputStreamToFile(InstrumentationRegistry.getInstrumentation().getContext().getAssets()
        .open("pgp/1_mb_image.jpg"), originalImage1Mb);

    Assert.assertTrue(FileUtils.contentEquals(decryptedFile, originalImage1Mb));
  }

  @Test
  public void testEncryptFile() {
    /*File encryptedTempFile = createTempFile();
    byte[] encryptedBytes = js.crypto_message_encrypt(
        new String[]{pgpKeyPublicBen.armor(), pgpKeyPublicDen.armor()}, IOUtils.toByteArray(InstrumentationRegistry
            .getInstrumentation().getContext().getAssets().open("pgp/1_mb_image.jpg")), image1Mb.getName());
    FileUtils.writeByteArrayToFile(encryptedTempFile, encryptedBytes);*/
  }

  private static DynamicStorageConnector prepareStoreConnectorInterface() throws IOException {
    Js js = new Js(InstrumentationRegistry.getInstrumentation().getTargetContext(), null);

    PgpContact[] pgpContacts = preparePgpContacts(js);
    PgpKeyInfo[] pgpKeyPrivateKeys = preparePgpKeyInfos(js);
    String[] passphraseStrings = preparePassphraseArray();

    return new DynamicStorageConnector(pgpContacts, pgpKeyPrivateKeys, passphraseStrings);
  }

  @NonNull
  private static PgpKey generatePgpKey(Js js, String privateKeyName) throws IOException {
    String privateKey = readFileFromAssetsAsString(InstrumentationRegistry.getInstrumentation().getContext(),
        privateKeyName);
    return js.crypto_key_read(privateKey);
  }

  private static PgpContact[] preparePgpContacts(Js js) throws IOException {
    PgpContact[] pgpContacts = new PgpContact[2];

    pgpContacts[0] = generatePgpContact(js, "Ben", ASSETS_PATH_BEN_SEC_ASC);
    pgpContacts[1] = generatePgpContact(js, "Den", ASSETS_PATH_DEN_SEC_ASC);

    return pgpContacts;
  }

  private static PgpContact generatePgpContact(Js js, String contactName, String privateKeyName) throws IOException {
    String privateKey = readFileFromAssetsAsString(InstrumentationRegistry.getInstrumentation().getContext(),
        privateKeyName);
    PgpKey pgpKeyPrivate = js.crypto_key_read(privateKey);
    String fingerprint = js.crypto_key_fingerprint(pgpKeyPrivate);
    String longId = js.crypto_key_longid(fingerprint);
    String keyOwner = pgpKeyPrivate.getPrimaryUserId().getEmail();
    String publicKey = pgpKeyPrivate.toPublic().armor();

    return new PgpContact(keyOwner, contactName, publicKey, true, "test", false, fingerprint, longId,
        js.mnemonic(longId), 0);
  }

  private static String readFileFromAssetsAsString(Context context, String filePath) throws IOException {
    return IOUtils.toString(context.getAssets().open(filePath), "UTF-8");
  }

  private static String[] preparePassphraseArray() {
    return new String[]{PGP_PASSWORD_ANDROID, PGP_PASSWORD_ANDROID};
  }

  private static PgpKeyInfo[] preparePgpKeyInfos(Js js) throws IOException {
    PgpKeyInfo[] pgpKeyInfos = new PgpKeyInfo[2];
    pgpKeyInfos[0] = generatePgpKeyInfo(js, ASSETS_PATH_BEN_SEC_ASC);
    pgpKeyInfos[1] = generatePgpKeyInfo(js, ASSETS_PATH_DEN_SEC_ASC);
    return pgpKeyInfos;
  }

  @NonNull
  private static PgpKeyInfo generatePgpKeyInfo(Js js, String privateKeyName) throws IOException {
    PgpKey pgpKeyPrivate = generatePgpKey(js, privateKeyName);
    return new PgpKeyInfo(pgpKeyPrivate.armor(), js.crypto_key_longid(js.crypto_key_fingerprint(pgpKeyPrivate)));
  }

  private static void loadImages() throws IOException {
    encryptedImage1Mb = createTempFile();
    image1Mb = createTempFile();
    FileUtils.copyInputStreamToFile(
        InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("pgp/1_mb_image.jpg.pgp"),
        encryptedImage1Mb);
    FileUtils.copyInputStreamToFile(
        InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("pgp/1_mb_image.jpg"), image1Mb);
  }

  @NonNull
  private static File createTempFile() throws IOException {
    return File.createTempFile(TAG, null, parentDir);
  }

  private static File decryptFile(File image1Mb) throws IOException {
    try (InputStream inputStream = new FileInputStream(image1Mb)) {
      PgpDecrypted pgpDecrypted = js.crypto_message_decrypt(IOUtils.toByteArray(inputStream));
      byte[] decryptedBytes = pgpDecrypted.getBytes();

      File decryptedFile = createTempFile();

      try (OutputStream outputStream = FileUtils.openOutputStream(decryptedFile)) {
        IOUtils.write(decryptedBytes, outputStream);
      }

      return decryptedFile;
    }
  }
}
