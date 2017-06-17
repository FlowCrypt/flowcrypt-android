/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.model.EmailAndNamePair;
import com.flowcrypt.email.js.PgpContact;

import java.util.ArrayList;

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
 *         Date: 22.05.2017
 *         Time: 22:25
 *         E-mail: DenBond7@gmail.com
 */

public class EmailAndNameUpdaterService extends IntentService {
    private static final String EXTRA_KEY_LIST_OF_PAIRS_EMAIL_NAME = BuildConfig.APPLICATION_ID +
            ".EXTRA_KEY_LIST_OF_PAIRS_EMAIL_NAME";
    private ContactsDaoSource contactsDaoSource;

    /**
     * Creates an IntentService.
     */
    public EmailAndNameUpdaterService() {
        super(EmailAndNameUpdaterService.class.getSimpleName());
    }

    /**
     * Generate a new intent to start {@link EmailAndNameUpdaterService}.
     *
     * @param context           Interface to global information about an application environment.
     * @param emailAndNamePairs A list of EmailAndNamePair objects.
     * @return <tt>{@link Intent}</tt> which will be start {@link EmailAndNameUpdaterService}.
     */
    public static Intent getStartIntent(Context context,
                                        ArrayList<EmailAndNamePair> emailAndNamePairs) {
        Intent intent = new Intent(context, EmailAndNameUpdaterService.class);
        intent.putExtra(EXTRA_KEY_LIST_OF_PAIRS_EMAIL_NAME, emailAndNamePairs);
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        contactsDaoSource = new ContactsDaoSource();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null && intent.hasExtra(EXTRA_KEY_LIST_OF_PAIRS_EMAIL_NAME)) {
            ArrayList<EmailAndNamePair> emailAndNamePairs = intent.getParcelableArrayListExtra
                    (EXTRA_KEY_LIST_OF_PAIRS_EMAIL_NAME);

            for (EmailAndNamePair emailAndNamePair : emailAndNamePairs) {
                PgpContact pgpContact = contactsDaoSource.getPgpContact(getApplicationContext(),
                        emailAndNamePair.getEmail());
                if (pgpContact != null) {
                    if (TextUtils.isEmpty(pgpContact.getName())) {
                        contactsDaoSource.updateNameOfPgpContact(getApplicationContext(),
                                emailAndNamePair.getEmail(),
                                emailAndNamePair.getName());
                    }
                } else {
                    contactsDaoSource.addRow(getApplicationContext(), new PgpContact
                            (emailAndNamePair.getEmail(), emailAndNamePair.getName()));
                }
            }
        }
    }
}
