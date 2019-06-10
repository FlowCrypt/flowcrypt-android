/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * @author Denis Bondarenko
 * Date: 24.02.2018
 * Time: 17:29
 * E-mail: DenBond7@gmail.com
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.ANNOTATION_CLASS)
annotation class Repeat(val value: Int = 1)
