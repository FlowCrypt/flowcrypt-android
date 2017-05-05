package com.flowcrypt.email.security;

import android.content.Context;
import android.text.TextUtils;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.test.Js;
import com.flowcrypt.email.test.PgpContact;
import com.flowcrypt.email.test.PgpKeyInfo;
import com.flowcrypt.email.test.StorageConnectorInterface;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This class implemented StorageConnectorInterface. We collect an information about available
 * private keys.
 *
 * @author DenBond7
 *         Date: 05.05.2017
 *         Time: 13:06
 *         E-mail: DenBond7@gmail.com
 */

public class SecurityStorageConnector implements StorageConnectorInterface {

    private LinkedList<PgpKeyInfo> pgpKeyInfoList;
    private LinkedList<String> passphraseList;

    public SecurityStorageConnector(Context context) {
        this.pgpKeyInfoList = new LinkedList<>();
        this.passphraseList = new LinkedList<>();

        try {
            Js js = new Js(context, null);
            File[] files = SecurityUtils.getCorrectPrivateKeys(context);
            for (File file : files) {
                try {
                    String armoredKey = FileUtils.readFileToString(file,
                            StandardCharsets.UTF_8);
                    pgpKeyInfoList.add(new PgpKeyInfo(armoredKey, js.crypto_key_longid
                            (armoredKey)));
                    passphraseList.add(parsePassphraseFromFileName(file.getName()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PgpContact findPgpContact(String longid) {
        return null;
    }

    @Override
    public PgpContact[] findPgpContacts(String[] longid) {
        return new PgpContact[0];
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
    public PgpKeyInfo[] getFilteredPgpPrivateKeys(String[] longId) {
        List<PgpKeyInfo> pgpKeyInfos = new ArrayList<>();
        for (String id : longId) {
            for (PgpKeyInfo pgpKeyInfo : this.pgpKeyInfoList) {
                if (pgpKeyInfo.getLongid().equals(id)) {
                    pgpKeyInfos.add(pgpKeyInfo);
                    break;
                }
            }
        }
        return pgpKeyInfos.toArray(new PgpKeyInfo[0]);
    }

    @Override
    public PgpKeyInfo[] getAllPgpPrivateKeys() {
        return pgpKeyInfoList.toArray(new PgpKeyInfo[0]);
    }

    @Override
    public String getPassphrase(String longid) {
        for (int i = 0; i < pgpKeyInfoList.size(); i++) {
            PgpKeyInfo pgpKeyInfo = pgpKeyInfoList.get(i);
            if (longid.equals(pgpKeyInfo.getLongid())) {
                return passphraseList.get(i);
            }
        }

        return null;
    }

    /**
     * Decrypt a message if it decrypted;
     *
     * @param name The name of a private key file.
     * @return <tt>String</tt> Return a parse passphrase.
     */
    private String parsePassphraseFromFileName(String name) {
        return !TextUtils.isEmpty(name) ? name.replace(Constants.PREFIX_PRIVATE_KEY, "") : "";
    }
}
