/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
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
//todo-denbond7 need to remove this class after refactoring
enum class ApiName {
  /*flowcrypt.com/attester*/
  GET_PUB,

  /*flowcrypt.com/api*/
  POST_HELP_FEEDBACK,
  POST_LINK_MESSAGE,
  POST_MESSAGE_REPLY,
  POST_LOGIN,
  POST_GET_DOMAIN_RULES
}
