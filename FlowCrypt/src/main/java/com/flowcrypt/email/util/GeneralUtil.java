/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.flowcrypt.email.BuildConfig;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;

/**
 * General util methods.
 *
 * @author DenBond7
 * Date: 31.03.2017
 * Time: 16:55
 * E-mail: DenBond7@gmail.com
 */

public class GeneralUtil {
  /**
   * Checking for an Internet connection.
   *
   * @param context Interface to global information about an application environment.
   * @return <tt>boolean</tt> true - a connection available, false if otherwise.
   */
  public static boolean isInternetConnectionAvailable(Context context) {
    ConnectivityManager connectivityManager =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

    NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
    return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
  }

  /**
   * Show the application system settings screen.
   *
   * @param context Interface to global information about an application environment.
   */
  public static void showAppSettingScreen(Context context) {
    Intent intent = new Intent();
    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    Uri uri = Uri.fromParts("package", context.getPackageName(), null);
    intent.setData(uri);
    context.startActivity(intent);
  }

  /**
   * Read a file by his Uri and return him as {@link String}.
   *
   * @param uri The {@link Uri} of the file.
   * @return <tt>{@link String}</tt> which contains a file.
   * @throws IOException will thrown for example if the file not found
   */
  public static String readFileFromUriToString(Context context, Uri uri) throws IOException {
    return IOUtils.toString(context.getContentResolver().openInputStream(uri),
        StandardCharsets.UTF_8);
  }

  /**
   * Remove all comments from the given HTML {@link String}.
   *
   * @param text The given string.
   * @return <tt>{@link String}</tt> which doesn't contain HTML comments.
   */
  public static String removeAllCommentsInHTML(String text) {
    return TextUtils.isEmpty(text) ? text : text.replaceAll("<!--[\\s\\S]*?-->", "");
  }

  /**
   * Get a file size from his Uri.
   *
   * @param fileUri The {@link Uri} of the file.
   * @return The size of the file in bytes.
   */
  public static long getFileSizeFromUri(Context context, Uri fileUri) {
    long fileSize = -1;

    if (fileUri != null) {
      if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(fileUri.getScheme())) {
        fileSize = new File(fileUri.getPath()).length();
      } else {
        Cursor returnCursor = context.getContentResolver().query(fileUri,
            new String[]{OpenableColumns.SIZE}, null, null, null);

        if (returnCursor != null) {
          if (returnCursor.moveToFirst()) {
            int index = returnCursor.getColumnIndex(OpenableColumns.SIZE);
            if (index >= 0) {
              fileSize = returnCursor.getLong(index);
            }
          }

          returnCursor.close();
        }
      }
    }

    return fileSize;
  }

  /**
   * Get a file name from his Uri.
   *
   * @param fileUri The {@link Uri} of the file.
   * @return The file name.
   */
  public static String getFileNameFromUri(Context context, Uri fileUri) {
    String fileName = null;

    if (fileUri != null) {
      if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(fileUri.getScheme())) {
        fileName = new File(fileUri.getPath()).getName();
      } else {
        Cursor returnCursor = context.getContentResolver().query(fileUri,
            new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);

        if (returnCursor != null) {
          if (returnCursor.moveToFirst()) {
            int index = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (index >= 0) {
              fileName = returnCursor.getString(index);
            }
          }

          returnCursor.close();
        }
      }
    }

    return fileName;
  }

  /**
   * Write to the file some data by his Uri and return him as {@link String}.
   *
   * @param context Interface to global information about an application environment.
   * @param data    The data which will be written.
   * @param uri     The {@link Uri} of the file.
   * @return the number of bytes copied, or -1 if &gt; Integer.MAX_VALUE
   * @throws IOException if an I/O error occurs
   */
  public static int writeFileFromStringToUri(Context context, Uri uri, String data)
      throws IOException {
    return IOUtils.copy(
        IOUtils.toInputStream(data, StandardCharsets.UTF_8),
        context.getContentResolver().openOutputStream(uri));
  }

  /**
   * Generate an unique extra key using the application id and the class name.
   *
   * @param key The key of the new extra key.
   * @param c   The class where a new extra key will be created.
   * @return The new extra key.
   */
  public static String generateUniqueExtraKey(String key, Class c) {
    return BuildConfig.APPLICATION_ID + "." + c.getSimpleName() + "." + key;
  }

  /**
   * Insert arbitrary string at regular interval into another string
   *
   * @param template       String template which will be inserted to the original string.
   * @param originalString The original string which will be formatted.
   * @param groupSize      Group size
   * @return The formatted string.
   */
  public static String doSectionsInText(String template, String originalString, int groupSize) {

    if (template == null
        || originalString == null
        || groupSize <= 0
        || originalString.length() <= groupSize) {
      return originalString;
    }

    StringBuilder stringBuilder = new StringBuilder(originalString);

    for (int i = stringBuilder.length(); i > 0; i -= groupSize) {
      stringBuilder.insert(i, template);
    }
    return stringBuilder.toString();
  }

  /**
   * Check is current email valid.
   *
   * @param email The current email.
   * @return true if the email has valid format, otherwise false.
   */
  public static boolean isEmailValid(CharSequence email) {
    return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
  }

  /**
   * Get a mime type of the input {@link Uri}
   *
   * @param context Interface to global information about an application environment.
   * @param uri     The {@link Uri} of the file.
   * @return A mime type of of the {@link Uri}.
   */
  public static String getFileMimeTypeFromUri(Context context, Uri uri) {
    if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme())) {
      ContentResolver contentResolver = context.getContentResolver();
      return contentResolver.getType(uri);
    } else {
      String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
      return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
    }
  }

  /**
   * Generate a unique name for {@link android.support.test.espresso.IdlingResource}
   *
   * @param aClass The class where we will use {@link android.support.test.espresso.IdlingResource}
   * @return A generated name.
   */
  public static String generateNameForIdlingResources(Class<?> aClass) {
    return aClass.getClass() + "-" + UUID.randomUUID();
  }

  /**
   * Clear the {@link ClipData}
   *
   * @param context Interface to global information about an application environment.
   */
  public static void clearClipboard(Context context) {
    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    if (clipboard != null) {
      clipboard.setPrimaryClip(ClipData.newPlainText(null, ""));
    }
  }

  /**
   * Check is the app foregrounded or visible.
   *
   * @return true if the app is foregrounded or visible.
   */
  public static boolean isAppForegrounded() {
    ActivityManager.RunningAppProcessInfo appProcessInfo = new ActivityManager.RunningAppProcessInfo();
    ActivityManager.getMyMemoryState(appProcessInfo);
    return (appProcessInfo.importance == IMPORTANCE_FOREGROUND || appProcessInfo.importance == IMPORTANCE_VISIBLE);
  }
}
