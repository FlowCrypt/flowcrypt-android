/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.attachment;

import android.content.Context;

import com.flowcrypt.email.api.email.model.AttachmentInfo;

/**
 * This class will be used to define information about a new download attachment task.
 *
 * @author Denis Bondarenko
 * Date: 16.08.2017
 * Time: 16:04
 * E-mail: DenBond7@gmail.com
 */

public class DownloadAttachmentTaskRequest {
  private Context context;
  private AttachmentInfo att;

  public DownloadAttachmentTaskRequest(Context context, AttachmentInfo attInfo) {
    this.context = context != null ? context.getApplicationContext() : null;
    this.att = attInfo;
  }

  public Context getContext() {
    return context;
  }

  public AttachmentInfo getAttInfo() {
    return att;
  }
}
