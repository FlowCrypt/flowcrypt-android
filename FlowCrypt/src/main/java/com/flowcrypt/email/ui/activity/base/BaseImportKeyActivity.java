/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.KeyImportModel;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.service.CheckClipboardToFindKeyService;
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment;
import com.flowcrypt.email.ui.loader.ParseKeysFromResourceAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;
import com.google.android.material.snackbar.Snackbar;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

/**
 * The base import key activity. This activity defines a logic of import a key (private or
 * public) via select a file or using clipboard.
 *
 * @author Denis Bondarenko
 * Date: 03.08.2017
 * Time: 12:35
 * E-mail: DenBond7@gmail.com
 */

public abstract class BaseImportKeyActivity extends BaseBackStackSyncActivity
    implements View.OnClickListener, LoaderManager.LoaderCallbacks<LoaderResult> {

  public static final String KEY_EXTRA_IS_SYNC_ENABLE
      = GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_IS_SYNC_ENABLE", BaseImportKeyActivity.class);

  public static final String KEY_EXTRA_IS_THROW_ERROR_IF_DUPLICATE_FOUND
      = GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_IS_THROW_ERROR_IF_DUPLICATE_FOUND",
      BaseImportKeyActivity.class);

  public static final String KEY_EXTRA_PRIVATE_KEY_IMPORT_MODEL_FROM_CLIPBOARD
      = GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_PRIVATE_KEY_IMPORT_MODEL_FROM_CLIPBOARD",
      BaseImportKeyActivity.class);

  public static final String KEY_EXTRA_TITLE
      = GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_TITLE", BaseImportKeyActivity.class);

  private static final int REQUEST_CODE_SELECT_KEYS_FROM_FILES_SYSTEM = 10;
  private static final int REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE = 11;

  protected ClipboardManager clipboardManager;
  protected KeyImportModel keyImportModel;
  protected CheckClipboardToFindKeyService checkClipboardToFindKeyService;

  protected View layoutContentView;
  protected View layoutProgress;
  protected TextView textViewTitle;
  protected View buttonLoadFromFile;

  protected boolean isCheckingPrivateKeyNow;
  protected boolean throwErrorIfDuplicateFound;
  protected boolean isCheckingClipboardEnabled = true;
  protected boolean isClipboardServiceBound;

  private String title;

  private ServiceConnection clipboardConn = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      CheckClipboardToFindKeyService.LocalBinder binder = (CheckClipboardToFindKeyService.LocalBinder) service;
      checkClipboardToFindKeyService = binder.getService();
      checkClipboardToFindKeyService.setPrivateKeyMode(isPrivateKeyMode());
      isClipboardServiceBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      isClipboardServiceBound = false;
    }
  };

  public abstract void onKeyFound(KeyDetails.Type type, ArrayList<NodeKeyDetails> keyDetailsList);

  public abstract boolean isPrivateKeyMode();

  public static Intent newIntent(Context context, String title, Class<?> cls) {
    return newIntent(context, title, false, cls);
  }

  public static Intent newIntent(Context context, String title, boolean isThrowErrorIfDuplicateFoundEnabled,
                                 Class<?> cls) {
    return newIntent(context, title, null, isThrowErrorIfDuplicateFoundEnabled, cls);
  }

  public static Intent newIntent(Context context, String title, KeyImportModel model,
                                 boolean isThrowErrorIfDuplicateFoundEnabled, Class<?> cls) {
    return newIntent(context, true, title, model, isThrowErrorIfDuplicateFoundEnabled, cls);
  }

  public static Intent newIntent(Context context, boolean isSyncEnabled, String title, KeyImportModel model,
                                 boolean isThrowErrorIfDuplicateFoundEnabled, Class<?> cls) {
    Intent intent = new Intent(context, cls);
    intent.putExtra(KEY_EXTRA_IS_SYNC_ENABLE, isSyncEnabled);
    intent.putExtra(KEY_EXTRA_TITLE, title);
    intent.putExtra(KEY_EXTRA_PRIVATE_KEY_IMPORT_MODEL_FROM_CLIPBOARD, model);
    intent.putExtra(KEY_EXTRA_IS_THROW_ERROR_IF_DUPLICATE_FOUND, isThrowErrorIfDuplicateFoundEnabled);
    return intent;
  }

  @Override
  public View getRootView() {
    return findViewById(R.id.layoutContent);
  }

  @Override
  public boolean isSyncEnabled() {
    return getIntent() == null || getIntent().getBooleanExtra(KEY_EXTRA_IS_SYNC_ENABLE, true);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    bindService(new Intent(this, CheckClipboardToFindKeyService.class), clipboardConn, Context.BIND_AUTO_CREATE);

    if (getIntent() != null) {
      this.throwErrorIfDuplicateFound = getIntent().getBooleanExtra(KEY_EXTRA_IS_THROW_ERROR_IF_DUPLICATE_FOUND, false);
      this.keyImportModel = getIntent().getParcelableExtra(KEY_EXTRA_PRIVATE_KEY_IMPORT_MODEL_FROM_CLIPBOARD);
      this.title = getIntent().getStringExtra(KEY_EXTRA_TITLE);
    }

    clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    initViews();

    if (keyImportModel != null) {
      LoaderManager.getInstance(this).restartLoader(R.id.loader_id_validate_key_from_clipboard, null, this);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if (isClipboardServiceBound && !isCheckingPrivateKeyNow && isCheckingClipboardEnabled) {
      keyImportModel = checkClipboardToFindKeyService.getKeyImportModel();
      if (keyImportModel != null) {
        LoaderManager.getInstance(this).restartLoader(R.id.loader_id_validate_key_from_clipboard, null, this);
      }
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    isCheckingClipboardEnabled = true;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (isClipboardServiceBound) {
      unbindService(clipboardConn);
      isClipboardServiceBound = false;
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_CODE_SELECT_KEYS_FROM_FILES_SYSTEM:
        isCheckingClipboardEnabled = false;

        switch (resultCode) {
          case Activity.RESULT_OK:
            if (data != null) {
              if (data.getData() != null) {
                handleSelectedFile(data.getData());
              } else {
                showInfoSnackbar(getRootView(), getString(R.string.please_use_another_app_to_choose_file),
                    Snackbar.LENGTH_LONG);
              }
            }
            break;
        }
        break;

      default:
        super.onActivityResult(requestCode, resultCode, data);
    }

  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    switch (requestCode) {
      case REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE:
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          selectFile();
        } else {
          showAccessDeniedWarning();
        }
        break;
    }
  }

  @Override
  public void onBackPressed() {
    if (isCheckingPrivateKeyNow) {
      LoaderManager.getInstance(this).destroyLoader(R.id.loader_id_validate_key_from_file);
      LoaderManager.getInstance(this).destroyLoader(R.id.loader_id_validate_key_from_clipboard);
      isCheckingPrivateKeyNow = false;
      UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress, layoutContentView);
    } else {
      super.onBackPressed();
    }
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.buttonLoadFromFile:
        dismissSnackBar();

        boolean isPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED;

        if (isPermissionGranted) {
          selectFile();
        } else {
          if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            showReadSdCardExplanation();
          } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE);
          }
        }
        break;

      case R.id.buttonLoadFromClipboard:
        dismissSnackBar();

        if (clipboardManager.hasPrimaryClip()) {
          ClipData clipData = clipboardManager.getPrimaryClip();
          if (clipData != null) {
            ClipData.Item item = clipData.getItemAt(0);
            CharSequence privateKeyFromClipboard = item.getText();
            if (!TextUtils.isEmpty(privateKeyFromClipboard)) {
              keyImportModel = new KeyImportModel(null, privateKeyFromClipboard.toString(),
                  isPrivateKeyMode(), KeyDetails.Type.CLIPBOARD);

              LoaderManager.getInstance(this).restartLoader(R.id.loader_id_validate_key_from_clipboard, null, this);
            } else {
              showClipboardIsEmptyInfoDialog();
            }
          }
        } else {
          showClipboardIsEmptyInfoDialog();
        }
        break;
    }
  }

  @NonNull
  @Override
  public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
    switch (id) {
      case R.id.loader_id_validate_key_from_file:
        isCheckingPrivateKeyNow = true;
        UIUtil.exchangeViewVisibility(getApplicationContext(), true, layoutProgress, layoutContentView);
        return new ParseKeysFromResourceAsyncTaskLoader(getApplicationContext(), keyImportModel, true);

      case R.id.loader_id_validate_key_from_clipboard:
        isCheckingPrivateKeyNow = true;
        UIUtil.exchangeViewVisibility(getApplicationContext(), true, layoutProgress, layoutContentView);
        return new ParseKeysFromResourceAsyncTaskLoader(getApplicationContext(), keyImportModel, false);

      default:
        return new Loader<>(this);
    }
  }

  @Override
  public void onLoadFinished(@NonNull Loader<LoaderResult> loader, LoaderResult loaderResult) {
    handleLoaderResult(loader, loaderResult);
  }

  @Override
  public void onLoaderReset(@NonNull Loader<LoaderResult> loader) {
    switch (loader.getId()) {
      case R.id.loader_id_validate_key_from_file:
      case R.id.loader_id_validate_key_from_clipboard:
        isCheckingPrivateKeyNow = false;
        UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress, layoutContentView);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void onSuccess(int loaderId, Object result) {
    switch (loaderId) {
      case R.id.loader_id_validate_key_from_file:
        isCheckingPrivateKeyNow = false;
        UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress, layoutContentView);
        ArrayList<NodeKeyDetails> keysFromFile = (ArrayList<NodeKeyDetails>) result;

        if (!keysFromFile.isEmpty()) {
          onKeyFound(KeyDetails.Type.FILE, keysFromFile);
        } else {
          showInfoSnackbar(getRootView(), getString(R.string.file_has_wrong_pgp_structure,
              isPrivateKeyMode() ? getString(R.string.private_) : getString(R.string.public_)));
        }
        break;

      case R.id.loader_id_validate_key_from_clipboard:
        isCheckingPrivateKeyNow = false;
        UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress, layoutContentView);
        ArrayList<NodeKeyDetails> keysFromClipboard = (ArrayList<NodeKeyDetails>) result;
        if (!keysFromClipboard.isEmpty()) {
          onKeyFound(KeyDetails.Type.CLIPBOARD, keysFromClipboard);
        } else {
          showInfoSnackbar(getRootView(), getString(R.string.clipboard_has_wrong_structure,
              isPrivateKeyMode() ? getString(R.string.private_) : getString(R.string.public_)));
        }
        break;

      default:
        super.onSuccess(loaderId, result);
    }
  }

  @Override
  public void onError(int loaderId, Exception e) {
    switch (loaderId) {
      case R.id.loader_id_validate_key_from_file:
      case R.id.loader_id_validate_key_from_clipboard:
        isCheckingPrivateKeyNow = false;
        UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress, layoutContentView);

        String errorMsg = e.getMessage();

        if (e instanceof FileNotFoundException) {
          errorMsg = getString(R.string.file_not_found);
        }

        showInfoSnackbar(getRootView(), errorMsg);
        break;

      default:
        super.onError(loaderId, e);
    }
  }

  @Override
  public void onReplyReceived(int requestCode, int resultCode, Object obj) {

  }

  @Override
  public void onErrorHappened(int requestCode, int errorType, Exception e) {

  }

  /**
   * Handle a selected file.
   *
   * @param uri A {@link Uri} of the selected file.
   */
  protected void handleSelectedFile(@NonNull Uri uri) {
    keyImportModel = new KeyImportModel(uri, null, isPrivateKeyMode(), KeyDetails.Type.FILE);
    LoaderManager.getInstance(this).restartLoader(R.id.loader_id_validate_key_from_file, null, this);
  }

  protected void initViews() {
    layoutContentView = findViewById(R.id.layoutContentView);
    layoutProgress = findViewById(R.id.layoutProgress);

    textViewTitle = findViewById(R.id.textViewTitle);
    textViewTitle.setText(title);

    buttonLoadFromFile = findViewById(R.id.buttonLoadFromFile);
    buttonLoadFromFile.setOnClickListener(this);

    if (findViewById(R.id.buttonLoadFromClipboard) != null) {
      findViewById(R.id.buttonLoadFromClipboard).setOnClickListener(this);
    }
  }

  private void showAccessDeniedWarning() {
    UIUtil.showSnackbar(getRootView(), getString(R.string.access_to_read_the_sdcard_id_denied),
        getString(R.string.change), new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            GeneralUtil.showAppSettingScreen(BaseImportKeyActivity.this);
          }
        });
  }

  /**
   * Show an explanation to the user for read the sdcard.
   * After the user sees the explanation, we try again to request the permission.
   */
  private void showReadSdCardExplanation() {
    UIUtil.showSnackbar(getRootView(), getString(R.string.read_sdcard_permission_explanation_text),
        getString(R.string.do_request), new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            ActivityCompat.requestPermissions(BaseImportKeyActivity.this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE);
          }
        });
  }

  private void selectFile() {
    Intent intent = new Intent();
    intent.setAction(Intent.ACTION_GET_CONTENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("*/*");
    startActivityForResult(Intent.createChooser(intent, getString(R.string.select_key_to_import)),
        REQUEST_CODE_SELECT_KEYS_FROM_FILES_SYSTEM);
  }

  private void showClipboardIsEmptyInfoDialog() {
    String dialogMsg = getString(R.string.hint_clipboard_is_empty, isPrivateKeyMode() ?
        getString(R.string.private_) : getString(R.string.public_), getString(R.string.app_name));
    InfoDialogFragment infoDialogFragment = InfoDialogFragment.newInstance(getString(R.string.hint), dialogMsg);
    infoDialogFragment.show(getSupportFragmentManager(), InfoDialogFragment.class.getSimpleName());
  }
}
