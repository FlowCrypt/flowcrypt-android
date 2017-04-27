package com.flowcrypt.email.test;

import com.eclipsesource.v8.V8Object;

public class Key extends MeaningfulV8ObjectContainer {

    public Key(V8Object o) {
        super(o);
    }

    public Boolean isPrivate() {
        return this.v8object.executeBooleanFunction("isPrivate", null);
    }

    public String armor() {
        return this.v8object.executeStringFunction("armor", null);
    }
}
