/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.ui.activity.base.BaseImportKeyActivity;
import com.flowcrypt.email.util.GeneralUtil;

/**
 * This activity describes a logic of import public keys.
 *
 * @author Denis Bondarenko
 *         Date: 03.08.2017
 *         Time: 12:35
 *         E-mail: DenBond7@gmail.com
 */

public class ImportPublicKeyActivity extends BaseImportKeyActivity {
    public static final String KEY_EXTRA_PGP_CONTACT
            = GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_PGP_CONTACT",
            ImportPublicKeyActivity.class);

    private PgpContact pgpContact;

    public static Intent newIntent(Context context, String title, PgpContact pgpContact) {
        Intent intent = newIntent(context, title, false, ImportPublicKeyActivity.class);
        intent.putExtra(KEY_EXTRA_PGP_CONTACT, pgpContact);
        return intent;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null && getIntent().hasExtra(KEY_EXTRA_PGP_CONTACT)) {
            this.pgpContact = getIntent().getParcelableExtra(KEY_EXTRA_PGP_CONTACT);
        } else {
            finish();
        }
    }

    @Override
    public void onKeyFromFileValidated() {
        updateInformationAboutPgpContact();
        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onKeyFromClipBoardValidated() {
        updateInformationAboutPgpContact();
        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public boolean isPrivateKeyChecking() {
        return false;
    }

    protected void updateInformationAboutPgpContact() {
        ContactsDaoSource contactsDaoSource = new ContactsDaoSource();
        if (pgpContact.getEmail().equalsIgnoreCase(keyDetails.getPgpContact().getEmail())) {
            pgpContact.setPubkey(keyDetails.getValue());
            contactsDaoSource.updatePgpContact(this, pgpContact);
        } else {
            pgpContact.setPubkey(keyDetails.getValue());
            contactsDaoSource.updatePgpContact(this, pgpContact);

            keyDetails.getPgpContact().setPubkey(keyDetails.getValue());
            contactsDaoSource.addRow(this, keyDetails.getPgpContact());
        }
    }
}
