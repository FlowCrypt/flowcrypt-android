/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import android.content.Context;

import java.util.ArrayList;

/**
 * This implementation of {@link StorageConnectorInterface} can be configured using dynamic info.
 *
 * @author Denis Bondarenko
 * Date: 14.12.2017
 * Time: 16:23
 * E-mail: DenBond7@gmail.com
 */

public class DynamicStorageConnector implements StorageConnectorInterface {

  private PgpContact[] pgpContacts;
  private PgpKeyInfo[] pgpKeyInfos;
  private String[] passphrases;

  public DynamicStorageConnector(PgpContact[] pgpContacts, PgpKeyInfo[] pgpKeyInfos, String[] passphrases) {
    this.pgpContacts = pgpContacts;
    this.pgpKeyInfos = pgpKeyInfos;
    this.passphrases = passphrases;
  }


  public PgpContact findPgpContact(String longid) {
    for (PgpContact c : this.pgpContacts) {
      if (c.getLongid().equals(longid)) {
        return c;
      }
    }
    return null;
  }

  public PgpContact[] findPgpContacts(String[] longid) {
    PgpContact[] results = new PgpContact[longid.length];
    for (Integer i = 0; i < longid.length; i++) {
      results[i] = findPgpContact(longid[i]);
    }
    return results;
  }

  public PgpKeyInfo getPgpPrivateKey(String longid) {
    for (PgpKeyInfo k : this.pgpKeyInfos) {
      if (k.getLongid().equals(longid)) {
        return k;
      }
    }
    return null;
  }

  public PgpKeyInfo[] getFilteredPgpPrivateKeys(String[] longid) {
    ArrayList<PgpKeyInfo> list = new ArrayList<>();
    for (String id : longid) {
      for (PgpKeyInfo k : this.pgpKeyInfos) {
        if (k.getLongid().equals(id)) {
          list.add(k);
          break;
        }
      }
    }
    return list.toArray(new PgpKeyInfo[0]);
  }

  public PgpKeyInfo[] getAllPgpPrivateKeys() {
    return this.pgpKeyInfos;
  }

  public String getPassphrase(String longid) {
    for (Integer i = 0; i < this.pgpKeyInfos.length; i++) {
      if (this.pgpKeyInfos[i].getLongid().equals(longid)) {
        return this.passphrases[i];
      }
    }
    return null;
  }

  @Override
  public void refresh(Context context) {

  }
}
