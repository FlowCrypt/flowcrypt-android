/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.MessageBlock;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.model.PublicKeyInfo;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.util.exception.ManualHandledException;

import org.acra.ACRA;

import java.util.ArrayList;
import java.util.List;

/**
 * This loader parses a list of {@link PublicKeyInfo} objects from an input string.
 *
 * @author Denis Bondarenko
 *         Date: 09.05.2018
 *         Time: 16:44
 *         E-mail: DenBond7@gmail.com
 */
public class ParsePublicKeysFromStringAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {
    private String inputString;

    public ParsePublicKeysFromStringAsyncTaskLoader(Context context, String inputString) {
        super(context);
        this.inputString = inputString;
        onContentChanged();
    }

    @Override
    public void onStartLoading() {
        if (takeContentChanged()) {
            forceLoad();
        }
    }

    @Override
    public LoaderResult loadInBackground() {
        if (!TextUtils.isEmpty(inputString)) {
            try {
                return new LoaderResult(parsePublicKeysInfo(new Js(getContext(), null), inputString), null);
            } catch (Exception e) {
                e.printStackTrace();
                if (ACRA.isInitialised()) {
                    ACRA.getErrorReporter().handleException(new ManualHandledException(e));
                }
                return new LoaderResult(null, e);
            }
        } else {
            return new LoaderResult(null, new NullPointerException("An input string is null!"));
        }
    }

    @Override
    public void onStopLoading() {
        cancelLoad();
    }

    private List<PublicKeyInfo> parsePublicKeysInfo(Js js, @NonNull String publicKey) {
        List<PublicKeyInfo> publicKeyInfoList = new ArrayList<>();
        MessageBlock[] messageBlocks = js.crypto_armor_detect_blocks(publicKey);
        for (MessageBlock messageBlock : messageBlocks) {
            if (messageBlock != null && messageBlock.getType() != null) {
                switch (messageBlock.getType()) {
                    case MessageBlock.TYPE_PGP_PUBLIC_KEY:
                        String content = messageBlock.getContent();
                        String fingerprint =
                                js.crypto_key_fingerprint(js.crypto_key_read(content));
                        String longId = js.crypto_key_longid(fingerprint);
                        String keyWords = js.mnemonic(longId);
                        PgpKey pgpKey = js.crypto_key_read(content);
                        String keyOwner = pgpKey.getPrimaryUserId().getEmail();

                        PgpContact pgpContact = new ContactsDaoSource().getPgpContact(getContext(), keyOwner);

                        PublicKeyInfo messagePartPgpPublicKey = new PublicKeyInfo(keyWords, fingerprint, keyOwner,
                                longId, pgpContact, publicKey);

                        publicKeyInfoList.add(messagePartPgpPublicKey);
                        break;
                }
            }
        }

        return publicKeyInfoList;
    }
}
