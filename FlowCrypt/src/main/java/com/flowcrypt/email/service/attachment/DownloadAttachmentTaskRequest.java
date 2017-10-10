/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.attachment;

import android.content.Context;

import com.flowcrypt.email.api.email.model.AttachmentInfo;

/**
 * This class will be used to define information about a new download attachment task.
 *
 * @author Denis Bondarenko
 *         Date: 16.08.2017
 *         Time: 16:04
 *         E-mail: DenBond7@gmail.com
 */

public class DownloadAttachmentTaskRequest {
    private Context context;
    private int startId;
    private AttachmentInfo attachmentInfo;

    public DownloadAttachmentTaskRequest(Context context, int startId, AttachmentInfo attachmentInfo) {
        this.context = context;
        this.startId = startId;
        this.attachmentInfo = attachmentInfo;
    }

    public Context getContext() {
        return context;
    }

    public int getStartId() {
        return startId;
    }

    public AttachmentInfo getAttachmentInfo() {
        return attachmentInfo;
    }
}
