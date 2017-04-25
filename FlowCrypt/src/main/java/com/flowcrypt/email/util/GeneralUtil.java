package com.flowcrypt.email.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

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
}
