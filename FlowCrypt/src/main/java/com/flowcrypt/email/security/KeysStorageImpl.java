/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security;

import android.content.Context;

import com.flowcrypt.email.model.KeysStorage;
import com.flowcrypt.email.model.PgpContact;
import com.flowcrypt.email.model.PgpKeyInfo;
import com.flowcrypt.email.security.model.PrivateKeyInfo;
import com.flowcrypt.email.util.exception.ExceptionUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * This class implements {@link KeysStorage}. Here we collect information about imported private keys.
 *
 * @author DenBond7
 * Date: 05.05.2017
 * Time: 13:06
 * E-mail: DenBond7@gmail.com
 */

public final class KeysStorageImpl implements KeysStorage {

  private static volatile KeysStorageImpl ourInstance;

  private LinkedList<PgpKeyInfo> pgpKeyInfoList;
  private LinkedList<String> passphrases;
  private List<OnRefreshListener> onRefreshListeners;

  private KeysStorageImpl(Context context) {
    this.onRefreshListeners = new ArrayList<>();
    setup(context);
  }

  public static void init(Context context) {
    KeysStorageImpl.getInstance(context);
  }

  public static KeysStorageImpl getInstance(Context context) {
    if (ourInstance == null) {
      synchronized (KeysStorageImpl.class) {
        if (ourInstance == null) {
          ourInstance = new KeysStorageImpl(context);
        }
      }
    }
    return ourInstance;
  }

  @Override
  public PgpContact findPgpContact(String longid) {
    return null;
  }

  @Override
  public List<PgpContact> findPgpContacts(String[] longid) {
    return Collections.emptyList();
  }

  @Override
  public PgpKeyInfo getPgpPrivateKey(String longid) {
    for (PgpKeyInfo pgpKeyInfo : pgpKeyInfoList) {
      if (longid.equals(pgpKeyInfo.getLongid())) {
        return pgpKeyInfo;
      }
    }
    return null;
  }

  @Override
  public List<PgpKeyInfo> getFilteredPgpPrivateKeys(String[] longId) {
    List<PgpKeyInfo> pgpKeyInfos = new ArrayList<>();
    for (String id : longId) {
      for (PgpKeyInfo pgpKeyInfo : this.pgpKeyInfoList) {
        if (pgpKeyInfo.getLongid().equals(id)) {
          pgpKeyInfos.add(pgpKeyInfo);
          break;
        }
      }
    }
    return pgpKeyInfos;
  }

  @Override
  public List<PgpKeyInfo> getAllPgpPrivateKeys() {
    return pgpKeyInfoList;
  }

  @Override
  public String getPassphrase(String longid) {
    for (int i = 0; i < pgpKeyInfoList.size(); i++) {
      PgpKeyInfo pgpKeyInfo = pgpKeyInfoList.get(i);
      if (longid.equals(pgpKeyInfo.getLongid())) {
        return passphrases.get(i);
      }
    }

    return null;
  }

  @Override
  public synchronized void refresh(Context context) {
    setup(context);

    for (OnRefreshListener onRefreshListener : onRefreshListeners) {
      onRefreshListener.onRefresh();
    }
  }

  public void attachOnRefreshListener(OnRefreshListener onRefreshListener) {
    if (onRefreshListener != null) {
      this.onRefreshListeners.add(onRefreshListener);
    }
  }

  public void removeOnRefreshListener(OnRefreshListener onRefreshListener) {
    if (onRefreshListener != null) {
      this.onRefreshListeners.remove(onRefreshListener);
    }
  }

  private void setup(Context context) {
    if (context == null) {
      return;
    }

    Context appContext = context.getApplicationContext();

    this.pgpKeyInfoList = new LinkedList<>();
    this.passphrases = new LinkedList<>();
    try {
      List<PrivateKeyInfo> privateKeysInfo = SecurityUtils.getPrivateKeysInfo(appContext);
      for (PrivateKeyInfo privateKeyInfo : privateKeysInfo) {
        pgpKeyInfoList.add(privateKeyInfo.getPgpKeyInfo());
        passphrases.add(privateKeyInfo.getPassphrase());
      }
    } catch (Exception e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  public interface OnRefreshListener {
    void onRefresh();
  }
}
