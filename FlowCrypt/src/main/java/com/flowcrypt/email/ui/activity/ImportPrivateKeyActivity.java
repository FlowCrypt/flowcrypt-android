/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Intent;

import com.flowcrypt.email.R;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.ui.activity.base.BaseImportKeyActivity;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This activity describes a logic of import private keys.
 *
 * @author Denis Bondarenko
 *         Date: 20.07.2017
 *         Time: 16:59
 *         E-mail: DenBond7@gmail.com
 */

public class ImportPrivateKeyActivity extends BaseImportKeyActivity {

    private static final int REQUEST_CODE_CHECK_PRIVATE_KEYS = 100;

    @Override
    public void onKeyFromFileValidated() {
        startActivityForResult(CheckKeysActivity.newIntent(this,
                new ArrayList<>(Arrays.asList(new
                        KeyDetails[]{keyDetails})),
                getString(R.string.template_check_key_name,
                        keyDetails.getKeyName()),
                getString(R.string.continue_),
                getString(R.string.choose_another_key), isThrowErrorIfDuplicateFound),
                REQUEST_CODE_CHECK_PRIVATE_KEYS);
    }

    @Override
    public void onKeyFromClipBoardValidated() {
        startActivityForResult(CheckKeysActivity.newIntent(this,
                new ArrayList<>(Arrays.asList(new
                        KeyDetails[]{keyDetails})),
                getString(R.string.loaded_private_key_from_your_clipboard),
                getString(R.string.continue_),
                getString(R.string.choose_another_key), isThrowErrorIfDuplicateFound),
                REQUEST_CODE_CHECK_PRIVATE_KEYS);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CHECK_PRIVATE_KEYS:
                isCheckClipboardFromServiceEnable = false;

                switch (resultCode) {
                    case Activity.RESULT_OK:
                        setResult(Activity.RESULT_OK);
                        finish();
                        break;
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean isPrivateKeyChecking() {
        return true;
    }
}
