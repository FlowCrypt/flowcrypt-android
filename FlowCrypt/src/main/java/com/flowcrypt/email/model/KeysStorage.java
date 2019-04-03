/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model;

import android.content.Context;

import java.util.List;

import androidx.annotation.Keep;

@Keep
public interface KeysStorage {

  PgpContact findPgpContact(String longid);

  // if two contacts requested and only one found, will still return list of 2:
  // eg [PgpContact, null] or [null, PgpContact] depending which one is missing
  List findPgpContacts(String[] longid);

  PgpKeyInfo getPgpPrivateKey(String longid);

  // if 2 keys requested and only one found, will return list of 1: [PgpKey]
  List<PgpKeyInfo> getFilteredPgpPrivateKeys(String[] longid);

  List<PgpKeyInfo> getAllPgpPrivateKeys();

  String getPassphrase(String longid);

  void refresh(Context context);

}


