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
import com.eclipsesource.v8.V8ResultUndefined;
import com.eclipsesource.v8.V8Value;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class Js {

    // Create one object per thread and use them separately. Not thread-safe.

    private Context context;
    private V8 v8;
    private V8Object tool;
    private V8Function cb_catcher;
    private Object[] cb_last_value = new Object[3];

    public Js(Context context) throws IOException {
        v8 = V8.createV8Runtime();

        // forward console.log and console.error to System.out.println
        JavascriptConsoleForwarder console = new JavascriptConsoleForwarder();
        V8Object v8Console = new V8Object(v8);
        v8.add("console", v8Console);
        v8Console.registerJavaMethod(console, "log", "log", new Class[]{String.class});
        v8Console.registerJavaMethod(console, "error", "error", new Class[]{String.class});
        v8Console.release();

        v8.executeScript(read(context.getAssets().open("js/window.js")));
        v8.executeScript(read(context.getAssets().open("js/openpgp.js")));
        v8.executeScript(read(context.getAssets().open("js/emailjs/emailjs-stringencoding.js")));
        v8.executeScript(read(context.getAssets().open("js/emailjs/emailjs-addressparser.js")));
        v8.executeScript(read(context.getAssets().open("js/emailjs/emailjs-mime-codec.js")));
        v8.executeScript(read(context.getAssets().open("js/emailjs/emailjs-mime-parser.js")));
        v8.executeScript(read(context.getAssets().open("js/global.js")));
        v8.executeScript(read(context.getAssets().open("js/tool.js")));
        tool = v8.getObject("window").getObject("tool");
        cb_catcher = new V8Function(v8, new JavaCallback() {
            @Override
            public Object invoke(V8Object receiver, V8Array parameters) {
                Arrays.fill(cb_last_value, null);
                for(Integer i = 0; i < parameters.length(); i++) {
                    cb_last_value[i] = parameters.get(i);
                    if (parameters.get(i) instanceof Releasable) {
                        ((Releasable) parameters.get(i)).release();
                    }
                }
                return null;
            }
        });
    }

    public Boolean str_is_email_valid(String email) {
        return (Boolean) this.call(Boolean.class, new String[]{"str", "is_email_valid"}, new
                V8Array(v8).push(email));
    }

    public V8Object str_parse_email(String email) { // {email: str, name: str}
        return (V8Object) this.call(Object.class, new String[]{"str", "parse_email"}, new
                V8Array(v8).push(email));
    }

    public String str_base64url_encode(String str) {
        return (String) this.call(String.class, new String[]{"str", "base64url_encode"}, new
                V8Array(v8).push(str));
    }

    public String str_base64url_decode(String str) {
        return (String) this.call(String.class, new String[]{"str", "base64url_decode"}, new
                V8Array(v8).push(str));
    }

    public MimeMessage mime_decode(String mime_message) {
        this.call(Object.class, new String[]{"mime", "decode"}, new V8Array(v8).push(mime_message)
                .push(cb_catcher));
        if((Boolean) cb_last_value[0]) {
            return new MimeMessage((V8Object) cb_last_value[1]);
        } else {
            return null;
        }
    }

    public String crypto_key_normalize(String armored_key) {
        return (String) this.call(String.class, new String[]{"crypto", "key", "normalize"}, new
                V8Array(v8).push(armored_key));
    }

    public Key crypto_key_read(String armored_key) {
        return new Key((V8Object) this.call(Object.class, new String[]{"crypto", "key", "read"}, new
                V8Array(v8).push(armored_key)));
    }

    public V8Object crypto_key_decrypt(Key private_key, String passphrase) {
        return (V8Object) this.call(Object.class, new String[]{"crypto", "key", "decrypt"}, new
                V8Array(v8).push(private_key.getV8Object()).push(passphrase));
    }

    public String crypto_message_encrypt(String pubkeys[], String text, Boolean armor) {
        V8Array params = new V8Array(v8).push(this.array(pubkeys)).push(V8Value.NULL)
                .push(V8Value.NULL).push(text).push(V8Value.NULL).push(armor).push(cb_catcher);
        this.call(void.class, new String[]{"crypto", "message", "encrypt"}, params);
        return ((V8Object) cb_last_value[0]).get("data").toString();
    }

    public String api_gmail_query_backups(String account_email) {
        return (String) this.call(String.class, new String[]{"api", "gmail", "query", "backups"},
                new V8Array(v8).push(account_email));
    }

    public IdToken api_auth_parse_id_token(String id_token) {
        return new IdToken((V8Object) this.call(Object.class, new String[]{"api", "auth",
                "parse_id_token"}, new V8Array(v8).push(id_token)));
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
        } else if (return_type == Void.class) {
            obj.executeVoidFunction(path[path.length - 1], args);
            return null;
        } else {
            return obj.executeObjectFunction(path[path.length - 1], args);
        }
    }

    private V8Array array(String arr[]) {
        V8Array v8arr = new V8Array(v8);
        for (String v : arr) {
            v8arr.push(v);
        }
        return v8arr;
    }

    private static String read(File file) throws IOException {
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    private static String read(InputStream inputStream) throws IOException {
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }

}

class MeaningfulV8ObjectContainer {

    protected V8Object v8object;

    MeaningfulV8ObjectContainer(V8Object o) {
        v8object = o;
    }

    V8Object getV8Object() {
        return v8object;
    }

    protected String getAttributeAsString(String k) {
        return getAttributeAsString(v8object, k);
    }

    protected String getAttributeAsString(V8Object obj, String k) {
        try {
            return obj.getString(k);
        } catch (V8ResultUndefined e) {
            return null;
        }
    }

    public V8Array getAttributeAsArray(String k) {
        return getAttributeAsArray(v8object, k);
    }

    public V8Array getAttributeAsArray(V8Object obj, String k) {
        try {
            return obj.getArray(k);
        } catch (V8ResultUndefined e) {
            return null;
        }
    }

    public V8Object getAttributeAsObject(String name) {
        return getAttributeAsObject(v8object, name);
    }

    public V8Object getAttributeAsObject(V8Object obj, String k) {
        try {
            return obj.getObject(k);
        } catch (V8ResultUndefined e) {
            return null;
        }
    }
}

class JavascriptConsoleForwarder {

    public void log(final String message) {
        System.out.println("[JAVASCRIPT.CONSOLE.LOG] " + message);
    }

    public void error(final String message) {
        System.out.println("[JAVASCRIPT.CONSOLE.ERROR] " + message);
    }

}