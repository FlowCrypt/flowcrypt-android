package com.flowcrypt.email.test;

import com.eclipsesource.v8.V8Object;

public class MimeAddress extends MeaningfulV8ObjectContainer {

    public MimeAddress(V8Object o) {
        super(o);
    }

    public String getAddress() {
        return this.getAttributeAsString("address");
    }

    public String getName() {
        return this.getAttributeAsString("name");
    }

}
