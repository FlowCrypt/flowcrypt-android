/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.JsForUiManager;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.StorageConnectorInterface;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.flowcrypt.email.ui.activity.base.BaseActivity;
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment;
import com.flowcrypt.email.ui.activity.fragment.dialog.WebViewInfoDialogFragment;
import com.flowcrypt.email.ui.loader.EncryptAndSavePrivateKeysAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class describes checking the received private keys. Here we validate and save encrypted
 * via {@link KeyStoreCryptoManager} keys to the database. If one of received private keys is
 * valid, we will return {@link Activity#RESULT_OK}.
 *
 * @author Denis Bondarenko
 * Date: 21.07.2017
 * Time: 9:59
 * E-mail: DenBond7@gmail.com
 */

public class CheckKeysActivity extends BaseActivity implements View.OnClickListener,
    LoaderManager.LoaderCallbacks<LoaderResult> {

  public static final int RESULT_NEGATIVE = 10;
  public static final int RESULT_NEUTRAL = 11;

  public static final String KEY_EXTRA_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
      "KEY_EXTRA_PRIVATE_KEYS", CheckKeysActivity.class);
  public static final String KEY_EXTRA_SUB_TITLE = GeneralUtil.generateUniqueExtraKey(
      "KEY_EXTRA_SUB_TITLE", CheckKeysActivity.class);
  public static final String KEY_EXTRA_POSITIVE_BUTTON_TITLE = GeneralUtil.generateUniqueExtraKey(
      "KEY_EXTRA_POSITIVE_BUTTON_TITLE", CheckKeysActivity.class);
  public static final String KEY_EXTRA_NEUTRAL_BUTTON_TITLE = GeneralUtil.generateUniqueExtraKey(
      "KEY_EXTRA_NEUTRAL_BUTTON_TITLE", CheckKeysActivity.class);
  public static final String KEY_EXTRA_NEGATIVE_BUTTON_TITLE =
      GeneralUtil.generateUniqueExtraKey(
          "KEY_EXTRA_NEGATIVE_BUTTON_TITLE", CheckKeysActivity.class);
  public static final String KEY_EXTRA_IS_EXTRA_IMPORT_OPTION = GeneralUtil.generateUniqueExtraKey(
      "KEY_EXTRA_IS_EXTRA_IMPORT_OPTION", CheckKeysActivity.class);

  private ArrayList<KeyDetails> privateKeyDetailsList;
  private Map<KeyDetails, String> mapOfKeyDetailsAndLongIds;

  private EditText editTextKeyPassword;
  private TextView textViewSubTitle;
  private View progressBar;

  private String subTitle;
  private String positiveButtonTitle;
  private String neutralButtonTitle;
  private String negativeButtonTitle;
  private int countOfUniqueKeys;

  public static Intent newIntent(Context context, ArrayList<KeyDetails> privateKeys,
                                 String bottomTitle, String positiveButtonTitle,
                                 String negativeButtonTitle) {
    return newIntent(context, privateKeys, bottomTitle, positiveButtonTitle, null, negativeButtonTitle, false);
  }

  public static Intent newIntent(Context context, ArrayList<KeyDetails> privateKeys,
                                 String bottomTitle, String positiveButtonTitle, String neutralButtonTitle,
                                 String negativeButtonTitle) {
    return newIntent(context, privateKeys, bottomTitle, positiveButtonTitle, neutralButtonTitle,
        negativeButtonTitle, false);
  }

  public static Intent newIntent(Context context, ArrayList<KeyDetails> privateKeys,
                                 String subTitle, String positiveButtonTitle,
                                 String neutralButtonTitle, String negativeButtonTitle, boolean isExtraImportOption) {
    Intent intent = new Intent(context, CheckKeysActivity.class);
    intent.putExtra(KEY_EXTRA_PRIVATE_KEYS, privateKeys);
    intent.putExtra(KEY_EXTRA_SUB_TITLE, subTitle);
    intent.putExtra(KEY_EXTRA_POSITIVE_BUTTON_TITLE, positiveButtonTitle);
    intent.putExtra(KEY_EXTRA_NEUTRAL_BUTTON_TITLE, neutralButtonTitle);
    intent.putExtra(KEY_EXTRA_NEGATIVE_BUTTON_TITLE, negativeButtonTitle);
    intent.putExtra(KEY_EXTRA_IS_EXTRA_IMPORT_OPTION, isExtraImportOption);
    return intent;
  }

  @Override
  public boolean isDisplayHomeAsUpEnabled() {
    return false;
  }

  @Override
  public int getContentViewResourceId() {
    return R.layout.activity_check_keys;
  }

  @Override
  public View getRootView() {
    return findViewById(R.id.layoutContent);
  }

  @Override
  public void onJsServiceConnected() {

  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent() != null) {
      this.privateKeyDetailsList = getIntent().getParcelableArrayListExtra(KEY_EXTRA_PRIVATE_KEYS);
      this.subTitle = getIntent().getStringExtra(KEY_EXTRA_SUB_TITLE);
      this.positiveButtonTitle = getIntent().getStringExtra(KEY_EXTRA_POSITIVE_BUTTON_TITLE);
      this.neutralButtonTitle = getIntent().getStringExtra(KEY_EXTRA_NEUTRAL_BUTTON_TITLE);
      this.negativeButtonTitle = getIntent().getStringExtra(KEY_EXTRA_NEGATIVE_BUTTON_TITLE);

      if (privateKeyDetailsList != null) {
        this.mapOfKeyDetailsAndLongIds = prepareMapFromKeyDetailsList(privateKeyDetailsList);
        this.countOfUniqueKeys = getUniqueKeysLongIdsCount(mapOfKeyDetailsAndLongIds);

        if (!getIntent().getBooleanExtra(KEY_EXTRA_IS_EXTRA_IMPORT_OPTION, false)) {
          removeAlreadyImportedKeysFromGivenList();

          if (privateKeyDetailsList.size() != mapOfKeyDetailsAndLongIds.size()) {
            this.privateKeyDetailsList = new ArrayList<>(mapOfKeyDetailsAndLongIds.keySet());
            if (privateKeyDetailsList.isEmpty()) {
              setResult(Activity.RESULT_OK);
              finish();
            } else {
              Map<KeyDetails, String> mapOfRemainingBackups
                  = prepareMapFromKeyDetailsList(privateKeyDetailsList);
              int remainingKeyCount = getUniqueKeysLongIdsCount(mapOfRemainingBackups);

              this.subTitle = getResources().getQuantityString(
                  R.plurals.not_recovered_all_keys, remainingKeyCount,
                  countOfUniqueKeys - remainingKeyCount, countOfUniqueKeys, remainingKeyCount);
            }
          } else {
            this.subTitle = getResources().getQuantityString(
                R.plurals.found_backup_of_your_account_key, countOfUniqueKeys, countOfUniqueKeys);
          }
        }
      } else {
        setResult(Activity.RESULT_CANCELED);
        finish();
      }
    } else {
      finish();
    }

    initViews();
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.buttonPositiveAction:
        UIUtil.hideSoftInput(this, editTextKeyPassword);
        if (privateKeyDetailsList != null && !privateKeyDetailsList.isEmpty()) {
          if (TextUtils.isEmpty(editTextKeyPassword.getText().toString())) {
            showInfoSnackbar(editTextKeyPassword, getString(R.string.passphrase_must_be_non_empty));
          } else {
            if (getSnackBar() != null) {
              getSnackBar().dismiss();
            }

            getSupportLoaderManager().restartLoader(R.id
                .loader_id_encrypt_and_save_private_keys_infos, null, this);
          }
        }
        break;

      case R.id.buttonNeutralAction:
        setResult(RESULT_NEUTRAL);
        finish();
        break;

      case R.id.buttonNegativeAction:
        setResult(RESULT_NEGATIVE);
        finish();
        break;

      case R.id.imageButtonHint:
        InfoDialogFragment infoDialogFragment = InfoDialogFragment.newInstance("",
            getString(R.string.hint_when_found_keys_in_email));
        infoDialogFragment.show(getSupportFragmentManager(), InfoDialogFragment.class.getSimpleName());
        break;

      case R.id.imageButtonPasswordHint:
        try {
          WebViewInfoDialogFragment webViewInfoDialogFragment = WebViewInfoDialogFragment.newInstance("",
              IOUtils.toString(getAssets().open("html/forgotten_pass_phrase_hint.htm"),
                  StandardCharsets.UTF_8));
          webViewInfoDialogFragment.show(getSupportFragmentManager(), WebViewInfoDialogFragment.class
              .getSimpleName());
        } catch (IOException e) {
          e.printStackTrace();
        }
        break;
    }
  }

  @NonNull
  @Override
  public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
    switch (id) {
      case R.id.loader_id_encrypt_and_save_private_keys_infos:
        progressBar.setVisibility(View.VISIBLE);
        return new EncryptAndSavePrivateKeysAsyncTaskLoader(this, privateKeyDetailsList,
            editTextKeyPassword.getText().toString());

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

  }

  @Override
  public void handleFailureLoaderResult(int loaderId, Exception e) {
    switch (loaderId) {
      case R.id.loader_id_encrypt_and_save_private_keys_infos:
        progressBar.setVisibility(View.GONE);
        showInfoSnackbar(getRootView(), TextUtils.isEmpty(e.getMessage())
            ? getString(R.string.can_not_read_this_private_key) : e.getMessage());
        break;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void handleSuccessLoaderResult(int loaderId, Object result) {
    switch (loaderId) {
      case R.id.loader_id_encrypt_and_save_private_keys_infos:
        progressBar.setVisibility(View.GONE);
        ArrayList<KeyDetails> savedKeyDetailsList = (ArrayList<KeyDetails>) result;
        if (savedKeyDetailsList != null && !savedKeyDetailsList.isEmpty()) {
          JsForUiManager.getInstance(this).getJs().getStorageConnector().refresh(this);
          restartJsService();

          Map<KeyDetails, String> mapOfSavedKeyDetailsBackups
              = prepareMapFromKeyDetailsList(savedKeyDetailsList);

          privateKeyDetailsList.removeAll(generateMatchedKeyDetailsList(mapOfSavedKeyDetailsBackups));
          if (privateKeyDetailsList.isEmpty()) {
            setResult(Activity.RESULT_OK);
            finish();
          } else {
            initButton(R.id.buttonNeutralAction, View.VISIBLE, getString(R.string.skip_remaining_backups));
            editTextKeyPassword.setText(null);
            Map<KeyDetails, String> mapOfRemainingBackups
                = prepareMapFromKeyDetailsList(privateKeyDetailsList);
            int remainingKeyCount = getUniqueKeysLongIdsCount(mapOfRemainingBackups);

            textViewSubTitle.setText(getResources().getQuantityString(
                R.plurals.not_recovered_all_keys, remainingKeyCount,
                countOfUniqueKeys - remainingKeyCount,
                countOfUniqueKeys, remainingKeyCount));
          }
        } else {
          showInfoSnackbar(getRootView(), getString(R.string.password_is_incorrect));
        }
        break;
    }
  }

  private void initViews() {
    if (findViewById(R.id.buttonPositiveAction) != null) {
      initButton(R.id.buttonPositiveAction, View.VISIBLE, positiveButtonTitle);
    }

    if (!TextUtils.isEmpty(neutralButtonTitle) && findViewById(R.id.buttonNeutralAction) != null) {
      initButton(R.id.buttonNeutralAction, View.VISIBLE, neutralButtonTitle);
    }

    if (findViewById(R.id.buttonNegativeAction) != null) {
      initButton(R.id.buttonNegativeAction, View.VISIBLE, negativeButtonTitle);
    }

    if (findViewById(R.id.imageButtonHint) != null) {
      View imageButtonHint = findViewById(R.id.imageButtonHint);
      if (privateKeyDetailsList != null && !privateKeyDetailsList.isEmpty()
          && privateKeyDetailsList.get(0).getBornType() == KeyDetails.Type.EMAIL) {
        imageButtonHint.setVisibility(View.VISIBLE);
        imageButtonHint.setOnClickListener(this);
      } else {
        imageButtonHint.setVisibility(View.GONE);
      }
    }

    if (findViewById(R.id.imageButtonPasswordHint) != null) {
      findViewById(R.id.imageButtonPasswordHint).setOnClickListener(this);
    }

    textViewSubTitle = findViewById(R.id.textViewSubTitle);
    if (textViewSubTitle != null) {
      textViewSubTitle.setText(subTitle);
    }

    editTextKeyPassword = findViewById(R.id.editTextKeyPassword);
    progressBar = findViewById(R.id.progressBar);

    if (getIntent().getBooleanExtra(KEY_EXTRA_IS_EXTRA_IMPORT_OPTION, false)) {
      TextView textViewTitle = findViewById(R.id.textViewTitle);
      textViewTitle.setText(R.string.import_private_key);
    }
  }

  private void initButton(int buttonViewId, int visibility, String text) {
    Button buttonNeutralAction = findViewById(buttonViewId);
    buttonNeutralAction.setVisibility(visibility);
    buttonNeutralAction.setText(text);
    buttonNeutralAction.setOnClickListener(this);
  }

  /**
   * Remove the already imported keys from the list of found backups.
   */
  private void removeAlreadyImportedKeysFromGivenList() {
    Set<String> longIds = getUniqueKeysLongIds(mapOfKeyDetailsAndLongIds);
    StorageConnectorInterface storageConnectorInterface
        = JsForUiManager.getInstance(this).getJs().getStorageConnector();

    for (String longId : longIds) {
      if (storageConnectorInterface.getPgpPrivateKey(longId) != null) {
        for (Iterator<Map.Entry<KeyDetails, String>> iterator = mapOfKeyDetailsAndLongIds.entrySet()
            .iterator(); iterator.hasNext(); ) {
          Map.Entry<KeyDetails, String> entry = iterator.next();
          if (longId.equals(entry.getValue())) {
            iterator.remove();
          }
        }
      }
    }
  }

  /**
   * Get a count of unique longIds.
   *
   * @param mapOfKeyDetailsAndLongIds An input map of {@link KeyDetails}.
   * @return A count of unique longIds.
   */
  private int getUniqueKeysLongIdsCount(Map<KeyDetails, String> mapOfKeyDetailsAndLongIds) {
    Set<String> strings = new HashSet<>();
    strings.addAll(mapOfKeyDetailsAndLongIds.values());
    return strings.size();
  }

  /**
   * Get a set of unique longIds.
   *
   * @param mapOfKeyDetailsAndLongIds An input map of {@link KeyDetails}.
   * @return A list of unique longIds.
   */
  private Set<String> getUniqueKeysLongIds(Map<KeyDetails, String> mapOfKeyDetailsAndLongIds) {
    Set<String> strings = new HashSet<>();
    strings.addAll(mapOfKeyDetailsAndLongIds.values());
    return strings;
  }

  /**
   * Generate a map of incoming list of {@link KeyDetails} objects where values will be a {@link PgpKey} longId.
   *
   * @param privateKeyDetailsList An incoming list of {@link KeyDetails} objects.
   * @return A generated map.
   */
  private Map<KeyDetails, String> prepareMapFromKeyDetailsList(ArrayList<KeyDetails> privateKeyDetailsList) {
    Js js = JsForUiManager.getInstance(this).getJs();
    Map<KeyDetails, String> keyDetailsStringMap = new HashMap<>();

    for (KeyDetails keyDetails : privateKeyDetailsList) {
      String normalizedArmoredKey = js.crypto_key_normalize(keyDetails.getValue());
      PgpKey pgpKey = js.crypto_key_read(normalizedArmoredKey);
      keyDetailsStringMap.put(keyDetails, pgpKey.getLongid());
    }
    return keyDetailsStringMap;
  }

  /**
   * Generate a matched list of the existing keys. It will contain all {@link KeyDetails} which has a right longId.
   *
   * @param mapOfSavedKeyDetailsAndLongIds An incoming map of {@link KeyDetails} objects.
   * @return A matched list.
   */
  private ArrayList<KeyDetails> generateMatchedKeyDetailsList(Map<KeyDetails, String>
                                                                  mapOfSavedKeyDetailsAndLongIds) {
    ArrayList<KeyDetails> matchedKeyDetails = new ArrayList<>();
    for (Map.Entry<KeyDetails, String> entry : mapOfSavedKeyDetailsAndLongIds.entrySet()) {
      for (Map.Entry<KeyDetails, String> innerEntry : mapOfKeyDetailsAndLongIds.entrySet()) {
        if (innerEntry.getValue().equals(entry.getValue())) {
          matchedKeyDetails.add(innerEntry.getKey());
        }
      }
    }

    return matchedKeyDetails;
  }
}
