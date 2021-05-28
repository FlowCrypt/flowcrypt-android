/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model.results

/**
 * This class used by Android loaders to create response result with an exception, if it happened.
 *
 * @author DenBond7
 * Date: 09.05.2017
 * Time: 16:02
 * E-mail: DenBond7@gmail.com
 */
//todo-denbond7 need to remove this class in the near future. Refactor code and remove this
data class LoaderResult constructor(
  /**
   * Some result which will be returned by loader.
   */
  var result: Any? = null,
  /**
   * Some exception which will be returned if it happened.
   */
  var exception: Exception? = null
)
