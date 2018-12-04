/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.MessageBlock;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.core.Js;
import com.flowcrypt.email.model.PublicKeyInfo;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.loader.content.AsyncTaskLoader;

/**
 * This loader parses a list of {@link PublicKeyInfo} objects from an input string.
 *
 * @author Denis Bondarenko
 * Date: 09.05.2018
 * Time: 16:44
 * E-mail: DenBond7@gmail.com
 */
public class ParsePublicKeysFromStringAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {
  /**
   * Max size of a key is 5Mb.
   */
  private static final int MAX_SIZE_IN_BYTES = 1024 * 1024 * 5;

  private String armoredKeys;
  private Uri fileUri;

  public ParsePublicKeysFromStringAsyncTaskLoader(Context context, String armoredKeys) {
    super(context);
    this.armoredKeys = armoredKeys;
    onContentChanged();
  }

  public ParsePublicKeysFromStringAsyncTaskLoader(Context context, Uri fileUri) {
    super(context);
    this.fileUri = fileUri;
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
    try {
      if (fileUri != null) {
        if (isKeyTooBig(fileUri)) {
          return new LoaderResult(null,
              new IllegalArgumentException("The key is too big"));
        }

        armoredKeys = GeneralUtil.readFileFromUriToString(getContext(), fileUri);
      }
    } catch (IOException e) {
      e.printStackTrace();
      return new LoaderResult(null, e);
    }

    if (!TextUtils.isEmpty(armoredKeys)) {
      if (isKeyTooBig(armoredKeys)) {
        return new LoaderResult(null,
            new IllegalArgumentException("The key is too big"));
      }

      try {
        Js js = new Js(getContext(), null);
        String normalizedArmoredKey = js.crypto_key_normalize(armoredKeys);
        PgpKey pgpKey = js.crypto_key_read(normalizedArmoredKey);

        if (js.is_valid_key(pgpKey, false)) {
          return new LoaderResult(parsePublicKeysInfo(js, armoredKeys), null);
        } else {
          return new LoaderResult(null, new IllegalArgumentException(getContext().getString(R.string
              .clipboard_has_wrong_structure, getContext().getString(R.string.public_))));
        }

      } catch (Exception e) {
        e.printStackTrace();
        ExceptionUtil.handleError(e);
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

    if (TextUtils.isEmpty(publicKey)) {
      return publicKeyInfoList;
    }

    Set<String> emails = new HashSet<>();
    MessageBlock[] messageBlocks = js.crypto_armor_detect_blocks(publicKey);
    for (MessageBlock messageBlock : messageBlocks) {
      if (messageBlock != null && messageBlock.getType() != null) {
        switch (messageBlock.getType()) {
          case MessageBlock.TYPE_PGP_PUBLIC_KEY:
            String content = messageBlock.getContent();
            String fingerprint = js.crypto_key_fingerprint(js.crypto_key_read(content));
            String longId = js.crypto_key_longid(fingerprint);
            String keyWords = js.mnemonic(longId);
            PgpKey pgpKey = js.crypto_key_read(content);
            String keyOwner = pgpKey.getPrimaryUserId().getEmail();

            if (emails.contains(keyOwner)) {
              continue;
            }

            emails.add(keyOwner);
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

  /**
   * Check that the key size mot bigger then #MAX_SIZE_IN_BYTES.
   *
   * @param fileUri The {@link Uri} of the selected file.
   * @return true if the key size not bigger then {@link #MAX_SIZE_IN_BYTES}, otherwise false
   */
  private boolean isKeyTooBig(Uri fileUri) {
    long fileSize = GeneralUtil.getFileSizeFromUri(getContext(), fileUri);
    return fileSize > MAX_SIZE_IN_BYTES;
  }

  /**
   * Check that the key size not bigger then #MAX_SIZE_IN_BYTES.
   *
   * @param inputString The input string which contains PGP keys.
   * @return true if the key size not bigger then {@link #MAX_SIZE_IN_BYTES}, otherwise false
   */
  private boolean isKeyTooBig(String inputString) {
    long fileSize = inputString.length();
    return fileSize > MAX_SIZE_IN_BYTES;
  }
}
