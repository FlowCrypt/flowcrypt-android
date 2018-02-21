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
 *         Date: 27.12.2017
 *         Time: 14:49
 *         E-mail: DenBond7@gmail.com
 */

public class AuthCredentialsManager {
    public static AuthCredentials getOutLookWithBackupAuthCredentials() {
        return readAuthCredentialsFromResources("outlook.json");
    }

    public static AuthCredentials getLocalWithOneBackupAuthCredentials() {
        return readAuthCredentialsFromResources("user_with_one_backup.json");
    }

    public static AuthCredentials getDefaultWithBackupAuthCredentials() {
        return readAuthCredentialsFromResources("default.json");
    }

    private static AuthCredentials readAuthCredentialsFromResources(String path) {
        try {
            return new Gson().fromJson(
                    IOUtils.toString(AuthCredentialsManager.class.getClassLoader().getResourceAsStream(path),
                            StandardCharsets.UTF_8), AuthCredentials.class);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return new AuthCredentials();
    }
}
