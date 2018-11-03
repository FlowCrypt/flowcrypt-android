/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.provider;

import android.net.Uri;

import com.flowcrypt.email.BuildConfig;

/**
 * The contract between the Flowcrypt provider and an application. Contains definitions for the
 * supported URIs and data columns.
 *
 * @author Denis Bondarenko
 * Date: 13.05.2017
 * Time: 11:53
 * E-mail: DenBond7@gmail.com
 */
public class FlowcryptContract {
  public static final String AUTHORITY = BuildConfig.APPLICATION_ID + "." +
      SecurityContentProvider.class.getSimpleName();

  public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

  public static final String CLEAN_DATABASE = "/clean";
  public static final String ERASE_DATABASE = "/erase";
}
