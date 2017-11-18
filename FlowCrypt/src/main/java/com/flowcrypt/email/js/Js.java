/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: Tom James Holub
 */

package com.flowcrypt.email.js;

import android.content.Context;
import com.flowcrypt.email.BuildConfig;
import android.os.Build;
import android.text.Html;
import android.text.TextUtils;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8ArrayBuffer;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8ResultUndefined;
import com.eclipsesource.v8.V8TypedArray;
import com.eclipsesource.v8.V8Value;
import com.eclipsesource.v8.utils.typedarrays.ArrayBuffer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;


public class Js { // Create one object per thread and use them separately. Not thread-safe.

    private final StorageConnectorInterface storage;
    private final Context context;
    private final V8 v8;
    private final V8Object tool;
    private final int NULL = V8Value.NULL;
    private V8Function cb_catcher;
    private Object[] cb_last_value = new Object[3];

    public Js(Context context, StorageConnectorInterface storage) throws IOException {
        if (context != null) {
            this.context = context.getApplicationContext();
        } else {
            throw new IllegalArgumentException("The context can not be null!");
        }
        this.storage = storage;
        this.v8 = V8.createV8Runtime();
        bindJavaMethods();
        tool = loadJavascriptCode();
        bindCallbackCatcher();
    }

    public Boolean str_is_email_valid(String email) {
        return (Boolean) this.call(Boolean.class, new String[]{"str", "is_email_valid"}, new V8Array(v8).push(email));
    }

    public PgpContact str_parse_email(String email) {
        V8Object e = (V8Object) this.call(Object.class, new String[]{"str", "parse_email"}, new V8Array(v8).push(email));
        return new PgpContact(e.getString("email"), e.getString("name"));
    }

    public String str_base64url_encode(String str) {
        return (String) this.call(String.class, new String[]{"str", "base64url_encode"}, new V8Array(v8).push(str));
    }

    public String str_base64url_decode(String str) {
        return (String) this.call(String.class, new String[]{"str", "base64url_decode"}, new V8Array(v8).push(str));
    }

    public long time_to_utc_timestamp(String str) {
        return Long.parseLong((String) this.call(String.class, new String[]{"time", "to_utc_timestamp"},
                new V8Array(v8).push(str).push(true)));
    }

    public MimeMessage mime_decode(String mime_message) {
        this.call(Object.class, new String[]{"mime", "decode"}, new V8Array(v8).push(mime_message).push(cb_catcher));
        if ((Boolean) cb_last_value[0]) {
            return new MimeMessage((V8Object) cb_last_value[1], this);
        } else {
            return null;
        }
    }

    public String mime_encode(String body, PgpContact[] to, PgpContact from, String subject, Attachment[] attachments,
                              MimeMessage reply_to) {
        V8Object headers = (reply_to == null) ? new V8Object(v8) : mime_reply_headers(reply_to);
        headers.add("to", PgpContact.arrayAsMime(to)).add("from", from.getMime()).add("subject", subject);
        V8Array files = new V8Array(v8);
        if (attachments != null && attachments.length > 0) {
            for (Attachment attachment: attachments) {
                files.push(attachment.getV8Object());
            }
        }
        this.call(
            Void.class, new String[]{"mime", "encode"},
            new V8Array(v8).push(body).push(headers).push(files).push(cb_catcher)
        );
        return (String) cb_last_value[0];
    }

    public ProcessedMime mime_process(String mime_message) {
        this.call(Object.class, new String[]{"mime", "process"}, new V8Array(v8).push(mime_message).push(cb_catcher));
        return new ProcessedMime((V8Object) cb_last_value[0], this);
    }

    public String crypto_key_normalize(String armored_key) {
        return (String) this.call(String.class, new String[]{"crypto", "key", "normalize"}, new V8Array(v8)
                .push(armored_key));
    }

    public PgpKey crypto_key_read(String armored_key) {
        return new PgpKey((V8Object) this.call(Object.class, new String[]{"crypto", "key", "read"}, new V8Array(v8)
                .push(armored_key)), this);
    }

    public V8Object crypto_key_decrypt(PgpKey private_key, String passphrase) {
        return (V8Object) this.call(Object.class, new String[]{"crypto", "key", "decrypt"}, new V8Array(v8)
                .push(private_key.getV8Object()).push(passphrase));
    }

    public String crypto_key_fingerprint(PgpKey key) {
        return (String) this.call(String.class, new String[]{"crypto", "key", "fingerprint"}, new V8Array(v8)
                .push(key.getV8Object()));
    }

    public String crypto_key_longid(PgpKey key) {
        return (String) this.call(String.class, new String[]{"crypto", "key", "longid"}, new V8Array(v8)
                .push(key.getV8Object()));
    }

    public String crypto_key_longid(String fingerprint) {
        return (String) this.call(String.class, new String[]{"crypto", "key", "longid"}, new V8Array(v8)
                .push(fingerprint));
    }

    public String crypto_armor_clip(String text) {
        return (String) this.call(String.class, new String[]{"crypto", "armor", "clip"}, new V8Array(v8).push(text));
    }

    public String mnemonic(String longid) {
        return (String) this.call(String.class, v8, new String[]{"mnemonic"}, new V8Array(v8).push(longid));
    }

    public String crypto_message_encrypt(String pubkeys[], String text) {
        V8Array params = new V8Array(v8).push(this.array(pubkeys)).push(NULL).push(NULL).push(text).push(NULL)
                .push(true).push(cb_catcher);
        this.call(void.class, new String[]{"crypto", "message", "encrypt"}, params);
        return ((V8Object) cb_last_value[0]).get("data").toString();
    }

    public byte[] crypto_message_encrypt(String pubkeys[], byte[] content, String filename) {
        V8Array params = new V8Array(v8).push(this.array(pubkeys)).push(NULL).push(NULL).push(uint8(content))
                .push(filename).push(false).push(cb_catcher);
        this.call(void.class, new String[]{"crypto", "message", "encrypt"}, params);
        V8Object packets = (V8Object) ((V8Object)((V8Object) cb_last_value[0]).get("message")).get("packets");
        V8TypedArray data = (V8TypedArray) packets.executeObjectFunction("write", new V8Array(v8));
        return data.getBytes(0, data.length());
    }

    public PgpDecrypted crypto_message_decrypt(String data, String password) {
        // db,account_email,encrypted_data,one_time_message_password,callback,force_output_format
        V8Array params = new V8Array(v8).push(NULL).push("").push(data).push(password).push(cb_catcher).push(NULL);
        this.call(void.class, new String[]{"crypto", "message", "decrypt"}, params);
        return new PgpDecrypted((V8Object) cb_last_value[0]);
    }

    public PgpDecrypted crypto_message_decrypt(String data) {
        return crypto_message_decrypt(data, "");
    }

    public PgpDecrypted crypto_message_decrypt(byte[] bytes) {
        // db,account_email,encrypted_data,one_time_message_password,callback,force_output_format
        V8Array params = new V8Array(v8).push(NULL).push("").push(uint8(bytes)).push("").push(cb_catcher).push(NULL);
        this.call(void.class, new String[]{"crypto", "message", "decrypt"}, params);
        return new PgpDecrypted((V8Object) cb_last_value[0]);
    }

    public String api_gmail_query_backups(String account_email) {
        return (String) this.call(String.class, new String[]{"api", "gmail", "query", "backups"},
                new V8Array(v8).push(account_email));
    }

    public IdToken api_auth_parse_id_token(String id_token) {
        return new IdToken((V8Object) this.call(Object.class, new String[]{"api", "auth", "parse_id_token"},
                new V8Array(v8).push(id_token)));
    }

    /**
     * Check that the key has a valid structure.
     *
     * @param armoredPrivateKey The armored private key.
     * @param isPrivateKey      true if this key must be private, otherwise false.
     * @return true if private key has valid structure, otherwise false.
     */
    public boolean is_valid_key(String armoredPrivateKey, boolean isPrivateKey) {
        String normalizedArmoredKey = crypto_key_normalize(armoredPrivateKey);
        PgpKey pgpKey = crypto_key_read(normalizedArmoredKey);
        return !TextUtils.isEmpty(pgpKey.getLongid())
                && !TextUtils.isEmpty(pgpKey.getFingerprint())
                && pgpKey.getPrimaryUserId() != null
                && (isPrivateKey ? pgpKey.isPrivate() : !pgpKey.isPrivate());
    }

    /**
     * Check that the key has a valid structure.
     *
     * @param pgpKey The {@link PgpKey} object.
     * @return true if private key has valid structure, otherwise false.
     */
    public boolean is_valid_key(PgpKey pgpKey, boolean isPrivateKey) {
        return !TextUtils.isEmpty(pgpKey.getLongid())
                && !TextUtils.isEmpty(pgpKey.getFingerprint())
                && pgpKey.getPrimaryUserId() != null
                && (isPrivateKey ? pgpKey.isPrivate() : !pgpKey.isPrivate());
    }

    public Attachment file_attachment(byte[] content, String name, String type) {
        return new Attachment((V8Object) this.call(V8Object.class, new String[]{"file", "attachment"},
                new V8Array(v8).push(name).push(type).push(uint8(content))));
    }

    private V8TypedArray uint8(byte[] data) {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, new ArrayBuffer(data).getByteBuffer());
        return new V8TypedArray(v8, buffer, V8Value.UNSIGNED_INT_8_ARRAY, 0, data.length);
    }

    private static String read(File file) throws IOException {
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    private static String read(InputStream inputStream) throws IOException {
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }

    private V8Object mime_reply_headers(MimeMessage original) {
        return (V8Object) this.call(Object.class, new String[]{"mime", "reply_headers"}, new V8Array(v8)
                .push(original.getV8Object()));
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

    private void bindJavaMethods() {
        JavaMethodsForJavascript methods = new JavaMethodsForJavascript(v8, storage);
        v8.registerJavaMethod(methods, "console_log", "$_HOST_console_log", new Class[]{String.class});
        v8.registerJavaMethod(methods, "console_error", "$_HOST_console_error", new Class[]{String.class});
        v8.registerJavaMethod(methods, "alert", "$_HOST_alert", new Class[]{String.class});
        v8.registerJavaMethod(methods, "storage_keys_get", "$_HOST_storage_keys_get", new Class[]{String.class, V8Array.class});
        v8.registerJavaMethod(methods, "storage_keys_get", "$_HOST_storage_keys_get", new Class[]{String.class, String.class});
        v8.registerJavaMethod(methods, "storage_keys_get", "$_HOST_storage_keys_get", new Class[]{String.class});
        v8.registerJavaMethod(methods, "storage_passphrase_get", "$_HOST_storage_passphrase_get", new Class[]{String.class, String.class});
        v8.registerJavaMethod(methods, "mod_pow_strings", "$_HOST_mod_pow", new Class[]{String.class, String.class, String.class});
        v8.registerJavaMethod(methods, "secure_random", "$_HOST_secure_random", new Class[]{Integer.class});
        v8.registerJavaMethod(methods, "html_to_text", "$_HOST_html_to_text", new Class[]{String.class});
        v8.registerJavaMethod(methods, "rsa_decrypt", "$_HOST_rsa_decrypt", new Class[]{String.class, String.class, V8Array.class});
    }

    private V8Object loadJavascriptCode() throws IOException {
        v8.executeScript("var engine_host_version = 'Android " + BuildConfig.VERSION_NAME.split("_")[0] + "';");
        v8.executeScript(read(context.getAssets().open("js/window.js")));
        v8.executeScript(read(context.getAssets().open("js/openpgp.js")));
        v8.executeScript(read(context.getAssets().open("js/emailjs/punycode.js")));
        v8.executeScript(read(context.getAssets().open("js/emailjs/emailjs-stringencoding.js")));
        v8.executeScript(read(context.getAssets().open("js/emailjs/emailjs-addressparser.js")));
        v8.executeScript(read(context.getAssets().open("js/emailjs/emailjs-mime-codec.js")));
        v8.executeScript(read(context.getAssets().open("js/emailjs/emailjs-mime-parser.js")));
        v8.executeScript(read(context.getAssets().open("js/emailjs/emailjs-mime-types.js")));
        v8.executeScript(read(context.getAssets().open("js/emailjs/emailjs-mime-builder.js")));
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
                for (Integer i = 0; i < parameters.length(); i++) {
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

    V8Array getAttributeAsArray(String k) {
        return getAttributeAsArray(v8object, k);
    }

    V8Object getAttributeAsObject(String name) {
        return getAttributeAsObject(v8object, name);
    }

    Boolean getAttributeAsBoolean(String name) {
        return getAttributeAsBoolean(v8object, name);
    }

    Integer getAttributeAsInteger(String name) {
        return getAttributeAsInteger(v8object, name);
    }

    String getAttributeAsString(String k) {
        return getAttributeAsString(v8object, k);
    }

    byte[] getAttributeAsBytes(String k) {
        return getAttributeAsBytes(v8object, k);
    }

    static V8Array getAttributeAsArray(V8Object obj, String k) {
        try {
            return obj.getArray(k);
        } catch (V8ResultUndefined e) {
            return null;
        }
    }

    static V8Object getAttributeAsObject(V8Object obj, String k) {
        try {
            return obj.getObject(k);
        } catch (V8ResultUndefined e) {
            return null;
        }
    }

    static Boolean getAttributeAsBoolean(V8Object obj, String k) {
        try {
            return obj.getBoolean(k);
        } catch (V8ResultUndefined e) {
            return null;
        }
    }

    static Integer getAttributeAsInteger(V8Object obj, String k) {
        try {
            return obj.getInteger(k);
        } catch (V8ResultUndefined e) {
            return null;
        }
    }

    static String getAttributeAsString(V8Object obj, String k) {
        try {
            return obj.getString(k);
        } catch (V8ResultUndefined e) {
            return null;
        }
    }

    static byte[] getAttributeAsBytes(V8Object obj, String k) {
        try {
            V8TypedArray typedArray = (V8TypedArray) obj.getObject(k);
            return typedArray.getBytes(0, typedArray.length());
        } catch (V8ResultUndefined e) {
            return null;
        }
    }

    V8Object getV8Object() {
        return v8object;
    }
}

class JavaMethodsForJavascript {

    private final StorageConnectorInterface storage;
    private final V8 v8;

    JavaMethodsForJavascript(V8 v8, StorageConnectorInterface storage) {
        this.storage = storage;
        this.v8 = v8;
    }

    public V8Object storage_keys_get(String account_email, String longid) {
        PgpKeyInfo ki = this.storage.getPgpPrivateKey(longid);
        if (ki == null) {
            return null;
        }
        return new V8Object(v8).add("private", ki.getPrivate()).add("longid", ki.getLongid());
    }

    public V8Array storage_keys_get(String account_email) {
        V8Array result = new V8Array(v8);
        for (PgpKeyInfo ki : this.storage.getAllPgpPrivateKeys()) {
            result.push(new V8Object(v8).add("private", ki.getPrivate()).add("longid", ki.getLongid()));
        }
        return result;
    }

    public V8Array storage_keys_get(String account_email, V8Array longid) {
        V8Array result = new V8Array(v8);
        for (PgpKeyInfo ki : this.storage.getFilteredPgpPrivateKeys(longid.getStrings(0, longid.length()))) {
            result.push(new V8Object(v8).add("private", ki.getPrivate()).add("longid", ki.getLongid()));
        }
        return result;
    }

    public String storage_passphrase_get(String account_email, String longid) {
        return this.storage.getPassphrase(longid);
    }

    public void console_log(final String message) {
        System.out.println("[JAVASCRIPT.CONSOLE.LOG] " + message);
    }

    public void console_error(final String message) {
        System.err.println("[JAVASCRIPT.CONSOLE.ERROR] " + message);
    }

    public String mod_pow_strings(String b, String e, String m) {
        return mod_pow(new BigInteger(b), new BigInteger(e), new BigInteger(m)).toString();
    }

    public String html_to_text(String html) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            return Html.fromHtml(html).toString();
        }
    }

    // Do modular exponentiation for the expression b^e mod m
    // (b to the power e, modulo m).
    public BigInteger mod_pow(BigInteger b, BigInteger e, BigInteger m) {
        // prints the calculations
        // System.out.println(b + " " + e + " " + m);
        BigInteger zero = new BigInteger("0");
        BigInteger one = new BigInteger("1");
        BigInteger two = one.add(one);

        // Base Case
        if (e.equals(zero))
            return one;
        if (e.equals(one))
            return b.mod(m);

        if (e.mod(two).equals(zero)) {
            // Calculates the square root of the answer
            BigInteger answer = mod_pow(b, e.divide(two), m);
            // Reuses the result of the square root
            return (answer.multiply(answer)).mod(m);
        }

        return (b.multiply(mod_pow(b, e.subtract(one), m))).mod(m);
    }

    public String rsa_decrypt(String modulus, String exponent, V8Array encrypted) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            KeySpec keySpec = new RSAPrivateKeySpec(new BigInteger(modulus), new BigInteger(exponent));
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            Cipher decryptCipher = Cipher.getInstance("RSA/ECB/NoPadding");
            decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decrypted_bytes = decryptCipher.doFinal(encrypted.getBytes(0, encrypted.length()));
            return new BigInteger(decrypted_bytes).toString();
        } catch (Exception e) {
            System.out.println("JAVA RSA ERROR:" + e.getClass() + " --- " + e.getMessage());
        }
        return "";
    }

    public V8Array secure_random(Integer byte_length) {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[byte_length];
        random.nextBytes(bytes);
        V8Array array = new V8Array(v8);
        for (Integer i = 0; i < byte_length; i++) {
            array.push((int) bytes[i] + 128); // signed to unsigned conversion to get random 0-255
        }
        return array;
    }

    public void alert(final String message) {
        System.out.println("[JAVASCRIPT.ALERT] " + message);
    }
}