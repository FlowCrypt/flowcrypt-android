package com.flowcrypt.email.test;

public class PgpContact {

    private String email;
    private String name;
    private String pubkey;
    private Boolean has_pgp;
    private String client;
    private Boolean attested;
    private String fingerprint;
    private String longid;
    private String keywords;
    private Integer last_use;

    public PgpContact(String email, String name, String pubkey, Boolean has_pgp, String client, Boolean attested, String fingerprint, String longid, String keywords, Integer last_use) {
        this.email = email;
        this.name = name;
        this.pubkey = pubkey;
        this.has_pgp = has_pgp;
        this.client = client;
        this.attested = attested;
        this.fingerprint = fingerprint;
        this.longid = longid;
        this.keywords = keywords;
        this.last_use = last_use;
    }

    public PgpContact(Js js, String email, String name, String pubkey, String client, Boolean attested) {
        this.email = email;
        this.name = name;
        this.pubkey = pubkey;
        this.has_pgp = (pubkey != null);
        this.client = client;
        this.attested = attested;
        this.fingerprint = js.crypto_key_fingerprint(js.crypto_key_read(pubkey));
        this.longid = js.crypto_key_longid(this.fingerprint);
        this.keywords = js.mnemonic(this.longid);
        this.last_use = 0;
    }

    public PgpContact(String email, String name) {
        this.email = email;
        this.name = name;
        this.pubkey = null;
        this.has_pgp = false;
        this.client = null;
        this.attested = false;
        this.fingerprint = null;
        this.longid = null;
        this.keywords = null;
        this.last_use = 0;
    }

    public String getEmail() {
        return this.email;
    }

    public String getName() {
        return name;
    }

    public String getPubkey() {
        return pubkey;
    }

    public Boolean getHasPgp() {
        return has_pgp;
    }

    public String getClient() {
        return client;
    }

    public Boolean getAttested() {
        return attested;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getLongid() {
        return longid;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public Integer getLastUse() {
        return last_use;
    }

    public String getMime() {
        return MimeAddress.stringify(name, email);
    }

    public static String arrayAsMime(PgpContact[] contacts) {
        String stringified = "";
        for(Integer i = 0; i < contacts.length; i++) {
            stringified += contacts[i].getMime() + (i < contacts.length - 1 ? "," : "");
        }
        return stringified;
    }
}
