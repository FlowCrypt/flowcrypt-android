/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * This class describes a logic of generation {@link AuthCredentials} from the resources folder.
 *
 * @author Denis Bondarenko
 * Date: 27.12.2017
 * Time: 14:49
 * E-mail: DenBond7@gmail.com
 */

public class AuthCredentialsManager {
  public static AuthCredentials getOutLookWithBackupAuthCreds() {
    return readAuthCredsFromResources("outlook.json");
  }

  public static AuthCredentials getLocalWithOneBackupAuthCreds() {
    return readAuthCredsFromResources("user_with_one_backup.json");
  }

  public static AuthCredentials getDefaultWithBackupAuthCreds() {
    return readAuthCredsFromResources("default.json");
  }

  private static AuthCredentials readAuthCredsFromResources(String path) {
    try {
      return new Gson().fromJson(IOUtils.toString(AuthCredentialsManager.class.getClassLoader().getResourceAsStream
          (path), StandardCharsets.UTF_8), AuthCredentials.class);

    } catch (IOException e) {
      e.printStackTrace();
    }

    return new AuthCredentials();
  }
}
