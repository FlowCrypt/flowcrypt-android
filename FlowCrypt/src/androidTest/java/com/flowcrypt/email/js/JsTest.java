/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import com.flowcrypt.email.security.SecurityStorageConnector;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

/**
 * @author Denis Bondarenko
 *         Date: 13.12.2017
 *         Time: 15:01
 *         E-mail: DenBond7@gmail.com
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class JsTest {
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
    private Js js;

    private StorageConnectorInterface storageConnectorInterface;

    public JsTest() throws IOException {
        this.storageConnectorInterface = prepareStoreConnectorInterface();
        this.js = new Js(InstrumentationRegistry.getTargetContext(), storageConnectorInterface);
    }

    @Test
    public void initStorageConnectorInterface() throws Exception {
        new SecurityStorageConnector(InstrumentationRegistry.getTargetContext());
    }

    @Test
    public void initJs() throws Exception {
        new Js(InstrumentationRegistry.getTargetContext(), null);
    }

    @Test
    public void testReadFileFromAssetsToString() throws Exception {
        readFileFromAssetsAsString(InstrumentationRegistry.getContext(), "pgp/ben_to_den_pgp_short_mime_message.acs");
    }

    @Test
    public void testMimeDecode() throws Exception {
        js.mime_decode(readFileFromAssetsAsString(InstrumentationRegistry.getContext(),
                "pgp/ben_to_den_pgp_short_mime_message.acs"));
    }

    @Test
    public void testDecryptText() throws Exception {
        MimeMessage mimeMessage = js.mime_decode(readFileFromAssetsAsString(InstrumentationRegistry.getContext(),
                "pgp/ben_to_den_pgp_short_mime_message.acs"));
        String decryptedText = js.crypto_message_decrypt(mimeMessage.getText()).getString();
        Assert.assertTrue(decryptedText.equals("This is a very security encrypted text."));
    }

    private DynamicStorageConnector prepareStoreConnectorInterface() throws IOException {
        Js js = new Js(InstrumentationRegistry.getTargetContext(), null);

        PgpContact[] pgpContacts = preparePgpContacts(js);
        PgpKeyInfo[] pgpKeyInfos = preparePgpKeyInfos(js);
        String[] passphraseStrings = preparePassphraseArray();

        return new DynamicStorageConnector(pgpContacts, pgpKeyInfos, passphraseStrings);
    }

    private String[] preparePassphraseArray() {
        return new String[]{PGP_PASSWORD_ANDROID, PGP_PASSWORD_ANDROID};
    }

    private PgpKeyInfo[] preparePgpKeyInfos(Js js) throws IOException {
        PgpKeyInfo[] pgpKeyInfos = new PgpKeyInfo[2];
        pgpKeyInfos[0] = generatePgpKeyInfo(js, "pgp/ben@flowcrypt.com-sec.asc");
        pgpKeyInfos[1] = generatePgpKeyInfo(js, "pgp/den@flowcrypt.com-sec.asc");
        return pgpKeyInfos;
    }

    @NonNull
    private PgpKeyInfo generatePgpKeyInfo(Js js, String privateKeyName) throws IOException {
        String privateKey = readFileFromAssetsAsString(InstrumentationRegistry.getContext(), privateKeyName);
        return new PgpKeyInfo(js.crypto_key_read(privateKey).armor(),
                js.crypto_key_longid(js.crypto_key_fingerprint(js.crypto_key_read(privateKey))));
    }

    private PgpContact[] preparePgpContacts(Js js) throws IOException {
        PgpContact[] pgpContacts = new PgpContact[2];

        pgpContacts[0] = generatePgpContact(js, "Ben", "pgp/ben@flowcrypt.com-sec.asc");
        pgpContacts[1] = generatePgpContact(js, "Den", "pgp/den@flowcrypt.com-sec.asc");

        return pgpContacts;
    }

    private PgpContact generatePgpContact(Js js, String contactName, String privateKeyName) throws IOException {
        String privateKey = readFileFromAssetsAsString(InstrumentationRegistry.getContext(), privateKeyName);
        PgpKey pgpKey = js.crypto_key_read(privateKey);
        String fingerprint = js.crypto_key_fingerprint(js.crypto_key_read(privateKey));
        String longId = js.crypto_key_longid(fingerprint);
        String keyOwner = pgpKey.getPrimaryUserId().getEmail();
        String publicKey = pgpKey.toPublic().armor();

        return new PgpContact(keyOwner, contactName, publicKey, true, "test", false, fingerprint, longId,
                js.mnemonic(longId), 0);
    }

    private String readFileFromAssetsAsString(Context context, String filePath) throws IOException {
        return IOUtils.toString(context.getAssets().open(filePath), "UTF-8");
    }
}