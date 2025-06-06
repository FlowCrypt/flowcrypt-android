/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import android.text.TextUtils
import android.util.Patterns
import android.webkit.MimeTypeMap
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.retrofit.RetrofitApiServiceInterface
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.hasActiveConnection
import com.flowcrypt.email.jetpack.workmanager.EmailAndNameWorker
import com.flowcrypt.email.model.KeysStorage
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.notifications.ErrorNotificationManager
import com.flowcrypt.email.util.exception.CommonConnectionException
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.google.GoogleApiClientHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.internet.InternetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.apache.commons.io.IOUtils
import org.eclipse.angus.mail.imap.IMAPFolder
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import retrofit2.Retrofit
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * General util methods.
 *
 * @author Denys Bondarenko
 */
class GeneralUtil {
  companion object {
    /**
     * This method checks is it a debug or uiTests build.
     *
     * @return true - if the current build is a debug or uiTests build.
     */
    fun isDebugBuild(): Boolean = BuildConfig.BUILD_TYPE in listOf("debug", "uiTests")

    /**
     * Checking for an Internet connection.
     *
     * @param context Interface to global information about an application environment.
     * @return true - a connection available, false if otherwise.
     */
    fun isConnected(context: Context?): Boolean {
      val connectivityManager =
        context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
      val network = connectivityManager.activeNetwork ?: return false
      val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
      return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
          || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
          || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
          || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    suspend fun hasInternetAccess(): Boolean = withContext(Dispatchers.IO) {
      val url = "https://www.google.com"
      val connectionTimeoutInMilliseconds = 2000L
      val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(connectionTimeoutInMilliseconds, TimeUnit.MILLISECONDS)
        .writeTimeout(connectionTimeoutInMilliseconds, TimeUnit.MILLISECONDS)
        .readTimeout(connectionTimeoutInMilliseconds, TimeUnit.MILLISECONDS)
        .build()

      val retrofit = Retrofit.Builder()
        .baseUrl(url)
        .client(okHttpClient)
        .build()
      val retrofitApiService = retrofit.create(RetrofitApiServiceInterface::class.java)
      try {
        retrofitApiService.isAvailable(url)
        return@withContext true
      } catch (e: Exception) {
        return@withContext false
      }
    }

    /**
     * Show the application system settings screen.
     *
     * @param context Interface to global information about an application environment.
     */
    fun showAppSettingScreen(context: Context) {
      val intent = Intent()
      intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
      val uri = Uri.fromParts("package", context.packageName, null)
      intent.data = uri
      context.startActivity(intent)
    }

    /**
     * Read a file by his Uri and return him as [String].
     *
     * @param uri The [Uri] of the file.
     * @return <tt>[String]</tt> which contains a file.
     * @throws IOException will thrown for example if the file not found
     */
    fun readFileFromUriToString(context: Context, uri: Uri): String? {
      val inputStream = context.contentResolver.openInputStream(uri)
      return if (inputStream != null) {
        IOUtils.toString(inputStream, StandardCharsets.UTF_8)
      } else null
    }

    /**
     * Remove all comments from the given HTML [String].
     *
     * @param text The given string.
     * @return <tt>[String]</tt> which doesn't contain HTML comments.
     */
    fun removeAllComments(text: String): String {
      return if (TextUtils.isEmpty(text)) text else text.replace("<!--[\\s\\S]*?-->".toRegex(), "")
    }

    /**
     * Get a file size from his Uri.
     *
     * @param fileUri The [Uri] of the file.
     * @return The size of the file in bytes.
     */
    fun getFileSizeFromUri(context: Context, fileUri: Uri?): Long {
      var fileSize: Long = -1

      fileUri?.let {
        if (ContentResolver.SCHEME_FILE.equals(fileUri.scheme!!, ignoreCase = true)) {
          val path = it.path ?: return@let
          fileSize = File(path).length()
        } else {
          val returnCursor = context.contentResolver.query(
            fileUri,
            arrayOf(OpenableColumns.SIZE), null, null, null
          )

          if (returnCursor != null) {
            if (returnCursor.moveToFirst()) {
              val index = returnCursor.getColumnIndex(OpenableColumns.SIZE)
              if (index >= 0) {
                fileSize = returnCursor.getLong(index)
              }
            }

            returnCursor.close()
          }
        }
      }

      return fileSize
    }

    /**
     * Get a file name from his Uri.
     *
     * @param fileUri The [Uri] of the file.
     * @return The file name.
     */
    fun getFileNameFromUri(context: Context, fileUri: Uri?): String? {
      var fileName: String? = null
      fileUri?.let {
        if (ContentResolver.SCHEME_FILE.equals(fileUri.scheme!!, ignoreCase = true)) {
          val path = it.path ?: return@let
          fileName = File(path).name
        } else {
          val returnCursor = context.contentResolver.query(
            fileUri,
            arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
          )

          if (returnCursor != null) {
            if (returnCursor.moveToFirst()) {
              val index = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
              if (index >= 0) {
                fileName = returnCursor.getString(index)
              }
            }

            returnCursor.close()
          }
        }
      }

      return fileName
    }

    /**
     * Write to the file some data by his Uri and return him as [String].
     *
     * @param context Interface to global information about an application environment.
     * @param data    The data which will be written.
     * @param uri     The [Uri] of the file.
     * @return the number of bytes copied, or -1 if &gt; Integer.MAX_VALUE
     * @throws IOException if an I/O error occurs
     */
    fun writeFileFromStringToUri(context: Context, uri: Uri, data: String): Int {
      val inputStream = IOUtils.toInputStream(data, StandardCharsets.UTF_8)
      val outputStream = context.contentResolver.openOutputStream(uri)
      return IOUtils.copy(inputStream, outputStream)
    }

    /**
     * Generate an unique extra key using the application id and the class name.
     *
     * @param key The key of the new extra key.
     * @param c   The class where a new extra key will be created.
     * @return The new extra key.
     */
    fun generateUniqueExtraKey(key: String, c: Class<*>): String {
      return BuildConfig.APPLICATION_ID + "." + c.simpleName + "." + key
    }

    /**
     * Insert arbitrary string at regular interval into another string
     *
     * @param template       String template which will be inserted to the original string.
     * @param originalString The original string which will be formatted.
     * @param groupSize      Group size
     * @return The formatted string.
     */
    fun doSectionsInText(
      template: String? = " ",
      originalString: String?,
      groupSize: Int
    ): String? {

      if (template == null || originalString == null || groupSize <= 0 || originalString.length <= groupSize) {
        return originalString
      }

      val stringBuilder = StringBuilder(originalString)

      var i = stringBuilder.length
      while (i > 0) {
        stringBuilder.insert(i, template)
        i -= groupSize
      }
      return stringBuilder.toString()
    }

    /**
     * Check is current email valid.
     *
     * @param email The current email.
     * @return true if the email has valid format, otherwise false.
     */
    fun isEmailValid(email: CharSequence?): Boolean {
      email ?: return false
      val isLocalhostAddress = try {
        EmailUtil.getDomain(email.toString()).lowercase() == "localhost"
      } catch (e: IllegalArgumentException) {
        false
      }
      return Patterns.EMAIL_ADDRESS.matcher(email).matches() || isLocalhostAddress
    }

    /**
     * Get a mime type of the input [Uri]
     *
     * @param context Interface to global information about an application environment.
     * @param uri     The [Uri] of the file.
     * @return A mime type of of the [Uri].
     */
    fun getFileMimeTypeFromUri(context: Context, uri: Uri): String {
      return if (ContentResolver.SCHEME_CONTENT.equals(uri.scheme, ignoreCase = true)) {
        val contentResolver = context.contentResolver
        contentResolver.getType(uri) ?: Constants.MIME_TYPE_BINARY_DATA
      } else {
        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        MimeTypeMap.getSingleton()
          .getMimeTypeFromExtension(fileExtension.lowercase())
          ?: Constants.MIME_TYPE_BINARY_DATA
      }
    }

    /**
     * Generate a unique name for [androidx.test.espresso.IdlingResource]
     *
     * @param aClass The class where we will use [androidx.test.espresso.IdlingResource]
     * @return A generated name.
     */
    fun genIdlingResourcesName(aClass: Class<*>): String {
      return aClass.simpleName + "-" + UUID.randomUUID()
    }

    /**
     * Clear the [ClipData]
     *
     * @param context Interface to global information about an application environment.
     */
    @Suppress("unused")
    fun clearClipboard(context: Context) {
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      clipboard.setPrimaryClip(ClipData.newPlainText(null, ""))
    }

    /**
     * Generate order number for an attachment. This value will be used for the notifications ordering.
     *
     * @param context Interface to global information about an application environment.
     * @return The generated order number.
     */
    fun genAttOrderId(context: Context): Int {
      return SharedPreferencesHelper.getInt(
        PreferenceManager.getDefaultSharedPreferences(context),
        Constants.PREF_KEY_LAST_ATT_ORDER_ID, 0
      ).inc().apply {
        SharedPreferencesHelper.setInt(
          PreferenceManager.getDefaultSharedPreferences(context),
          Constants.PREF_KEY_LAST_ATT_ORDER_ID, this
        )
      }
    }

    /**
     * Open a Chrome Custom Tab with a predefined style.
     *
     * @param context Interface to global information about an application environment.
     * @param url The given url
     */
    fun openCustomTab(context: Context, url: String) {
      val builder = CustomTabsIntent.Builder()
      val customTabsIntent = builder.build()
      builder.setDefaultColorSchemeParams(
        CustomTabColorSchemeParams.Builder()
          .setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary))
          .build()
      )

      val intent = Intent(Intent.ACTION_VIEW)
      intent.data = url.toUri()
      if (intent.resolveActivity(context.packageManager) != null) {
        intent.data?.let {
          customTabsIntent.launchUrl(context, it)
        }
      }
    }

    /**
     * This function helps to get [Bitmap] from the given [Drawable]
     *
     * @param drawable The given drawable
     */
    fun drawableToBitmap(drawable: Drawable): Bitmap {
      if (drawable is BitmapDrawable) {
        if (drawable.bitmap != null) {
          return drawable.bitmap
        }
      }

      val bitmap: Bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
        createBitmap(1, 1)
      } else {
        createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
      }

      val canvas = Canvas(bitmap)
      drawable.setBounds(0, 0, canvas.width, canvas.height)
      drawable.draw(canvas)
      return bitmap
    }

    /**
     * Returns a language code of the current [Locale].
     */
    fun getLocaleLanguageCode(context: Context?): String {
      return when {
        context == null || context.resources.configuration.locales.isEmpty -> "en"
        else -> context.resources.configuration.locales.get(0).language.lowercase()
      }
    }

    /**
     * Generate a base FES URL path.
     */
    fun genBaseFesUrlPath(useCustomerFesUrl: Boolean, domain: String? = null): String {
      return if (useCustomerFesUrl && domain != null) {
        "fes.$domain"
        //for example fes.customer-domain.com
      } else {
        val url = URL(BuildConfig.SHARED_TENANT_FES_URL)
        (url.authority + url.path).replace("/$".toRegex(), "")
        //flowcrypt.com/shared-tenant-fes
      }
    }

    /**
     * Get recipients without usable public keys;
     *
     * @param context     Interface to global information about an application environment.
     * @param emails      A list which contains recipients
     */
    suspend fun getRecipientsWithoutUsablePubKeys(
      context: Context,
      emails: List<String>
    ): List<String> = withContext(Dispatchers.IO) {
      val mapOfRecipients = mutableMapOf(*emails.map { Pair(it, false) }.toTypedArray())
      val recipientsWithPubKeys = FlowCryptRoomDatabase.getDatabase(context).recipientDao()
        .getRecipientsWithPubKeysByEmailsSuspend(emails)

      for (recipientWithPubKeys in recipientsWithPubKeys) {
        for (publicKeyEntity in recipientWithPubKeys.publicKeys) {
          val pgpKeyDetailsList =
            PgpKey.parseKeys(source = publicKeyEntity.publicKey).pgpKeyDetailsList
          for (pgpKeyRingDetails in pgpKeyDetailsList) {
            if (!pgpKeyRingDetails.isExpired && !pgpKeyRingDetails.isRevoked) {
              mapOfRecipients[recipientWithPubKeys.recipient.email] = true
            }
          }
        }
      }

      return@withContext mapOfRecipients.filter { entry -> !entry.value }.keys.toList()
    }

    suspend fun notifyUserAboutProblemWithOutgoingMsgs(context: Context, account: AccountEntity) =
      withContext(Dispatchers.IO) {
        val failedOutgoingMsgsCount = FlowCryptRoomDatabase.getDatabase(context).msgDao()
          .getFailedOutgoingMessagesCountSuspend(account.email)
        if (failedOutgoingMsgsCount > 0) {
          ErrorNotificationManager(context).notifyUserAboutProblemWithOutgoingMessages(
            account,
            failedOutgoingMsgsCount
          )
        }
      }

    fun genViewAttachmentIntent(
      uri: Uri,
      attachmentInfo: AttachmentInfo,
      useCommonPattern: Boolean = false
    ): Intent {
      return Intent.createChooser(
        Intent()
          .setAction(Intent.ACTION_VIEW)
          .setDataAndType(
            uri,
            if (useCommonPattern) {
              "*/*"
            } else {
              Intent.normalizeMimeType(attachmentInfo.type)
            }
          )
          .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null
      )
    }

    suspend fun getGoogleIdTokenSilently(
      context: Context,
      maxRetryAttemptCount: Int = 0,
      retryAttempt: Int = 0,
      accountEntity: AccountEntity
    ): String =
      withContext(Dispatchers.IO) {
        //before fetch idToken from [GoogleSignInClient]
        //we try to get IdToken from the flavor settings
        @Suppress("UNNECESSARY_SAFE_CALL", "KotlinRedundantDiagnosticSuppress")
        FlavorSettings.getGoogleIdToken()?.let { return@withContext it }

        val googleSignInClient = GoogleSignIn.getClient(
          context,
          GoogleApiClientHelper.generateGoogleSignInOptions(accountEntity.account)
        )
        val silentSignIn = googleSignInClient.silentSignIn()
        if (!silentSignIn.isSuccessful || silentSignIn.result.isExpired) {
          if (retryAttempt <= maxRetryAttemptCount) {
            //do delay for 10 seconds and try again. Max attempts == maxRetryAttemptCount
            delay(TimeUnit.SECONDS.toMillis(10))
            return@withContext getGoogleIdTokenSilently(
              context,
              maxRetryAttemptCount,
              retryAttempt + 1,
              accountEntity
            )
          } else throw IllegalStateException("Could not receive idToken")
        }

        val claims = JwtConsumerBuilder()
          .setExpectedAudience(GoogleApiClientHelper.SERVER_CLIENT_ID)
          .setRequireIssuedAt()
          .setRequireExpirationTime()
          .setRelaxVerificationKeyValidation()
          .setSkipSignatureVerification()
          .build()
          .processToClaims(silentSignIn.result.idToken)

        val email = claims.getClaimValueAsString("email")

        if (!accountEntity.email.equals(email, true)) {
          throw IllegalStateException("Received tokenId for a wrong account($email)")
        }

        return@withContext requireNotNull(silentSignIn.result.idToken)
      }

    suspend fun preProcessException(
      context: Context,
      causedException: Throwable
    ): Throwable = withContext(Dispatchers.IO) {
      return@withContext when (causedException) {
        is UnknownHostException, is SocketTimeoutException, is ConnectException -> {
          if (context.hasActiveConnection()) {
            CommonConnectionException(
              cause = causedException,
              hasInternetAccess = hasInternetAccess()
            )
          } else {
            CommonConnectionException(cause = causedException, hasInternetAccess = false)
          }
        }

        else -> causedException
      }
    }


    fun prepareWarningTextAboutUnusableForEncryptionKeys(
      context: Context,
      keysStorage: KeysStorage
    ): String {
      val stringBuilder = StringBuilder()
      stringBuilder.append("<ul>")

      keysStorage.getPgpKeyDetailsList().forEach { pgpKeyRingDetails ->
        stringBuilder.append("<li>")
        val fingerprint = doSectionsInText(
          originalString = pgpKeyRingDetails.fingerprint, groupSize = 4
        )
        stringBuilder.append("<b>$fingerprint</b> - ")
        val status = pgpKeyRingDetails.getStatusText(context)
        val colorStateList = pgpKeyRingDetails.getColorStateListDependsOnStatus(context)
        val backgroundColor = colorStateList?.defaultColor ?: Color.BLACK
        val backgroundColorHex = String.format("#%06X", 0xFFFFFF and backgroundColor)
        stringBuilder.append(
          "<span style=\"background-color:$backgroundColorHex\">&nbsp;" +
              "<font color=\"#ffffff\">$status</font>&nbsp;</span>"
        )
        stringBuilder.append("</li>")
      }

      stringBuilder.append("</ul>")

      val preamble = context.getString(R.string.no_private_keys_suitable_for_encryption)
      return "$preamble <br><br> $stringBuilder"
    }

    suspend fun updateLocalContactsIfNeeded(
      context: Context,
      imapFolder: IMAPFolder? = null,
      messages: Array<Message>
    ) = withContext(Dispatchers.IO) {
      try {
        val isSentFolder = imapFolder?.attributes?.contains("\\Sent") != false

        if (isSentFolder) {
          val emailAndNamePairs = mutableListOf<Pair<String, String>>()
          for (message in messages) {
            emailAndNamePairs.addAll(getEmailAndNamePairs(message))
          }

          EmailAndNameWorker.enqueue(context, emailAndNamePairs)
        }
      } catch (e: MessagingException) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
      }
    }

    /**
     * Generate a list of [Pair] objects from the input message.
     * This information will be retrieved from "to" and "cc" headers.
     *
     * @param msg The input [jakarta.mail.Message].
     * @return <tt>[List]</tt> of [Pair] objects, which contains information about emails and names.
     * @throws MessagingException when retrieve information about recipients.
     */
    private fun getEmailAndNamePairs(msg: Message): List<Pair<String, String>> {
      val pairs = mutableListOf<Pair<String, String>>()

      val addressesTo = msg.getRecipients(Message.RecipientType.TO)
      if (addressesTo != null) {
        for (address in addressesTo) {
          val internetAddress = address as InternetAddress
          pairs.add(Pair(internetAddress.address, internetAddress.personal ?: ""))
        }
      }

      val addressesCC = msg.getRecipients(Message.RecipientType.CC)
      if (addressesCC != null) {
        for (address in addressesCC) {
          val internetAddress = address as InternetAddress
          pairs.add(Pair(internetAddress.address, internetAddress.personal ?: ""))
        }
      }

      return pairs
    }
  }
}
