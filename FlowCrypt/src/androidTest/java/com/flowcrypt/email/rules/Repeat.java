/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;

/**
 * @author Denis Bondarenko
 * Date: 24.02.2018
 * Time: 17:29
 * E-mail: DenBond7@gmail.com
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({METHOD, ANNOTATION_TYPE})
public @interface Repeat {
  int value() default 1;
}
