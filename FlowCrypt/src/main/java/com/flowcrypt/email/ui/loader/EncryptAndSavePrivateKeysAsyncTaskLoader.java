/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.eclipsesource.v8.V8Object;
import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.KeysDao;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.flowcrypt.email.security.model.PrivateKeySourceType;
import com.flowcrypt.email.util.GeneralUtil;

import org.acra.ACRA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class EncryptAndSavePrivateKeysAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {
    private static final String KEY_SUCCESS = "success";

    private boolean isThrowErrorIfDuplicateFound;
    private List<KeyDetails> privateKeyDetailsList;
    private String passphrase;

    private KeysDaoSource keysDaoSource;

    public EncryptAndSavePrivateKeysAsyncTaskLoader(Context context,
                                                    ArrayList<KeyDetails>
                                                            privateKeyDetailsList,
                                                    String passphrase,
                                                    boolean isThrowErrorIfDuplicateFound) {
        super(context);
        this.privateKeyDetailsList = privateKeyDetailsList;
        this.passphrase = passphrase;
        this.keysDaoSource = new KeysDaoSource();
        this.isThrowErrorIfDuplicateFound = isThrowErrorIfDuplicateFound;
        onContentChanged();
    }

    @Override
    public LoaderResult loadInBackground() {
        boolean isOneOrMoreKeySaved = false;
        Map<String, String> mapOfAlreadyUsedKey = new HashMap<>();
        try {
            KeyStoreCryptoManager keyStoreCryptoManager = new KeyStoreCryptoManager(getContext());
            Js js = new Js(getContext(), null);
            for (KeyDetails keyDetails : privateKeyDetailsList) {
                String armoredPrivateKey = null;

                switch (keyDetails.getBornType()) {
                    case FILE:
                        armoredPrivateKey = GeneralUtil.readFileFromUriToString(getContext(),
                                keyDetails.getUri());
                        break;

                    case EMAIL:
                    case CLIPBOARD:
                        armoredPrivateKey = keyDetails.getValue();
                        break;
                }


                String normalizedArmoredKey = js.crypto_key_normalize(armoredPrivateKey);

                PgpKey pgpKey = js.crypto_key_read(normalizedArmoredKey);
                V8Object v8Object = js.crypto_key_decrypt(pgpKey, passphrase);

                if (pgpKey.isPrivate()) {
                    if (!mapOfAlreadyUsedKey.containsKey(pgpKey.getLongid()) &&
                            v8Object != null && v8Object.getBoolean(KEY_SUCCESS)) {
                        if (!keysDaoSource.isKeyExist(getContext(), pgpKey.getLongid())) {
                            Uri uri = saveKeyToDatabase(keyStoreCryptoManager, keyDetails,
                                    pgpKey, passphrase);
                            PgpContact pgpContact = pgpKey.getPrimaryUserId();
                            PgpKey publicKey = pgpKey.toPublic();
                            if (pgpContact != null) {
                                pgpContact.setPubkey(publicKey.armor());
                                new ContactsDaoSource().addRow(getContext(), pgpContact);
                            }
                            isOneOrMoreKeySaved = uri != null;
                        } else if (isThrowErrorIfDuplicateFound) {
                            return new LoaderResult(null, new Exception(getContext().getString(R
                                    .string.the_key_already_added)));
                        }
                    }
                    mapOfAlreadyUsedKey.put(pgpKey.getLongid(), pgpKey.getLongid());
                } else throw new IllegalArgumentException("This is not a private key");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (ACRA.isInitialised()) {
                ACRA.getErrorReporter().handleException(e);
            }
            return new LoaderResult(null, e);
        }
        return new LoaderResult(isOneOrMoreKeySaved, null);
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
     *                              information about a key;
     * @param keyDetails            The private key details
     * @param pgpKey                A normalized key;
     * @param passphrase            A passphrase which user entered;
     */
    private Uri saveKeyToDatabase(KeyStoreCryptoManager keyStoreCryptoManager,
                                  KeyDetails keyDetails, PgpKey pgpKey,
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

        switch (keyDetails.getBornType()) {
            case EMAIL:
                keysDao.setPrivateKeySourceType(PrivateKeySourceType.BACKUP);
                break;

            case FILE:
            case CLIPBOARD:
                keysDao.setPrivateKeySourceType(PrivateKeySourceType.IMPORT);
                break;
        }

        String encryptedPrivateKey = keyStoreCryptoManager.encrypt(pgpKey.armor(),
                randomVector);
        keysDao.setPrivateKey(encryptedPrivateKey);
        keysDao.setPublicKey(pgpKey.toPublic().armor());

        String encryptedPassphrase = keyStoreCryptoManager.encrypt(passphrase, randomVector);
        keysDao.setPassphrase(encryptedPassphrase);
        return keysDaoSource.addRow(getContext(), keysDao);
    }
}
