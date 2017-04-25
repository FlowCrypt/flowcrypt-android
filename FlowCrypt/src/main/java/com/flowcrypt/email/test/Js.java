/* Business Source License 1.0 © 2016 Tom James Holub (tom@cryptup.org). Use limitations apply.
This version will change to GPLv3 on 2021-01-01. See https://github
.com/tomholub/cryptup-chrome/tree/master/src/LICENCE */

package com.flowcrypt.email.test;

import android.content.Context;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8Value;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


public class Js {

    // Create one object per thread and use them separately. Not thread-safe.

    private Context context;
    private V8 v8;
    private V8Object tool;
    private V8Function cb_catcher;
    private Object cb_last_value;

    public Js(Context context) throws IOException {
        this.v8 = V8.createV8Runtime();
        this.v8.executeScript(read(context.getAssets().open("js/window.js")));
        this.v8.executeScript(read(context.getAssets().open("js/openpgp.js")));
        this.v8.executeScript(read(context.getAssets().open("js/global.js")));
        this.v8.executeScript(read(context.getAssets().open("js/tool.js")));
        this.tool = this.v8.getObject("window").getObject("tool");
        cb_catcher = new V8Function(this.v8, new JavaCallback() {
            @Override
            public Object invoke(V8Object receiver, V8Array parameters) {
                cb_last_value = parameters.get(0);
                if (parameters.get(0) instanceof Releasable) {
                    ((Releasable) parameters.get(0)).release();
                }
                return null;
            }
        });
    }

    public static String read(File file) throws IOException {
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    public static String read(InputStream inputStream) throws IOException {
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }

    public Boolean str_is_email_valid(String email) {
        return (Boolean) this.call(Boolean.class, new String[]{"str", "is_email_valid"}, new
                V8Array(v8).push(email));
    }

    public String str_base64url_encode(String str) {
        return (String) this.call(Boolean.class, new String[]{"str", "base64url_encode"}, new
                V8Array(v8).push(str));
    }

    public String str_base64url_decode(String str) {
        return (String) this.call(Boolean.class, new String[]{"str", "base64url_decode"}, new
                V8Array(v8).push(str));
    }

    public String crypto_key_normalize(String armored_key) {
        return (String) this.call(String.class, new String[]{"crypto", "key", "normalize"}, new
                V8Array(v8).push(armored_key));
    }

    public String crypto_message_encrypt(String pubkeys[], String text, Boolean armor) {
        V8Array params = new V8Array(v8).push(this.array(pubkeys)).push(V8Value.NULL)
                .push(V8Value.NULL).push(text).push(V8Value.NULL).push(armor).push(cb_catcher);
        this.call(void.class, new String[]{"crypto", "message", "encrypt"}, params);
        return ((V8Object) cb_last_value).get("data").toString();
    }

    private Object call(Class<?> return_type, String path[], V8Array args) {
        V8Object obj = null;
        for (Integer i = 0; i < path.length - 1; i++) {
            obj = (obj == null) ? this.tool.getObject(path[i]) : obj.getObject(path[i]);
        }
        if (obj == null) {
            obj = this.tool;
        }
        if (return_type == String.class) {
            return obj.executeStringFunction(path[path.length - 1], args);
        } else if (return_type == Boolean.class) {
            return obj.executeBooleanFunction(path[path.length - 1], args);
        } else if (return_type == Integer.class) {
            return obj.executeIntegerFunction(path[path.length - 1], args);
        } else {
            obj.executeVoidFunction(path[path.length - 1], args);
            return null;
        }
    }

    private V8Array array(String arr[]) {
        V8Array v8arr = new V8Array(this.v8);
        for (String v : arr) {
            v8arr.push(v);
        }
        return v8arr;
    }
}
