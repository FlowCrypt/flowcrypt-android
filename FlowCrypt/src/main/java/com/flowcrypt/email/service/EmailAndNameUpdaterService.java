/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.jobscheduler.JobIdManager;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.EmailAndNamePair;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

/**
 * This service does update a name of some email entry or creates a new email entry if it not
 * exists.
 * <p>
 * Used a next logic:
 * <ul>
 * <li> if email in db:
 * <ul>
 * <li>if db_row.name is null and bool(name) == true:
 * "save that person's name into the existing DB record"
 * </ul>
 * <li> else:
 * "save that email, name pair into DB like so: new PgpContact(email, name);"
 * </ul>
 *
 * @author DenBond7
 * Date: 22.05.2017
 * Time: 22:25
 * E-mail: DenBond7@gmail.com
 */

public class EmailAndNameUpdaterService extends JobIntentService {
  private static final String EXTRA_KEY_LIST_OF_PAIRS_EMAIL_NAME = BuildConfig.APPLICATION_ID +
      ".EXTRA_KEY_LIST_OF_PAIRS_EMAIL_NAME";
  private ContactsDaoSource contactsDaoSource;

  /**
   * Enqueue a new task for {@link EmailAndNameUpdaterService}.
   *
   * @param context           Interface to global information about an application environment.
   * @param emailAndNamePairs A list of EmailAndNamePair objects.
   */
  public static void enqueueWork(Context context, ArrayList<EmailAndNamePair> emailAndNamePairs) {
    if (emailAndNamePairs != null && !emailAndNamePairs.isEmpty()) {
      Intent intent = new Intent(context, EmailAndNameUpdaterService.class);
      intent.putExtra(EXTRA_KEY_LIST_OF_PAIRS_EMAIL_NAME, emailAndNamePairs);

      enqueueWork(context, EmailAndNameUpdaterService.class, JobIdManager.JOB_TYPE_EMAIL_AND_NAME_UPDATE, intent);
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();
    contactsDaoSource = new ContactsDaoSource();
  }

  @Override
  protected void onHandleWork(@NonNull Intent intent) {
    if (intent.hasExtra(EXTRA_KEY_LIST_OF_PAIRS_EMAIL_NAME)) {
      ArrayList<EmailAndNamePair> pairs = intent.getParcelableArrayListExtra(EXTRA_KEY_LIST_OF_PAIRS_EMAIL_NAME);

      for (EmailAndNamePair pair : pairs) {
        PgpContact pgpContact = contactsDaoSource.getPgpContact(getApplicationContext(),
            pair.getEmail());
        if (pgpContact != null) {
          if (TextUtils.isEmpty(pgpContact.getName())) {
            contactsDaoSource.updateNameOfPgpContact(getApplicationContext(), pair.getEmail(), pair.getName());
          }
        } else {
          contactsDaoSource.addRow(getApplicationContext(), new PgpContact(pair.getEmail(), pair.getName()));
        }
      }
    }
  }
}
