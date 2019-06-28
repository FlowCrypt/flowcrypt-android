/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import androidx.core.app.JobIntentService
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.database.dao.source.ContactsDaoSource
import com.flowcrypt.email.jobscheduler.JobIdManager
import com.flowcrypt.email.model.EmailAndNamePair
import com.flowcrypt.email.model.PgpContact
import java.util.*

/**
 * This service does update a name of some email entry or creates a new email entry if it not
 * exists.
 *
 *
 * Used a next logic:
 *
 *  *  if email in db:
 *
 *  * if db_row.name is null and bool(name) == true:
 * "save that person's name into the existing DB record"
 *
 *  *  else:
 * "save that email, name pair into DB like so: new PgpContact(email, name);"
 *
 *
 * @author DenBond7
 * Date: 22.05.2017
 * Time: 22:25
 * E-mail: DenBond7@gmail.com
 */

class EmailAndNameUpdaterService : JobIntentService() {
  private var contactsDaoSource: ContactsDaoSource = ContactsDaoSource()

  override fun onHandleWork(intent: Intent) {
    if (intent.hasExtra(EXTRA_KEY_LIST_OF_PAIRS_EMAIL_NAME)) {
      val pairs = intent.getParcelableArrayListExtra<EmailAndNamePair>(EXTRA_KEY_LIST_OF_PAIRS_EMAIL_NAME)

      for ((email, name) in pairs) {
        val pgpContact = contactsDaoSource.getPgpContact(applicationContext, email)
        if (pgpContact != null) {
          if (TextUtils.isEmpty(pgpContact.name)) {
            email?.let { contactsDaoSource.updateNameOfPgpContact(applicationContext, it, name) }
          }
        } else {
          contactsDaoSource.addRow(applicationContext, PgpContact(email!!, name))
        }
      }
    }
  }

  companion object {
    private const val EXTRA_KEY_LIST_OF_PAIRS_EMAIL_NAME =
        BuildConfig.APPLICATION_ID + ".EXTRA_KEY_LIST_OF_PAIRS_EMAIL_NAME"

    /**
     * Enqueue a new task for [EmailAndNameUpdaterService].
     *
     * @param context           Interface to global information about an application environment.
     * @param emailAndNamePairs A list of EmailAndNamePair objects.
     */
    @JvmStatic
    fun enqueueWork(context: Context, emailAndNamePairs: ArrayList<EmailAndNamePair>?) {
      if (emailAndNamePairs != null && !emailAndNamePairs.isEmpty()) {
        val intent = Intent(context, EmailAndNameUpdaterService::class.java)
        intent.putExtra(EXTRA_KEY_LIST_OF_PAIRS_EMAIL_NAME, emailAndNamePairs)

        enqueueWork(context, EmailAndNameUpdaterService::class.java, JobIdManager.JOB_TYPE_EMAIL_AND_NAME_UPDATE,
            intent)
      }
    }
  }
}
