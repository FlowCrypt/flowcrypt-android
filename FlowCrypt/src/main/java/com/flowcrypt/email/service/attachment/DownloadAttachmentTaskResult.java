/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.attachment;

import android.net.Uri;

import com.flowcrypt.email.api.email.model.AttachmentInfo;

/**
 * This class defines the download attachment task result.
 *
 * @author Denis Bondarenko
 * Date: 16.08.2017
 * Time: 16:22
 * E-mail: DenBond7@gmail.com
 */

public class DownloadAttachmentTaskResult {
  private int startId;
  private AttachmentInfo attachmentInfo;
  private Exception exception;
  private Uri uri;
  private int progressInPercentage;
  private long timeLeft;

  public DownloadAttachmentTaskResult(int startId, AttachmentInfo attachmentInfo, Exception exception, Uri uri,
                                      int progressInPercentage, long timeLeft) {
    this.startId = startId;
    this.attachmentInfo = attachmentInfo;
    this.exception = exception;
    this.uri = uri;
    this.progressInPercentage = progressInPercentage;
    this.timeLeft = timeLeft;
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

  public Uri getUri() {
    return uri;
  }

  public int getProgressInPercentage() {
    return progressInPercentage;
  }

  public long getTimeLeft() {
    return timeLeft;
  }

  public static class Builder {
    private int startId;
    private AttachmentInfo attachmentInfo;
    private Exception exception;
    private Uri uri;
    private int progressInPercentage;
    private long timeLeft;

    public Builder setStartId(int startId) {
      this.startId = startId;
      return this;
    }

    public Builder setAttachmentInfo(AttachmentInfo attachmentInfo) {
      this.attachmentInfo = attachmentInfo;
      return this;
    }

    public Builder setException(Exception exception) {
      this.exception = exception;
      return this;
    }

    public Builder setUri(Uri uri) {
      this.uri = uri;
      return this;
    }

    public Builder setProgressInPercentage(int progressInPercentage) {
      this.progressInPercentage = progressInPercentage;
      return this;
    }

    public Builder setTimeLeft(long timeLeft) {
      this.timeLeft = timeLeft;
      return this;
    }

    public DownloadAttachmentTaskResult build() {
      return new DownloadAttachmentTaskResult(startId, attachmentInfo, exception, uri, progressInPercentage,
          timeLeft);
    }
  }
}
