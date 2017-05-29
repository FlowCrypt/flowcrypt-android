package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.eclipsesource.v8.V8Object;
import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.KeysDao;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.model.results.ActionResult;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.flowcrypt.email.security.model.PrivateKeySourceType;
import com.flowcrypt.email.test.Js;
import com.flowcrypt.email.test.PgpKey;

import java.util.List;
import java.util.UUID;

/**
 * This loader try to encrypt and save encrypted key with entered password by
 * {@link KeyStoreCryptoManager} to the database.
 * <p>
 * Return true if one or more key saved, false otherwise;
 *
 * @author DenBond7
 *         Date: 03.05.2017
 *         Time: 11:47
 *         E-mail: DenBond7@gmail.com
 */

public class EncryptAndSavePrivateKeysAsyncTaskLoader extends
        AsyncTaskLoader<ActionResult<Boolean>> {
    private static final String KEY_SUCCESS = "success";

    private List<String> privateKeys;
    private String passphrase;

    private KeysDaoSource keysDaoSource;

    public EncryptAndSavePrivateKeysAsyncTaskLoader(Context context,
                                                    List<String> privateKeys, String passphrase) {
        super(context);
        this.privateKeys = privateKeys;
        this.passphrase = passphrase;
        this.keysDaoSource = new KeysDaoSource();
        onContentChanged();
    }

    @Override
    public ActionResult<Boolean> loadInBackground() {
        boolean isOneOrMoreKeySaved = false;
        try {
            KeyStoreCryptoManager keyStoreCryptoManager = new KeyStoreCryptoManager(getContext());
            Js js = new Js(getContext(), null);
            for (String rawArmoredKey : privateKeys) {
                String normalizedArmoredKey = js.crypto_key_normalize(rawArmoredKey);

                PgpKey pgpKey = js.crypto_key_read(normalizedArmoredKey);
                V8Object v8Object = js.crypto_key_decrypt(pgpKey, passphrase);

                if (pgpKey.isPrivate() && v8Object != null && v8Object.getBoolean(KEY_SUCCESS)) {
                    if (!keysDaoSource.isKeyExist(getContext(), pgpKey.getLongid())) {
                        Uri uri = saveKeyToDatabase(keyStoreCryptoManager, pgpKey, passphrase);
                        isOneOrMoreKeySaved = uri != null;
                    } else {
                        return new ActionResult<>(null, new Exception(getContext().getString(R
                                .string.the_key_already_added)));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ActionResult<>(null, e);
        }
        return new ActionResult<>(isOneOrMoreKeySaved, null);
    }

    @Override
    public void onStartLoading() {
        if (takeContentChanged()) {
            forceLoad();
        }
    }

    @Override
    public void onStopLoading() {
        cancelLoad();
    }

    /**
     * Try to decrypt some key with entered password and save it encrypted version by
     * {@link KeyStoreCryptoManager} to the database. This method use {@link PgpKey#getLongid()}
     * for generate an algorithm parameter spec String.
     *
     * @param keyStoreCryptoManager A {@link KeyStoreCryptoManager} which will bu used to encrypt
     *                              an information about a key;
     * @param pgpKey                A normalized key;
     * @param passphrase            A passphrase which user entered;
     */
    private Uri saveKeyToDatabase(KeyStoreCryptoManager keyStoreCryptoManager, PgpKey pgpKey,
                                  String passphrase) throws Exception {
        KeysDao keysDao = new KeysDao();
        keysDao.setLongId(pgpKey.getLongid());

        String randomVector;

        if (TextUtils.isEmpty(pgpKey.getLongid())) {
            randomVector = KeyStoreCryptoManager.normalizeAlgorithmParameterSpecString(
                    UUID.randomUUID().toString().substring(0,
                            KeyStoreCryptoManager.SIZE_OF_ALGORITHM_PARAMETER_SPEC));
        } else {
            randomVector = KeyStoreCryptoManager.normalizeAlgorithmParameterSpecString
                    (pgpKey.getLongid());
        }

        keysDao.setPrivateKeySourceType(PrivateKeySourceType.BACKUP);

        String encryptedPrivateKey = keyStoreCryptoManager.encrypt(pgpKey.armor(),
                randomVector);
        keysDao.setPrivateKey(encryptedPrivateKey);
        keysDao.setPublicKey(pgpKey.toPublic().armor());

        String encryptedPassphrase = keyStoreCryptoManager.encrypt(passphrase, randomVector);
        keysDao.setPassphrase(encryptedPassphrase);
        return keysDaoSource.addRow(getContext(), keysDao);
    }
}
