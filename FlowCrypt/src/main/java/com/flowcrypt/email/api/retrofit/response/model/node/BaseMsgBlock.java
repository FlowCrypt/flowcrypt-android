/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node;

import android.os.Parcel;

/**
 * It's a base {@link MsgBlock}
 *
 * @author Denis Bondarenko
 * Date: 3/26/19
 * Time: 9:46 AM
 * E-mail: DenBond7@gmail.com
 */
public class BaseMsgBlock extends MsgBlock {
  public BaseMsgBlock(Parcel in, Type type) {
    super(in, type);
  }
}
