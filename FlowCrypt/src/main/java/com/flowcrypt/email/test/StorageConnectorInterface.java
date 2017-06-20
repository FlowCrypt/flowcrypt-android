/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: Tom James Holub
 */

package com.flowcrypt.email.test;

public interface StorageConnectorInterface {

    PgpContact findPgpContact(String longid);

    // if two contacts requested and only one found, will still return array of 2:
    // eg [PgpContact, null] or [null, PgpContact] depending which one is missing
    PgpContact[] findPgpContacts(String longid[]);

    PgpKeyInfo getPgpPrivateKey(String longid);

    // if 2 keys requested and only one found, will return array of 1: [PgpKey]
    PgpKeyInfo[] getFilteredPgpPrivateKeys(String longid[]);

    PgpKeyInfo[] getAllPgpPrivateKeys();

    String getPassphrase(String longid);

}


