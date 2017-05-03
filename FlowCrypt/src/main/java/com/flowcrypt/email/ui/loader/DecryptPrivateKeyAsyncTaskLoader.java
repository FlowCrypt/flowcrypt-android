package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.eclipsesource.v8.V8Object;
import com.flowcrypt.email.Constants;
import com.flowcrypt.email.test.Js;
import com.flowcrypt.email.test.PgpKey;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * This loader try to decrypt and save keys with entered password.
 * Return true if one or more key accepted, false otherwise;
 *
 * @author DenBond7
 *         Date: 03.05.2017
 *         Time: 11:47
 *         E-mail: DenBond7@gmail.com
 */

public class DecryptPrivateKeyAsyncTaskLoader extends AsyncTaskLoader<Boolean> {
    private static final String KEY_SUCCESS = "success";

    private List<String> keysPathList;
    private String passphrase;

    public DecryptPrivateKeyAsyncTaskLoader(Context context, List<String> keysPathLis, String
            passphrase) {
        super(context);
        this.keysPathList = keysPathLis;
        this.passphrase = passphrase;
        onContentChanged();
    }

    @Override
    public Boolean loadInBackground() {
        boolean isOneOrMoreKeySaved = false;
        try {
            Js js = new Js(getContext(), null);
            for (String filePath : keysPathList) {
                File file = new File(filePath);
                try {
                    String rawArmoredKey = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

                    String normalizedArmoredKey = js.crypto_key_normalize(rawArmoredKey);

                    PgpKey pgpKey = js.crypto_key_read(normalizedArmoredKey);
                    V8Object v8Object = js.crypto_key_decrypt(pgpKey, passphrase);

                    if (pgpKey.isPrivate() && v8Object != null
                            && v8Object.getBoolean(KEY_SUCCESS)) {
                        saveKeyToStorage(file.getParent(), normalizedArmoredKey, passphrase);
                        isOneOrMoreKeySaved = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isOneOrMoreKeySaved;
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
     * Try to decrypt some key with entered password.
     *
     * @param directory            Directory where file will be save;
     * @param normalizedArmoredKey A normalized key;
     * @param passphrase           A passphrase which user entered;
     */
    private void saveKeyToStorage(String directory, String normalizedArmoredKey, String
            passphrase) {
        try {
            String fileName = Constants.PREFIX_PRIVATE_KEY + passphrase;
            FileUtils.writeStringToFile(new File(directory, fileName),
                    normalizedArmoredKey,
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
