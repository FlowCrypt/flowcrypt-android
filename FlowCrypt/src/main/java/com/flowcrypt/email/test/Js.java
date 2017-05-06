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


public class Js { // Create one object per thread and use them separately. Not thread-safe.

    private final StorageConnectorInterface storage;
    private final Context context;
    private final V8 v8;
    private final V8Object tool;
    private V8Function cb_catcher;
    private Object[] cb_last_value = new Object[3];

    public Js(Context context, StorageConnectorInterface storage) throws IOException {
        this.context = context;
        this.storage = storage;
        this.v8 = V8.createV8Runtime();
        bindJavaMethods();
        tool = loadJavascriptCode();
        bindCallbackCatcher();
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

    public long time_to_utc_timestamp(String str) {
        return Long.parseLong((String) this.call(String.class, new String[]{"time", "to_utc_timestamp"}, new
                V8Array(v8).push(str).push(true)));
    }

    public MimeMessage mime_decode(String mime_message) {
        this.call(Object.class, new String[]{"mime", "decode"}, new V8Array(v8).push(mime_message)
                .push(cb_catcher));
        if((Boolean) cb_last_value[0]) {
            return new MimeMessage((V8Object) cb_last_value[1], this);
        } else {
            return null;
        }
    }

    public String crypto_key_normalize(String armored_key) {
        return (String) this.call(String.class, new String[]{"crypto", "key", "normalize"}, new
                V8Array(v8).push(armored_key));
    }

    public PgpKey crypto_key_read(String armored_key) {
        return new PgpKey((V8Object) this.call(Object.class, new String[]{"crypto", "key", "read"}, new
                V8Array(v8).push(armored_key)), this);
    }

    public V8Object crypto_key_decrypt(PgpKey private_key, String passphrase) {
        return (V8Object) this.call(Object.class, new String[]{"crypto", "key", "decrypt"}, new
                V8Array(v8).push(private_key.getV8Object()).push(passphrase));
    }

    public String crypto_key_fingerprint(PgpKey key) {
        return (String) this.call(String.class, new String[]{"crypto", "key", "fingerprint"}, new
                V8Array(v8).push(key.getV8Object()));
    }

    public String crypto_key_longid(PgpKey key) {
        return (String) this.call(String.class, new String[]{"crypto", "key", "longid"}, new
                V8Array(v8).push(key.getV8Object()));
    }

    public String crypto_key_longid(String fingerprint) {
        return (String) this.call(String.class, new String[]{"crypto", "key", "longid"}, new
                V8Array(v8).push(fingerprint));
    }

    public String crypto_armor_clip(String text) {
        return (String) this.call(String.class, new String[]{"crypto", "armor", "clip"}, new
                V8Array(v8).push(text));
    }

    public String mnemonic(String longid) {
        return (String) this.call(String.class, v8, new String[]{"mnemonic"}, new
                V8Array(v8).push(longid));
    }

    public String crypto_message_encrypt(String pubkeys[], String text, Boolean armor) {
        V8Array params = new V8Array(v8).push(this.array(pubkeys)).push(V8Value.NULL)
                .push(V8Value.NULL).push(text).push(V8Value.NULL).push(armor).push(cb_catcher);
        this.call(void.class, new String[]{"crypto", "message", "encrypt"}, params);
        return ((V8Object) cb_last_value[0]).get("data").toString();
    }

    public PgpDecrypted crypto_message_decrypt(String data, String password) {
        // db,account_email,encrypted_data,one_time_message_password,callback,force_output_format
        V8Array params = new V8Array(v8).push(V8Value.NULL).push("").push(data)
                .push(password).push(cb_catcher).push(V8Value.NULL);
        this.call(void.class, new String[]{"crypto", "message", "decrypt"}, params);
        return new PgpDecrypted((V8Object) cb_last_value[0]);
    }

    public PgpDecrypted crypto_message_decrypt(String data) {
        return crypto_message_decrypt(data, "");
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
        return this.call(return_type, this.tool, path, args);
    }

    private Object call(Class<?> return_type, V8Object base, String path[], V8Array args) {
        V8Object obj = null;
        for (Integer i = 0; i < path.length - 1; i++) {
            obj = (obj == null) ? base.getObject(path[i]) : obj.getObject(path[i]);
        }
        if (obj == null) {
            obj = base;
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

    private void bindJavaMethods() {
        JavaMethodsForJavascript methods = new JavaMethodsForJavascript(v8, storage);
        v8.registerJavaMethod(methods, "alert", "alert", new Class[]{String.class});
        v8.registerJavaMethod(methods, "private_keys_get", "private_keys_get", new Class[]{String.class, V8Array.class});
        v8.registerJavaMethod(methods, "private_keys_get", "private_keys_get", new Class[]{String.class, String.class});
        v8.registerJavaMethod(methods, "private_keys_get", "private_keys_get", new Class[]{String.class});
        v8.registerJavaMethod(methods, "get_passphrase", "get_passphrase", new Class[]{String.class, String.class});
        V8Object v8Console = new V8Object(v8);
        v8.add("console", v8Console);
        v8Console.registerJavaMethod(methods, "console_log", "log", new Class[]{String.class});
        v8Console.registerJavaMethod(methods, "console_error", "error", new Class[]{String.class});
        v8Console.release();
    }

    private V8Object loadJavascriptCode() throws IOException {
        v8.executeScript(read(context.getAssets().open("js/window.js")));
        v8.executeScript(read(context.getAssets().open("js/openpgp.js")));
        v8.executeScript(read(context.getAssets().open("js/emailjs/emailjs-stringencoding.js")));
        v8.executeScript(read(context.getAssets().open("js/emailjs/emailjs-addressparser.js")));
        v8.executeScript(read(context.getAssets().open("js/emailjs/emailjs-mime-codec.js")));
        v8.executeScript(read(context.getAssets().open("js/emailjs/emailjs-mime-parser.js")));
        v8.executeScript(read(context.getAssets().open("js/mnemonic.js")));
        v8.executeScript(read(context.getAssets().open("js/global.js")));
        v8.executeScript(read(context.getAssets().open("js/tool.js")));
        return v8.getObject("window").getObject("tool");
    }

    private void bindCallbackCatcher() {
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

    public Boolean getAttributeAsBoolean(String name) {
        return getAttributeAsBoolean(v8object, name);
    }

    public Boolean getAttributeAsBoolean(V8Object obj, String k) {
        try {
            return obj.getBoolean(k);
        } catch (V8ResultUndefined e) {
            return null;
        }
    }

    public Integer getAttributeAsInteger(String name) {
        return getAttributeAsInteger(v8object, name);
    }

    public Integer getAttributeAsInteger(V8Object obj, String k) {
        try {
            return obj.getInteger(k);
        } catch (V8ResultUndefined e) {
            return null;
        }
    }
}

class JavaMethodsForJavascript {

    private final StorageConnectorInterface storage;
    private final V8 v8;

    JavaMethodsForJavascript(V8 v8, StorageConnectorInterface storage) {
        this.storage = storage;
        this.v8 = v8;
    }

    public V8Object private_keys_get(String account_email, String longid) {
        PgpKeyInfo ki = this.storage.getPgpPrivateKey(longid);
        if(ki == null) {
            return null;
        }
        return new V8Object(v8).add("armored", ki.getArmored()).add("longid", ki.getLongid());
    }

    public V8Array private_keys_get(String account_email) {
        V8Array result = new V8Array(v8);
        for(PgpKeyInfo ki: this.storage.getAllPgpPrivateKeys()) {
            result.push(new V8Object(v8).add("armored", ki.getArmored()).add("longid", ki.getLongid()));
        }
        return result;
    }

    public V8Array private_keys_get(String account_email, V8Array longid) {
        V8Array result = new V8Array(v8);
        for(PgpKeyInfo ki: this.storage.getFilteredPgpPrivateKeys(longid.getStrings(0, longid.length()))) {
            result.push(new V8Object(v8).add("armored", ki.getArmored()).add("longid", ki.getLongid()));
        }
        return result;
    }

    public String get_passphrase(String account_email, String longid) {
        return this.storage.getPassphrase(longid);
    }

    public void console_log(final String message) {
        System.out.println("[JAVASCRIPT.CONSOLE.LOG] " + message);
    }

    public void console_error(final String message) {
        System.err.println("[JAVASCRIPT.CONSOLE.ERROR] " + message);
    }

    public void alert(final String message) {
        System.out.println("[JAVASCRIPT.ALERT] " + message);
    }
}