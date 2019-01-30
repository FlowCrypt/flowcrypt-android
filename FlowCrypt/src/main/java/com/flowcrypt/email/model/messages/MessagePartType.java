/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model.messages;

import com.flowcrypt.email.js.MessageBlock;

/**
 * The {@link MessageBlock} types. Can be one of:
 * <p>
 * <ul>
 * <li>{@link MessageBlock#TYPE_TEXT}</li>
 * <li>{@link MessageBlock#TYPE_PGP_MESSAGE}</li>
 * <li>{@link MessageBlock#TYPE_PGP_PUBLIC_KEY}</li>
 * <li>{@link MessageBlock#TYPE_PGP_SIGNED_MESSAGE}</li>
 * <li>{@link MessageBlock#TYPE_PGP_PASSWORD_MESSAGE}</li>
 * <li>{@link MessageBlock#TYPE_ATTEST_PACKET}</li>
 * <li>{@link MessageBlock#TYPE_VERIFICATION}</li>
 * </ul>
 *
 * @author Denis Bondarenko
 * Date: 18.07.2017
 * Time: 17:47
 * E-mail: DenBond7@gmail.com
 */

public enum MessagePartType {
  TEXT,
  PGP_MESSAGE,
  PGP_PUBLIC_KEY,
  PGP_SIGNED_MESSAGE,
  PGP_PASSWORD_MESSAGE,
  ATTEST_PACKET,
  VERIFICATION
}
