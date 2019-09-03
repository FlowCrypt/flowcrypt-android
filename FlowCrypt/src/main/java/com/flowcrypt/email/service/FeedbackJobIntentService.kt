/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.core.app.JobIntentService
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.ApiService
import com.flowcrypt.email.api.retrofit.request.model.PostHelpFeedbackModel
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.jobscheduler.JobIdManager
import com.flowcrypt.email.util.GeneralUtil
import com.google.gson.GsonBuilder
import okhttp3.internal.cache.DiskLruCache
import okhttp3.internal.io.FileSystem
import okio.Okio
import org.apache.commons.io.IOUtils
import org.rm3l.maoni.common.model.Feedback
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * This service sends a user feedback to our API
 *
 * @author Denis Bondarenko
 *         Date: 9/3/19
 *         Time: 9:09 AM
 *         E-mail: DenBond7@gmail.com
 */
class FeedbackJobIntentService : JobIntentService() {
  private lateinit var diskLruCache: DiskLruCache
  private val gson = GsonBuilder().create()

  override fun onCreate() {
    super.onCreate()
    initFeedbackCache(this)
  }

  private fun initFeedbackCache(context: Context) {
    diskLruCache = DiskLruCache.create(FileSystem.SYSTEM, File(context.cacheDir, CACHE_DIR_NAME),
        CACHE_VERSION, 1, CACHE_SIZE)
  }

  override fun onHandleWork(intent: Intent) {
    addFeedbackFromIntentToCache(intent)

    if (GeneralUtil.isConnected(this)) {
      sendCachedFeedback()
    }
  }

  private fun addFeedbackFromIntentToCache(intent: Intent) {
    val account = intent.getParcelableExtra<AccountDao>(EXTRA_KEY_ACCOUNT)
    val feedbackMsg = intent.getStringExtra(EXTRA_KEY_FEEDBACK_MSG)
    val logsUri = intent.getParcelableExtra<Uri>(EXTRA_KEY_LOGS_URI)
    val screenShotUri = intent.getParcelableExtra<Uri>(EXTRA_KEY_SCREENSHOT_URI)

    val logs = if (logsUri != null) {
      IOUtils.toString(contentResolver.openInputStream(logsUri), StandardCharsets.UTF_8)
    } else {
      null
    }

    val screenShot = if (screenShotUri != null) {
      Base64.encodeToString(contentResolver.openInputStream(screenShotUri)?.readBytes(), Base64.DEFAULT)
    } else {
      null
    }

    feedbackMsg?.let {
      addFeedbackToCache(UUID.randomUUID().toString(),
          FeedBackItem(account?.email, feedbackMsg, logs, screenShot))
    }
  }

  private fun addFeedbackToCache(key: String, feedBackItem: FeedBackItem) {
    val editor = diskLruCache.edit(key) ?: return

    val bufferedSink = Okio.buffer(editor.newSink(0))
    bufferedSink.writeString(gson.toJson(feedBackItem), StandardCharsets.UTF_8)
    bufferedSink.flush()
    editor.commit()
    bufferedSink.close()
  }

  private fun sendCachedFeedback() {
    val itemsIterator = diskLruCache.snapshots()
    val apiService = ApiHelper.getInstance(this).retrofit.create(ApiService::class.java)

    while (itemsIterator.hasNext()) {
      val item = itemsIterator.next()
      val bufferedSource = Okio.buffer(item.getSource(0))
      val inputStreamReader = InputStreamReader(bufferedSource.inputStream())
      val feedBackItem = gson.fromJson<FeedBackItem>(inputStreamReader, FeedBackItem::class.java)
      bufferedSource.close()

      val response = with(feedBackItem) {
        apiService
            .postHelpFeedback(PostHelpFeedbackModel(email ?: "", logs, screenShot, feedbackMsg))
            .execute()
      }

      if (response.isSuccessful) {
        val body = response.body()
        if (body?.isSent == true) {
          diskLruCache.remove(item.key())
        }
      }
    }
  }

  companion object {
    private val EXTRA_KEY_ACCOUNT =
        GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_ACCOUNT", FeedbackJobIntentService::class.java)
    private val EXTRA_KEY_FEEDBACK_MSG =
        GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_FEEDBACK_MSG", FeedbackJobIntentService::class.java)
    private val EXTRA_KEY_LOGS_URI =
        GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_LOGS_URI", FeedbackJobIntentService::class.java)
    private val EXTRA_KEY_SCREENSHOT_URI =
        GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_SCREENSHOT_URI", FeedbackJobIntentService::class.java)

    private const val CACHE_VERSION = 1
    private const val CACHE_SIZE: Long = 1024 * 1000 * 3 //3Mb
    private const val CACHE_DIR_NAME = "feedback"

    /**
     * Enqueue a new task for [FeedbackJobIntentService]. Set the feedback param to null to
     * enqueue checking of non-sent feedbacks
     *
     * @param context   Interface to global information about an application environment.
     * @param account   An active account.
     * @param feedback  A feedback which will be sent.
     */
    @JvmStatic
    fun enqueueWork(context: Context, account: AccountDao?, feedback: Feedback?) {
      val intent = Intent(context, FeedbackJobIntentService::class.java)
      intent.putExtra(EXTRA_KEY_ACCOUNT, account)

      if (feedback != null) {
        intent.putExtra(EXTRA_KEY_FEEDBACK_MSG, feedback.userComment)

        val logsUri = if (feedback.logsFile != null) {
          Uri.fromFile(feedback.logsFile)
        } else {
          feedback.logsFileUri
        }
        intent.putExtra(EXTRA_KEY_LOGS_URI, logsUri)

        val screenShotUri = if (feedback.screenshotFile != null) {
          Uri.fromFile(feedback.screenshotFile)
        } else {
          feedback.screenshotFileUri
        }
        intent.putExtra(EXTRA_KEY_SCREENSHOT_URI, screenShotUri)
      }
      enqueueWork(context, FeedbackJobIntentService::class.java,
          JobIdManager.JOB_TYPE_FEEDBACK, intent)
    }
  }

  /**
   * It's data class which describes info about a feedback
   */
  data class FeedBackItem(val email: String?,
                          val feedbackMsg: String,
                          val logs: String?,
                          val screenShot: String?)
}