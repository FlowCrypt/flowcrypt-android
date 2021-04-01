/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.util.gson.GsonHelper
import com.google.gson.Gson
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * @author Denis Bondarenko
 * Date: 18.01.2018
 * Time: 13:02
 * E-mail: DenBond7@gmail.com
 */
class TestGeneralUtil {
  companion object {
    @JvmStatic
    fun <T> readObjectFromResources(path: String, aClass: Class<T>): T {
      val json = IOUtils.toString(aClass.classLoader!!.getResourceAsStream(path), StandardCharsets.UTF_8)
      return Gson().fromJson(json, aClass)
    }

    @JvmStatic
    fun readObjectFromResourcesAsByteArray(path: String): ByteArray {
      return IOUtils.toByteArray(TestGeneralUtil::class.java.classLoader!!.getResourceAsStream(path))
    }

    @JvmStatic
    fun readResourcesAsString(path: String): String {
      return IOUtils.toString(TestGeneralUtil::class.java.classLoader!!.getResourceAsStream(path), StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun readResourcesAsStream(path: String): InputStream {
      return TestGeneralUtil::class.java.classLoader!!.getResourceAsStream(path)
    }

    @JvmStatic
    fun readFileFromAssetsAsString(context: Context, filePath: String): String {
      return IOUtils.toString(context.assets.open(filePath), "UTF-8")
    }

    @JvmStatic
    fun readFileFromAssetsAsByteArray(context: Context, filePath: String): ByteArray {
      return IOUtils.toByteArray(context.assets.open(filePath))
    }

    @JvmStatic
    fun deleteFiles(files: List<File>) {
      files.forEach { file ->
        if (!file.delete()) {
          println("Can't delete a file $file")
        }
      }
    }

    @JvmStatic
    fun createFile(fileName: String, fileText: String): File {
      val file = File(InstrumentationRegistry.getInstrumentation().targetContext
          .getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
      try {
        FileOutputStream(file).use { outputStream -> outputStream.write(fileText.toByteArray()) }
      } catch (e: Exception) {
        e.printStackTrace()
      }

      return file
    }

    @JvmStatic
    fun <T> getObjectFromJson(jsonPathInAssets: String?, classOfT: Class<T>): T? {
      try {
        if (jsonPathInAssets != null) {
          val gson = GsonHelper.gson
          val json = readFileFromAssetsAsString(InstrumentationRegistry.getInstrumentation().context, jsonPathInAssets)
          return gson.fromJson(json, classOfT)
        }
      } catch (e: IOException) {
        e.printStackTrace()
      }

      return null
    }

    @JvmStatic
    fun replaceVersionInKey(key: String?): String {
      val regex = "Version: FlowCrypt \\d*.\\d*.\\d* Gmail".toRegex()
      val version = BuildConfig.VERSION_NAME.split("_").first()
      val replacement = "Version: FlowCrypt $version Gmail"

      key?.let {
        return key.replaceFirst(regex, replacement)
      }

      return ""
    }

    /**
     * Generate an [Intent] with [Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION]
     * and [Intent.FLAG_GRANT_READ_URI_PERMISSION]
     */
    fun genIntentWithPersistedReadPermissionForFile(file: File): Intent {
      return Intent().apply {
        val context: Context = ApplicationProvider.getApplicationContext()
        val uri = FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, file)
        context.grantUriPermission(BuildConfig.APPLICATION_ID, uri,
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_READ_URI_PERMISSION)
        data = uri
        flags = Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
            Intent.FLAG_GRANT_READ_URI_PERMISSION
      }
    }
  }
}
