/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;

import com.flowcrypt.email.R;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.ui.activity.base.BaseImportKeyActivity;
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity;

/**
 * @author Denis Bondarenko
 * Date: 04.05.2018
 * Time: 17:07
 * E-mail: DenBond7@gmail.com
 */
public class AddContactActivity extends BaseImportKeyActivity {

    public static Intent newIntent(Context context) {
        return newIntent(context, context.getString(R.string.add_public_keys_of_your_contacts),
                false, AddContactActivity.class);
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_import_public_keys;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_import_public_keys, menu);
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

    @Override
    public void onKeyValidated(KeyDetails.Type type) {
        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public boolean isPrivateKeyChecking() {
        return false;
    }

}
