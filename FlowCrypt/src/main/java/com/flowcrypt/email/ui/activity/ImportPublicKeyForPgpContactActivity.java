/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.ui.activity.base.BaseImportKeyActivity;
import com.flowcrypt.email.util.GeneralUtil;

import androidx.annotation.Nullable;

/**
 * This activity describes a logic of import public keys.
 *
 * @author Denis Bondarenko
 * Date: 03.08.2017
 * Time: 12:35
 * E-mail: DenBond7@gmail.com
 */

public class ImportPublicKeyForPgpContactActivity extends BaseImportKeyActivity {
  public static final String KEY_EXTRA_PGP_CONTACT
      = GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_PGP_CONTACT",
      ImportPublicKeyForPgpContactActivity.class);

  private PgpContact pgpContact;

  public static Intent newIntent(Context context, String title, PgpContact pgpContact) {
    Intent intent = newIntent(context, title, false, ImportPublicKeyForPgpContactActivity.class);
    intent.putExtra(KEY_EXTRA_PGP_CONTACT, pgpContact);
    return intent;
  }

  @Override
  public int getContentViewResourceId() {
    return R.layout.activity_import_public_key_for_pgp_contact;
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
  public void onKeyValidated(KeyDetails.Type type) {
    if (!keyDetailsList.isEmpty()) {
      if (keyDetailsList.size() == 1) {
        updateInformationAboutPgpContact();
        setResult(Activity.RESULT_OK);
        finish();
      } else {
        showInfoSnackbar(getRootView(), getString(R.string.select_only_one_key));
      }
    } else {
      showInfoSnackbar(getRootView(), getString(R.string.unknown_error));
    }
  }

  @Override
  public boolean isPrivateKeyMode() {
    return false;
  }

  protected void updateInformationAboutPgpContact() {
    ContactsDaoSource contactsDaoSource = new ContactsDaoSource();
    KeyDetails keyDetails = keyDetailsList.get(0);

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
