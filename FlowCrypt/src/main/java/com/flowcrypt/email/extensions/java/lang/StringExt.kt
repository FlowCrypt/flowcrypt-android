/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.java.lang

import java.util.Locale

/**
 * @author Denis Bondarenko
 *         Date: 9/6/21
 *         Time: 12:30 PM
 *         E-mail: DenBond7@gmail.com
 */
fun String.lowercase(): String = toLowerCase(Locale.ROOT)
fun String.uppercase(): String = toUpperCase(Locale.ROOT)
