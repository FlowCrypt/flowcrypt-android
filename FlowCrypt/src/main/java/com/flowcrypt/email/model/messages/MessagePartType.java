/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model.messages;


import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock;

/**
 * The {@link MsgBlock} types.
 *
 * @author Denis Bondarenko
 * Date: 18.07.2017
 * Time: 17:47
 * E-mail: DenBond7@gmail.com
 */

public enum MessagePartType {
  HTML,
  TEXT,
  PGP_MESSAGE,
  PGP_PUBLIC_KEY,
  PGP_SIGNED_MESSAGE,
  PGP_PASSWORD_MESSAGE,
  ATTEST_PACKET,
  VERIFICATION
}
