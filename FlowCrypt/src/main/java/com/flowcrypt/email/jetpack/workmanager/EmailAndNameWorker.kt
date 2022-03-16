/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.dao.RecipientDao
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.jobscheduler.JobIdManager
import com.flowcrypt.email.model.EmailAndNamePair

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
 * "save that email, name pair into DB like so: new RecipientEntity(email, name);"
 *
 *
 * @author DenBond7
 * Date: 22.05.2017
 * Time: 22:25
 * E-mail: DenBond7@gmail.com
 */
class EmailAndNameWorker : JobIntentService() {
  private var recipientDao: RecipientDao = FlowCryptRoomDatabase.getDatabase(this).recipientDao()

  override fun onHandleWork(intent: Intent) {
    val pairs =
      intent.getParcelableArrayListExtra<EmailAndNamePair>(EXTRA_KEY_LIST_OF_PAIRS_EMAIL_NAME)
        ?: return

    for (pair in pairs) {
      val email = pair.email?.lowercase() ?: continue
      val recipientEntity = recipientDao.getRecipientByEmail(email)
      if (recipientEntity != null) {
        if (recipientEntity.name.isNullOrEmpty()) {
          recipientDao.update(recipientEntity.copy(name = pair.name))
        }
      } else {
        recipientDao.insert(RecipientEntity(email = email, name = pair.name))
      }
    }
  }

  companion object {
    private const val EXTRA_KEY_LIST_OF_PAIRS_EMAIL_NAME =
      BuildConfig.APPLICATION_ID + ".EXTRA_KEY_LIST_OF_PAIRS_EMAIL_NAME"

    /**
     * Enqueue a new task for [EmailAndNameWorker].
     *
     * @param context           Interface to global information about an application environment.
     * @param emailAndNamePairs A list of EmailAndNamePair objects.
     */
    fun enqueueWork(context: Context, emailAndNamePairs: ArrayList<EmailAndNamePair>?) {
      if (emailAndNamePairs != null && emailAndNamePairs.isNotEmpty()) {
        val intent = Intent(context, EmailAndNameWorker::class.java)
        intent.putExtra(EXTRA_KEY_LIST_OF_PAIRS_EMAIL_NAME, emailAndNamePairs)

        enqueueWork(
          context,
          EmailAndNameWorker::class.java,
          JobIdManager.JOB_TYPE_EMAIL_AND_NAME_UPDATE,
          intent
        )
      }
    }
  }
}
