/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import android.content.Context;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;

public class SampleStorageConnector implements StorageConnectorInterface {

  private Context context;
  private PgpContact[] sampleContacts = new PgpContact[2];
  private PgpKeyInfo[] sampleKeys = new PgpKeyInfo[1];
  private String[] samplePassphrases = new String[1];

  public SampleStorageConnector(Context context) throws IOException {
    this.context = context;
    this.sampleContacts[0] = new PgpContact("cryptup.tester@gmail.com", "CryptUp Tester", read("sample/pub.asc"),
        true, "cryptup", true,
        "8B8A05A25B106262B06217C606CA553EC2455D70", "06CA553EC2455D70", "ALMOST FAMOUS EXILE LOYAL FICTION COME", 0);
    this.sampleContacts[1] = new PgpContact("tom@bitoasis.net", "Tom James Holub", read("sample/pub-tom.asc"), true,
        "cryptup", true,
        "8B8A05A2216EE6E4C5EE3D540D5688EBF3102BE7", "0D5688EBF3102BE7", "ASK REFORM DEPEND TOWER ACTOR DIAGRAM", 0);
    this.sampleKeys[0] = new PgpKeyInfo(read("sample/prv.asc"), "06CA553EC2455D70");
    this.samplePassphrases[0] = read("sample/passphrase.txt");
    System.out.println(this.samplePassphrases[0]);
  }

  public PgpContact findPgpContact(String longid) {
    for (PgpContact c : this.sampleContacts) {
      if (c.getLongid().equals(longid)) {
        return c;
      }
    }
    return null;
  }

  public PgpContact[] findPgpContacts(String[] longid) {
    PgpContact[] results = new PgpContact[longid.length];
    for (Integer i = 0; i < longid.length; i++) {
      results[i] = findPgpContact(longid[i]); // PgpContact or null
    }
    return results;
  }

  public PgpKeyInfo getPgpPrivateKey(String longid) {
    for (PgpKeyInfo k : this.sampleKeys) {
      if (k.getLongid().equals(longid)) {
        return k;
      }
    }
    return null;
  }

  public PgpKeyInfo[] getFilteredPgpPrivateKeys(String[] longid) {
    ArrayList<PgpKeyInfo> list = new ArrayList<PgpKeyInfo>();
    for (String id : longid) {
      for (PgpKeyInfo k : this.sampleKeys) {
        if (k.getLongid().equals(id)) {
          list.add(k);
          break;
        }
      }
    }
    return list.toArray(new PgpKeyInfo[0]);
  }

  public PgpKeyInfo[] getAllPgpPrivateKeys() {
    return this.sampleKeys;
  }

  public String getPassphrase(String longid) {
    for (Integer i = 0; i < this.sampleKeys.length; i++) {
      if (this.sampleKeys[i].getLongid().equals(longid)) {
        return this.samplePassphrases[i]; // get corresponding pass phrase
      }
    }
    return null;
  }

  @Override
  public void refresh(Context context) {

  }

  private String read(String filePath) throws IOException {
    return IOUtils.toString(context.getAssets().open(filePath), "UTF-8");
  }
}
