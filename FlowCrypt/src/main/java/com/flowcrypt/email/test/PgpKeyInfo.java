package com.flowcrypt.email.test;

public class PgpKeyInfo {

    private final String longid;
    private final String armored;

    PgpKeyInfo(String armored, String longid) {
        this.armored = armored;
        this.longid = longid;
    }

    public String getLongid() {
        return longid;
    }

    public String getArmored() {
        return armored;
    }

}
