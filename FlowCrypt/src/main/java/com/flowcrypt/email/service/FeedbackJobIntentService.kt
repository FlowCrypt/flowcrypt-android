/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service

import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.core.app.JobIntentService
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.ApiService
import com.flowcrypt.email.api.retrofit.request.model.PostHelpFeedbackModel
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jobscheduler.JobIdManager
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.cache.DiskLruCache
import com.google.gson.GsonBuilder
import okhttp3.internal.io.FileSystem
import okio.buffer
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
    diskLruCache = DiskLruCache(FileSystem.SYSTEM, File(context.cacheDir, CACHE_DIR_NAME), CACHE_VERSION, 1, CACHE_SIZE)
  }

  override fun onHandleWork(intent: Intent) {
    addFeedbackFromIntentToCache(intent)

    if (GeneralUtil.isConnected(this)) {
      sendCachedFeedback()
    }
  }

  private fun addFeedbackFromIntentToCache(intent: Intent) {
    val account = intent.getParcelableExtra<AccountEntity>(EXTRA_KEY_ACCOUNT)
    val feedbackMsg = intent.getStringExtra(EXTRA_KEY_FEEDBACK_MSG)
    val screenShotBytes = intent.getByteArrayExtra(EXTRA_KEY_SCREENSHOT_BYTES)
    val screenShotBase64 = Base64.encodeToString(screenShotBytes ?: byteArrayOf(), Base64.DEFAULT)

    feedbackMsg?.let {
      addFeedbackToCache(UUID.randomUUID().toString(),
          FeedBackItem(account?.email, feedbackMsg, screenShotBase64))
    }
  }

  private fun addFeedbackToCache(key: String, feedBackItem: FeedBackItem) {
    val editor = diskLruCache.edit(key) ?: return

    val bufferedSink = editor.newSink(0).buffer()
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
      val bufferedSource = item.getSource(0).buffer()
      val inputStreamReader = InputStreamReader(bufferedSource.inputStream())
      val feedBackItem = gson.fromJson<FeedBackItem>(inputStreamReader, FeedBackItem::class.java)
      bufferedSource.close()

      val response = with(feedBackItem) {
        apiService
            .postHelpFeedback(PostHelpFeedbackModel(email ?: "", "", screenShot, feedbackMsg))
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
    private val EXTRA_KEY_SCREENSHOT_BYTES =
        GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_SCREENSHOT_BYTES", FeedbackJobIntentService::class.java)

    private const val CACHE_VERSION = 1
    private const val CACHE_SIZE: Long = 1024 * 1000 * 3 //3Mb
    private const val CACHE_DIR_NAME = "feedback"

    /**
     * Enqueue a new task for [FeedbackJobIntentService]. Set the feedback param to null to
     * enqueue checking of non-sent feedbacks
     *
     * @param context   Interface to global information about an application environment.
     * @param account   An active account.
     * @param userComment  A feedback which will be sent.
     * @param screenShotBytes  A screenshot bytes array.
     */
    @JvmStatic
    fun enqueueWork(context: Context, account: AccountEntity? = null, userComment: String? = null,
                    screenShotBytes: ByteArray? = null) {
      val intent = Intent(context, FeedbackJobIntentService::class.java)
      intent.putExtra(EXTRA_KEY_ACCOUNT, account)
      intent.putExtra(EXTRA_KEY_FEEDBACK_MSG, userComment)
      intent.putExtra(EXTRA_KEY_SCREENSHOT_BYTES, screenShotBytes)
      enqueueWork(context, FeedbackJobIntentService::class.java,
          JobIdManager.JOB_TYPE_FEEDBACK, intent)
    }
  }

  /**
   * It's data class which describes info about a feedback
   */
  data class FeedBackItem(val email: String?,
                          val feedbackMsg: String,
                          val screenShot: String?)
}