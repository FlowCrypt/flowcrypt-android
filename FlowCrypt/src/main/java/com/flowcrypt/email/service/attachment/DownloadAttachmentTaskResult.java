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
  private AttachmentInfo attInfo;
  private Exception exception;
  private Uri uri;
  private int progressInPercentage;
  private long timeLeft;
  private boolean isLast;

  public DownloadAttachmentTaskResult(AttachmentInfo attInfo, Exception e, Uri uri, int progressInPercentage, long
      timeLeft,
                                      boolean isLast) {
    this.attInfo = attInfo;
    this.exception = e;
    this.uri = uri;
    this.progressInPercentage = progressInPercentage;
    this.timeLeft = timeLeft;
    this.isLast = isLast;
  }

  public AttachmentInfo getAttachmentInfo() {
    return attInfo;
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

  public boolean isLast() {
    return isLast;
  }

  public static class Builder {
    private AttachmentInfo attInfo;
    private Exception exception;
    private Uri uri;
    private int progressInPercentage;
    private long timeLeft;
    private boolean isLast;

    public Builder setAttachmentInfo(AttachmentInfo attInfo) {
      this.attInfo = attInfo;
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

    public Builder setLast(boolean last) {
      isLast = last;
      return this;
    }

    public DownloadAttachmentTaskResult build() {
      return new DownloadAttachmentTaskResult(attInfo, exception, uri, progressInPercentage, timeLeft, isLast);
    }
  }
}
