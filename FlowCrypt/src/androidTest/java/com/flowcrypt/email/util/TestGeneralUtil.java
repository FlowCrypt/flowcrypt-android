/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import android.content.Context;

import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Denis Bondarenko
 *         Date: 18.01.2018
 *         Time: 13:02
 *         E-mail: DenBond7@gmail.com
 */

public class TestGeneralUtil {

    public static <T> T readObjectFromResources(String path, Class<T> aClass) {
        try {
            return new Gson().fromJson(
                    IOUtils.toString(aClass.getClassLoader().getResourceAsStream(path), StandardCharsets.UTF_8),
                    aClass);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String readFileFromAssetsAsString(Context context, String filePath) throws IOException {
        return IOUtils.toString(context.getAssets().open(filePath), "UTF-8");
    }
}
