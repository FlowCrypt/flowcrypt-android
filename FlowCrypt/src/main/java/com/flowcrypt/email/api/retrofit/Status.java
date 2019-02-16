/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit;

/**
 * This class describes all available states for API calls which are making by {@link retrofit2.Retrofit}
 *
 * @author Denis Bondarenko
 * Date: 2/15/19
 * Time: 2:54 PM
 * E-mail: DenBond7@gmail.com
 */
public enum Status {
  SUCCESS,
  ERROR,
  EXCEPTION,
  LOADING
}
