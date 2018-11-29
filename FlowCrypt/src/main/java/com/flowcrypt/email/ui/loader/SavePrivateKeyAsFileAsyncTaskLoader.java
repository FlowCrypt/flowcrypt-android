/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.net.Uri;

import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;

import androidx.loader.content.AsyncTaskLoader;

/**
 * This loader tries to save the backup of the private key as a file.
 * <p>
 * Return true if the key saved, false otherwise;
 *
 * @author DenBond7
 * Date: 26.07.2017
 * Time: 13:18
 * E-mail: DenBond7@gmail.com
 */

public class SavePrivateKeyAsFileAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {
  private Uri destinationUri;
  private AccountDao accountDao;

  public SavePrivateKeyAsFileAsyncTaskLoader(Context context, AccountDao accountDao, Uri destinationUri) {
    super(context);
    this.accountDao = accountDao;
    this.destinationUri = destinationUri;
    onContentChanged();
  }

  @Override
  public LoaderResult loadInBackground() {
    try {
      Js js = new Js(getContext(), new SecurityStorageConnector(getContext()));
      return new LoaderResult(GeneralUtil.writeFileFromStringToUri(getContext(), destinationUri,
          SecurityUtils.generatePrivateKeysBackup(getContext(), js, accountDao, false)) > 0, null);
    } catch (Exception e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
      return new LoaderResult(null, e);
    }
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
}
