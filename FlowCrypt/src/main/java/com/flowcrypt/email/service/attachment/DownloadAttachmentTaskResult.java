/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.attachment;

import com.flowcrypt.email.api.email.model.AttachmentInfo;

/**
 * This class defines the download attachment task result.
 *
 * @author Denis Bondarenko
 *         Date: 16.08.2017
 *         Time: 16:22
 *         E-mail: DenBond7@gmail.com
 */

public class DownloadAttachmentTaskResult {
    private int startId;
    private AttachmentInfo attachmentInfo;
    private Exception exception;

    public DownloadAttachmentTaskResult(int startId, AttachmentInfo attachmentInfo, Exception exception) {
        this.startId = startId;
        this.attachmentInfo = attachmentInfo;
        this.exception = exception;
    }

    public int getStartId() {
        return startId;
    }

    public AttachmentInfo getAttachmentInfo() {
        return attachmentInfo;
    }

    public Exception getException() {
        return exception;
    }
}
