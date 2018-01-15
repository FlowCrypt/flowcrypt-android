/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit;

/**
 * List of all API which uses in the RETROFIT.
 *
 * @author Denis Bondarenko
 *         Date: 10.03.2015
 *         Time: 13:51
 *         E-mail: DenBond7@gmail.com
 */
public enum ApiName {
    /*attester.flowcrypt.com*/
    POST_TEST_WELCOME,
    POST_INITIAL_REQUEST,
    POST_INITIAL_CONFIRM,
    POST_INITIAL_LEGACY_SUBMIT,
    POST_LOOKUP_EMAIL_SINGLE,
    POST_LOOKUP_EMAIL_MULTIPLY,
    POST_REPLACE_REQUEST,
    POST_REPLACE_CONFIRM,

    /*api.cryptup.io*/
    POST_HELP_FEEDBACK,
    POST_LINK_MESSAGE,
    POST_MESSAGE_REPLY
}
