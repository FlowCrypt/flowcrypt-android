/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import android.os.Environment;

import com.flowcrypt.email.BuildConfig;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The debug log writer which will be used to write logs to the file.
 *
 * @author Denis Bondarenko
 *         Date: 10.07.2017
 *         Time: 16:10
 *         E-mail: DenBond7@gmail.com
 */
public class DebugLogWriter {
    private static final long MAX_FILE_SIZE = FileUtils.ONE_MB;
    private static final String LOG_MESSAGE_PATTERN = "%-20s    %s";
    private static final String YYYY_MM_DD_HH_MM_SS_S = "yyyy-MM-dd HH:mm:ss.S";

    private File fileLog;
    private DateFormat dateFormat;

    public DebugLogWriter(String fileName) {
        this(new File(Environment.getExternalStorageDirectory() + "/" + BuildConfig
                .APPLICATION_ID + "_" + fileName + "" +
                ".log"));
    }

    public DebugLogWriter(File fileLog) {
        this.fileLog = fileLog;

        if (fileLog.length() >= MAX_FILE_SIZE) {
            try {
                FileUtils.writeStringToFile(fileLog, "", Charset.defaultCharset(), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        dateFormat = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS_S, Locale.US);
    }

    public void writeLog(String message) {
        try {
            FileUtils.writeStringToFile(fileLog, String.format(LOG_MESSAGE_PATTERN,
                    dateFormat.format(new Date()), ""), Charset.defaultCharset(), true);
            FileUtils.writeStringToFile(fileLog, message, Charset.defaultCharset(), true);
            FileUtils.writeStringToFile(fileLog, "\n", Charset.defaultCharset(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void resetLogs() {
        if (fileLog != null) {
            try {
                FileUtils.writeStringToFile(fileLog, "", Charset.defaultCharset(), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
