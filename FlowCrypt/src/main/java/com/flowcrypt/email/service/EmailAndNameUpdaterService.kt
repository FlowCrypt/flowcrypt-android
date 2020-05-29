/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.dao.ContactsDao
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
  private var contactsDao: ContactsDao = FlowCryptRoomDatabase.getDatabase(this).contactsDao()

  override fun onHandleWork(intent: Intent) {
    val pairs = intent.getParcelableArrayListExtra<EmailAndNamePair>(EXTRA_KEY_LIST_OF_PAIRS_EMAIL_NAME)
        ?: return

    for (pair in pairs) {
      val email = pair.email?.toLowerCase(Locale.getDefault()) ?: continue
      val contactEntity = contactsDao.getContactByEmails(email)
      if (contactEntity != null) {
        if (contactEntity.name.isNullOrEmpty()) {
          contactsDao.update(contactEntity.copy(name = pair.name))
        }
      } else {
        contactsDao.insert(PgpContact(email, pair.name).toContactEntity())
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
    fun enqueueWork(context: Context, emailAndNamePairs: ArrayList<EmailAndNamePair>?) {
      if (emailAndNamePairs != null && emailAndNamePairs.isNotEmpty()) {
        val intent = Intent(context, EmailAndNameUpdaterService::class.java)
        intent.putExtra(EXTRA_KEY_LIST_OF_PAIRS_EMAIL_NAME, emailAndNamePairs)

        enqueueWork(context, EmailAndNameUpdaterService::class.java, JobIdManager.JOB_TYPE_EMAIL_AND_NAME_UPDATE,
            intent)
      }
    }
  }
}
