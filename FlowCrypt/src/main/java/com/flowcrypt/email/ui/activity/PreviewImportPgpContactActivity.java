/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity;
import com.flowcrypt.email.ui.activity.fragment.PreviewImportPgpContactFragment;
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity;
import com.flowcrypt.email.util.GeneralUtil;

/**
 * This activity displays information about public keys owners and information about keys.
 *
 * @author Denis Bondarenko
 * Date: 10.05.2018
 * Time: 18:01
 * E-mail: DenBond7@gmail.com
 */
public class PreviewImportPgpContactActivity extends BaseBackStackActivity {
  public static final String KEY_EXTRA_PUBLIC_KEY_STRING
      = GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_PUBLIC_KEY_STRING", PreviewImportPgpContactActivity.class);

  public static final String KEY_EXTRA_PUBLIC_KEYS_FILE_URI
      = GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_PUBLIC_KEYS_FILE_URI",
      PreviewImportPgpContactActivity.class);

  public static Intent newIntent(Context context, String publicKeysString) {
    Intent intent = new Intent(context, PreviewImportPgpContactActivity.class);
    intent.putExtra(KEY_EXTRA_PUBLIC_KEY_STRING, publicKeysString);
    return intent;
  }

  public static Intent newIntent(Context context, Uri publicKeysFileUri) {
    Intent intent = new Intent(context, PreviewImportPgpContactActivity.class);
    intent.putExtra(KEY_EXTRA_PUBLIC_KEYS_FILE_URI, publicKeysFileUri);
    return intent;
  }

  @Override
  public int getContentViewResourceId() {
    return R.layout.activity_preview_import_pgp_contact;
  }

  @Override
  public View getRootView() {
    return findViewById(R.id.layoutContent);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent() == null || (!getIntent().hasExtra(KEY_EXTRA_PUBLIC_KEY_STRING)
        && !getIntent().hasExtra(KEY_EXTRA_PUBLIC_KEYS_FILE_URI))) {
      setResult(Activity.RESULT_CANCELED);
      finish();
    }

    String publicKeysString = getIntent().getStringExtra(KEY_EXTRA_PUBLIC_KEY_STRING);
    Uri publicKeysFileUri = getIntent().getParcelableExtra(KEY_EXTRA_PUBLIC_KEYS_FILE_URI);

    FragmentManager supportFragmentManager = getSupportFragmentManager();
    PreviewImportPgpContactFragment previewImportPgpContactFragment = (PreviewImportPgpContactFragment)
        supportFragmentManager.findFragmentById(R.id.layoutContent);

    if (previewImportPgpContactFragment == null) {
      previewImportPgpContactFragment =
          PreviewImportPgpContactFragment.newInstance(publicKeysString, publicKeysFileUri);
      supportFragmentManager.beginTransaction().add(R.id.layoutContent, previewImportPgpContactFragment).commit();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_preview_import_pgp_contact, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menuActionHelp:
        startActivity(new Intent(this, FeedbackActivity.class));
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }
}
