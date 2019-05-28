/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit

/**
 * List of all API which uses in the RETROFIT.
 *
 * @author Denis Bondarenko
 * Date: 10.03.2015
 * Time: 13:51
 * E-mail: DenBond7@gmail.com
 */
enum class ApiName {
  /*flowcrypt.com/attester*/
  POST_TEST_WELCOME,
  POST_INITIAL_LEGACY_SUBMIT,
  POST_LOOKUP_EMAIL_SINGLE,
  POST_LOOKUP_EMAIL_MULTIPLY,
  GET_LOOKUP,

  /*flowcrypt.com/api*/
  POST_HELP_FEEDBACK,
  POST_LINK_MESSAGE,
  POST_MESSAGE_REPLY
}
