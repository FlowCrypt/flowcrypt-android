/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js.core;

import android.os.Build;
import android.text.Html;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.flowcrypt.email.model.PgpKeyInfo;
import com.flowcrypt.email.model.StorageConnectorInterface;
import com.flowcrypt.email.util.exception.ExceptionUtil;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateKeySpec;

import javax.crypto.Cipher;

import androidx.annotation.Keep;

/**
 * @author Denis Bondarenko
 * Date: 12/4/18
 * Time: 3:30 PM
 * E-mail: DenBond7@gmail.com
 */
@Keep
public class JavaMethodsForJavaScript {

  private final StorageConnectorInterface storage;
  private final V8 v8;

  public JavaMethodsForJavaScript(V8 v8, StorageConnectorInterface storage) {
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
    ExceptionUtil.handleError(isError, title, stack_trace, details);
  }

  public String mod_pow_strings(String b, String e, String m) {
    return mod_pow(new BigInteger(b), new BigInteger(e), new BigInteger(m)).toString();
  }

  @SuppressWarnings("deprecation")
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
