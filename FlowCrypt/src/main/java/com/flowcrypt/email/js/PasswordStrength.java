/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import com.eclipsesource.v8.V8Object;

public class PasswordStrength extends MeaningfulV8ObjectContainer {

    private String word;
    private int bar;
    private String time;
    private int seconds;
    private boolean pass;
    private String color;

    PasswordStrength(V8Object o) {
        super(o);
    }

    public String getWord() {
        return getAttributeAsString("word");
    }

    public int getBar() {
        return getAttributeAsInteger("bar");
    }


    public String getTime() {
        return getAttributeAsString("time");
    }


    public int getSeconds() {
        return getAttributeAsInteger("seconds");
    }


    public boolean didPass() {
        return getAttributeAsBoolean("pass");
    }


    public String getColor() {
        return getAttributeAsString("color");
    }
}
