/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import android.content.Context;
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
import com.flowcrypt.email.BuildConfig;

import org.acra.ACRA;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;


@SuppressWarnings("WeakerAccess")
public class Js { // Create one object per thread and use them separately. Not thread-safe.

  private final StorageConnectorInterface storage;
  private final Context context;
  private final V8 v8;
  private final V8Object tool;
  private final int NULL = V8Value.NULL;
  private V8Function cb_catch;
  private Object[] cb_last_value = new Object[3];
  private Class str = String.class;
  private Class arr = V8Array.class;

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

  public StorageConnectorInterface getStorageConnector() {
    return storage;
  }

  public Boolean str_is_email_valid(String email) {
    return (Boolean) this.call(Boolean.class, p("str", "is_email_valid"), new V8Array(v8).push(email));
  }

  public PgpContact str_parse_email(String email) {
    V8Object e = (V8Object) this.call(Object.class, p("str", "parse_email"), new V8Array(v8).push(email));
    return new PgpContact(e.getString("email"), e.getString("name"));
  }

  public long time_to_utc_timestamp(String string) {
    if (TextUtils.isEmpty(string) || "NaN".equalsIgnoreCase(string)) {
      return -1;
    }

    String number = (String) this.call(str, p("time", "to_utc_timestamp"),
        new V8Array(v8).push(string).push(true));

    try {
      return Long.parseLong(number);
    } catch (NumberFormatException e) {
      e.printStackTrace();
      return -1;
    }
  }

  public MimeMessage mime_decode(String mime_message) {
    this.call(Object.class, p("mime", "decode"), new V8Array(v8).push(mime_message).push(cb_catch));
    if ((Boolean) cb_last_value[0]) {
      return new MimeMessage((V8Object) cb_last_value[1], this);
    } else {
      return null;
    }
  }

  public String mime_encode(String body, PgpContact[] to, PgpContact[] cc, PgpContact[] bcc,
                            PgpContact from, String subject, Attachment[] attachments, MimeMessage reply_to) {
    V8Object headers = (reply_to == null) ? new V8Object(v8) : mime_reply_headers(reply_to);
    headers.add("to", PgpContact.arrayAsMime(to)).add("from", from.getMime()).add("subject", subject);

    if (cc != null && cc.length > 0) {
      headers.add("cc", PgpContact.arrayAsMime(cc));
    }

    if (bcc != null && bcc.length > 0) {
      headers.add("bcc", PgpContact.arrayAsMime(bcc));
    }

    V8Array files = new V8Array(v8);
    if (attachments != null && attachments.length > 0) {
      for (Attachment attachment : attachments) {
        files.push(attachment.getV8Object());
      }
    }
    this.call(Void.class, p("mime", "encode"), new V8Array(v8).push(body).push(headers).push(files).push(cb_catch));
    return (String) cb_last_value[0];
  }

  public ProcessedMime mime_process(String mime_message) {
    this.call(Object.class, p("mime", "process"), new V8Array(v8).push(mime_message).push(cb_catch));
    return new ProcessedMime((V8Object) cb_last_value[0], this);
  }

  public MessageBlock[] crypto_armor_detect_blocks(String text) {
    V8Array blocks = ((V8Array) this.call(Object.class, p("crypto", "armor", "detect_blocks"),
        new V8Array(v8).push(text)));
    return MessageBlock.arrayFromV8Array(blocks);
  }

  public String crypto_key_normalize(String armored_key) {
    try {
      return (String) this.call(str, p("crypto", "key", "normalize"), new V8Array(v8).push(armored_key));
    } catch (com.eclipsesource.v8.V8ResultUndefined e) {
      return "";
    }
  }

  public PgpKey crypto_key_read(String armored_key) {
    return new PgpKey((V8Object) this.call(Object.class, p("crypto", "key", "read"), new V8Array(v8)
        .push(armored_key)), this);
  }

  public PgpKey crypto_key_create(PgpContact[] user_ids, int num_bits, String pass_phrase) {
    V8Array args = new V8Array(v8).push(PgpContact.arrayAsV8UserIds(v8, user_ids)).push(num_bits).push(pass_phrase)
        .push(cb_catch);
    this.call(void.class, p("crypto", "key", "create"), args);
    return crypto_key_read((String) cb_last_value[0]);
  }

  public V8Object crypto_key_decrypt(PgpKey private_key, String passphrase) {
    return (V8Object) this.call(Object.class, p("crypto", "key", "decrypt"), new V8Array(v8)
        .push(private_key.getV8Object()).push(passphrase));
  }

  public String crypto_key_fingerprint(PgpKey k) {
    return (String) this.call(str, p("crypto", "key", "fingerprint"), new V8Array(v8).push(k.getV8Object()));
  }

  public String crypto_key_longid(PgpKey k) {
    return (String) this.call(str, p("crypto", "key", "longid"), new V8Array(v8).push(k.getV8Object()));
  }

  public String crypto_key_longid(String fingerprint) {
    return (String) this.call(str, p("crypto", "key", "longid"), new V8Array(v8).push(fingerprint));
  }

  public String mnemonic(String longid) {
    return (String) this.call(str, v8, p("mnemonic"), new V8Array(v8).push(longid));
  }

  public String crypto_message_encrypt(String pubkeys[], String text) {
    V8Array params = new V8Array(v8).push(this.array(pubkeys)).push(NULL).push(NULL).push(text).push(NULL)
        .push(true).push(cb_catch);
    this.call(void.class, p("crypto", "message", "encrypt"), params);
    return ((V8Object) cb_last_value[0]).get("data").toString();
  }

  public byte[] crypto_message_encrypt(String pubkeys[], byte[] content, String filename) {
    V8Array params = new V8Array(v8).push(this.array(pubkeys)).push(NULL).push(NULL).push(uint8(content))
        .push(filename).push(false).push(cb_catch);
    this.call(void.class, p("crypto", "message", "encrypt"), params);
    V8Object packets = (V8Object) ((V8Object) ((V8Object) cb_last_value[0]).get("message")).get("packets");
    V8TypedArray data = (V8TypedArray) packets.executeObjectFunction("write", new V8Array(v8));
    return data.getBytes(0, data.length());
  }

  public PgpDecrypted crypto_message_decrypt(String data, String password) {
    // db,account_email,encrypted_data,one_time_message_password,callback,force_output_format
    V8Array params = new V8Array(v8).push(NULL).push("").push(data).push(password).push(cb_catch).push(NULL);
    this.call(void.class, p("crypto", "message", "decrypt"), params);
    return new PgpDecrypted((V8Object) cb_last_value[0]);
  }

  public PgpDecrypted crypto_message_decrypt(String data) {
    return crypto_message_decrypt(data, "");
  }

  public PgpDecrypted crypto_message_decrypt(byte[] bytes) {
    // db,account_email,encrypted_data,one_time_message_password,callback,force_output_format
    V8Array params = new V8Array(v8).push(NULL).push("").push(uint8(bytes)).push("").push(cb_catch).push(NULL);
    this.call(void.class, p("crypto", "message", "decrypt"), params);
    return new PgpDecrypted((V8Object) cb_last_value[0]);
  }

  public List<String> crypto_password_weak_words() {
    V8Array a = ((V8Array) this.call(Object.class, p("crypto", "password", "weak_words"), new V8Array(v8)));
    List<String> list = new ArrayList<>();
    for (int i = 0; i < a.length(); i++) {
      list.add(a.getString(i));
    }
    return list;
  }

  public PasswordStrength crypto_password_estimate_strength(double zxcvbn_guesses) {
    return new PasswordStrength((V8Object) this.call(Object.class, p("crypto", "password", "estimate_strength"),
        new V8Array(v8).push(zxcvbn_guesses)));
  }

  public String api_gmail_query_backups(String email) {
    return (String) this.call(str, p("api", "gmail", "query", "backups"), new V8Array(v8).push(email));
  }

  /**
   * Check that the key has valid structure.
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
   * Check that the key has valid structure.
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
    return new Attachment((V8Object) this.call(V8Object.class, p("file", "attachment"), new V8Array(v8)
        .push(name).push(type).push(uint8(content))));
  }

  private static String read(InputStream inputStream) throws IOException {
    return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
  }

  private V8TypedArray uint8(byte[] data) {
    V8ArrayBuffer buffer = new V8ArrayBuffer(v8, new ArrayBuffer(data).getByteBuffer());
    return new V8TypedArray(v8, buffer, V8Value.UNSIGNED_INT_8_ARRAY, 0, data.length);
  }

  private V8Object mime_reply_headers(MimeMessage m) {
    return (V8Object) this.call(Object.class, p("mime", "reply_headers"), new V8Array(v8).push(m.getV8Object()));
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
    if (return_type == str) {
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

  private V8Array array(String a[]) {
    V8Array v8arr = new V8Array(v8);
    for (String v : a) {
      v8arr.push(v);
    }
    return v8arr;
  }

  private String[] p(String... js) {
    return js;
  }

  private Class[] args(Class... classes) {
    return classes;
  }

  private void bindJavaMethods() {
    JavaMethodsForJavaScript m = new JavaMethodsForJavaScript(v8, storage);
    v8.registerJavaMethod(m, "console_log", "$_HOST_console_log", args(str));
    v8.registerJavaMethod(m, "console_error", "$_HOST_console_error", args(str));
    v8.registerJavaMethod(m, "alert", "$_HOST_alert", args(str));
    v8.registerJavaMethod(m, "report", "$_HOST_report", args(Boolean.class, str, str, str));
    v8.registerJavaMethod(m, "storage_keys_get", "$_HOST_storage_keys_get", args(str, arr));
    v8.registerJavaMethod(m, "storage_keys_get", "$_HOST_storage_keys_get", args(str, str));
    v8.registerJavaMethod(m, "storage_keys_get", "$_HOST_storage_keys_get", args(str));
    v8.registerJavaMethod(m, "storage_passphrase_get", "$_HOST_storage_passphrase_get", args(str, str));
    v8.registerJavaMethod(m, "mod_pow_strings", "$_HOST_mod_pow", args(str, str, str));
    v8.registerJavaMethod(m, "secure_random", "$_HOST_secure_random", args(Integer.class));
    v8.registerJavaMethod(m, "html_to_text", "$_HOST_html_to_text", args(str));
    v8.registerJavaMethod(m, "rsa_decrypt", "$_HOST_rsa_decrypt", args(str, str, arr));
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
    v8.executeScript(read(context.getAssets().open("js/common.js")));
    return v8.getObject("window").getObject("tool");
  }

  private void bindCallbackCatcher() {
    cb_catch = new V8Function(v8, new JavaCallback() {
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

@SuppressWarnings("WeakerAccess")
class MeaningfulV8ObjectContainer {

  protected V8Object v8object;

  MeaningfulV8ObjectContainer(V8Object o) {
    v8object = o;
  }

  static V8Array getAttributeAsArray(V8Object obj, String k) {
    try {
      V8Array a = obj.getArray(k);
      return a.isUndefined() ? null : a;
    } catch (V8ResultUndefined e) {
      return null;
    }
  }

  static V8Object getAttributeAsObject(V8Object obj, String k) {
    try {
      V8Object result = obj.getObject(k);
      return result.isUndefined() ? null : result;
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

  V8Object getV8Object() {
    return v8object;
  }
}


class JavaScriptError extends Exception {
  JavaScriptError(String msg) {
    super(msg);
  }
}

class JavaScriptReport extends Exception {
  JavaScriptReport(String msg) {
    super(msg);
  }
}

class JavaMethodsForJavaScript {

  private final StorageConnectorInterface storage;
  private final V8 v8;

  JavaMethodsForJavaScript(V8 v8, StorageConnectorInterface storage) {
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

  public void console_log(String message) {
    System.out.println("[JAVASCRIPT.CONSOLE.LOG] " + message);
  }

  public void console_error(String message) {
    System.err.println("[JAVASCRIPT.CONSOLE.ERROR] " + message);
  }

  public void report(Boolean isError, String title, String stack_trace, String details) {
    console_error(title);
    console_error(stack_trace);
    if (details.length() > 0) {
      console_error(details);
    }
    ACRA.getErrorReporter().putCustomData("JAVASCRIPT_TITLE", title);
    ACRA.getErrorReporter().putCustomData("JAVASCRIPT_STACK_TRACE", stack_trace);
    ACRA.getErrorReporter().putCustomData("JAVASCRIPT_DETAILS", details);
    if (isError) {
      ACRA.getErrorReporter().handleSilentException(new JavaScriptError(title));
    } else {
      ACRA.getErrorReporter().handleSilentException(new JavaScriptReport(title));
    }
    ACRA.getErrorReporter().removeCustomData("JAVASCRIPT_TITLE");
    ACRA.getErrorReporter().removeCustomData("JAVASCRIPT_STACK_TRACE");
    ACRA.getErrorReporter().removeCustomData("JAVASCRIPT_DETAILS");
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

  public BigInteger mod_pow(BigInteger b, BigInteger e, BigInteger m) {
    // Do modular exponentiation for the expression b^e mod m (b to the power e, modulo m).
    BigInteger zero = new BigInteger("0");
    BigInteger one = new BigInteger("1");
    BigInteger two = one.add(one);
    if (e.equals(zero)) {
      return one;
    }
    if (e.equals(one)) {
      return b.mod(m);
    }
    if (e.mod(two).equals(zero)) {
      BigInteger answer = mod_pow(b, e.divide(two), m); // Calculates the square root of the answer
      return (answer.multiply(answer)).mod(m); // Reuses the result of the square root
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