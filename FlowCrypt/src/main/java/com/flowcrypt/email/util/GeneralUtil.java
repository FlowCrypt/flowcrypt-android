/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.Settings;

import com.flowcrypt.email.BuildConfig;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * General util methods.
 *
 * @author DenBond7
 *         Date: 31.03.2017
 *         Time: 16:55
 *         E-mail: DenBond7@gmail.com
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
}
