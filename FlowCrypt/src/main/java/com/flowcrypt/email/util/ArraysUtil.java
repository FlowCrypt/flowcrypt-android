/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import javax.annotation.Nullable;

/**
 * @author Denis Bondarenko
 * Date: 3/22/19
 * Time: 10:29 AM
 * E-mail: DenBond7@gmail.com
 */
public class ArraysUtil {

  public static <T> boolean isEmpty(@Nullable T[] original) {
    return original == null || original.length == 0;
  }
}
