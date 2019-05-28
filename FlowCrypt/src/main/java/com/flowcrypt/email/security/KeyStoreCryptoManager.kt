/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Base64;

import com.flowcrypt.email.broadcastreceivers.CorruptedStorageBroadcastReceiver;
import com.flowcrypt.email.util.exception.ManualHandledException;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class uses AndroidKeyStore for encrypt/decrypt information.
 * Since version 0.7.4 we use only AES cipher for encryption/decryption which uses
 * AndroidKeyStore for storing keys.
 * <p>
 * But to support the older versions for compatibility we also use RSA + AES schema.
 * Since encryption which uses the RSA has a limit on the maximum size of the data that can be encrypted
 * ("The RSA algorithm can only encrypt data that has a maximum byte length of the RSA key length in bits
 * divided with eight minus eleven padding bytes, i.e. number of maximum bytes = key length in bits / 8 - 11.", see
 * http://stackoverflow.com/questions/10007147/getting-a-illegalblocksizeexception-data-must-not-be-longer-than-256
 * -bytes-when), we use the following algorithm for option RSA + AES:
 * <ul>
 * <li>Generate an RSA key pair via {@code KeyPairGenerator.getInstance(
 * KeyProperties.KEY_ALGORITHM_RSA, PROVIDER_ANDROID_KEY_STORE)}</li>
 * <li>Generate a 128 bits symmetric key with use {@link SecureRandom}</li>
 * <li>Encrypt and save the symmetric key with the RSA key from Android Keystore System to the shared preferences</li>
 * <li>Encrypt the data with the decrypted symmetric key</li>
 * <li>Decrypt the data with the decrypted symmetric key</li>
 * </ul>
 * <p>
 * See the great articles https://proandroiddev.com/secure-data-in-android-encryption-7eda33e68f58
 * <p>
 *
 * @author DenBond7
 * Date: 12.05.2017
 * Time: 12:29
 * E-mail: DenBond7@gmail.com
 * @version 2.0 Added using AES cipher for encryption/decryption which uses AndroidKeyStore for storing keys.
 */

public final class KeyStoreCryptoManager {
  private static final int SIZE_OF_ALGORITHM_PARAMETER_SPEC = 16;
  private static final String PREFERENCE_KEY_SECRET = "preference_key_secret";
  private static final String TRANSFORMATION_TYPE_RSA_ECB_PKCS1_PADDING = "RSA/ECB/PKCS1Padding";
  private static final String TRANSFORMATION_AES_CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding";
  private static final String TRANSFORMATION_AES_CBC_PKCS7_PADDING = "AES/CBC/PKCS7Padding";
  private static final String PROVIDER_ANDROID_KEY_STORE = "AndroidKeyStore";
  private static final String ANDROID_KEY_STORE_RSA_ALIAS = "flowcrypt_main";
  private static final String ANDROID_KEY_STORE_AES_ALIAS = "flowcrypt_main_aes";

  private static KeyStoreCryptoManager ourInstance;

  private KeyStore keyStore;
  private PrivateKey privateKey;
  private PublicKey publicKey;
  private SecretKey secretKey;

  private final Object decryptLock = new Object();

  private boolean isOldLogicUsed;

  /**
   * This constructor does initialization of symmetric (AES) and asymmetric keys (RSA).
   *
   * @throws Exception Initialization can throw exceptions.
   */
  private KeyStoreCryptoManager(Context context) throws Exception {
    keyStore = KeyStore.getInstance(PROVIDER_ANDROID_KEY_STORE);
    keyStore.load(null);

    isOldLogicUsed = keyStore.containsAlias(ANDROID_KEY_STORE_RSA_ALIAS);

    if (context == null) {
      throw new NullPointerException("The context is null!");
    }

    Context appContext = context.getApplicationContext();
    setup(appContext);
  }

  public static void init(Context context) {
    KeyStoreCryptoManager.getInstance(context);
  }

  public static KeyStoreCryptoManager getInstance(Context context) {
    if (ourInstance == null) {
      synchronized (KeyStoreCryptoManager.class) {
        if (ourInstance == null) {
          try {
            ourInstance = new KeyStoreCryptoManager(context);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    }
    return ourInstance;
  }

  /**
   * Generate a random 16 byte size String for algorithm parameter spec.
   *
   * @return <tt>String</tt> Return a generated String.
   */
  public static String generateAlgorithmParameterSpecString() {
    return UUID.randomUUID().toString().substring(0, SIZE_OF_ALGORITHM_PARAMETER_SPEC);
  }

  /**
   * Normalize an input String to return only 16 bytes for algorithm parameter spec.
   *
   * @param rawString The input String which must be equals or longer then {@link
   *                  KeyStoreCryptoManager#SIZE_OF_ALGORITHM_PARAMETER_SPEC}
   * @return <tt>String</tt> Return a normalized String.
   */
  public static String normalizeAlgorithmParameterSpecString(String rawString) {
    if (!TextUtils.isEmpty(rawString) && rawString.length() >= SIZE_OF_ALGORITHM_PARAMETER_SPEC) {
      return rawString.substring(0, SIZE_OF_ALGORITHM_PARAMETER_SPEC);
    } else {
      throw new IllegalArgumentException("The rawString must be equals or longer then " +
          SIZE_OF_ALGORITHM_PARAMETER_SPEC + " bytes");
    }
  }

  /**
   * This method encrypts an input text via AES symmetric algorithm and returns encrypted data.
   *
   * @param plainData                    The input text which will be encrypted.
   * @param algorithmParameterSpecString The algorithm parameter spec which will be used to randomize encryption.
   *                                     The size must be equal 16 byte. It only used for compatibility with the
   *                                     older versions.
   * @return <tt>String</tt> A base64 encoded encrypted result.
   * @throws Exception The encryption process can throw a lot of exceptions.
   */
  public String encrypt(String plainData, String algorithmParameterSpecString) throws Exception {
    if (!TextUtils.isEmpty(plainData)) {
      if (isOldLogicUsed) {
        if (TextUtils.isEmpty(algorithmParameterSpecString)) {
          throw new IllegalArgumentException("The algorithm parameter spec must not be null!");
        }

        if (algorithmParameterSpecString.length() != SIZE_OF_ALGORITHM_PARAMETER_SPEC) {
          throw new IllegalArgumentException("The algorithm parameter spec size must be equal "
              + SIZE_OF_ALGORITHM_PARAMETER_SPEC + " bytes!");
        }

        Cipher cipher = Cipher.getInstance(TRANSFORMATION_AES_CBC_PKCS5_PADDING);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(algorithmParameterSpecString.getBytes()));
        byte[] encryptedBytes = cipher.doFinal(plainData.getBytes(StandardCharsets.UTF_8));

        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
      } else {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION_AES_CBC_PKCS7_PADDING);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] iv = cipher.getIV();
        byte[] encryptedBytes = cipher.doFinal(plainData.getBytes(StandardCharsets.UTF_8));

        return Base64.encodeToString(iv, Base64.DEFAULT) + "\n" + Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
      }
    } else return plainData;
  }

  /**
   * This method decrypts the input encrypted text via AES symmetric algorithm and returns decrypted data.
   *
   * @param encryptedData                The input encrypted text, which must be encrypted and encoded in base64.
   * @param algorithmParameterSpecString The algorithm parameter spec which will be used to randomize encryption.
   *                                     The size must be equal 16 byte. It only used for compatibility with the
   *                                     older versions.
   * @return <tt>String</tt> Return decrypted data.
   * @throws Exception The decryption process can throw a lot of exceptions.
   */
  public String decrypt(String encryptedData, String algorithmParameterSpecString) throws Exception {
    if (!TextUtils.isEmpty(encryptedData)) {
      if (isOldLogicUsed) {
        if (TextUtils.isEmpty(algorithmParameterSpecString)) {
          throw new IllegalArgumentException("The algorithm parameter spec must not be null!");
        }

        if (algorithmParameterSpecString.length() != SIZE_OF_ALGORITHM_PARAMETER_SPEC) {
          throw new IllegalArgumentException("The algorithm parameter spec size must be equal "
              + SIZE_OF_ALGORITHM_PARAMETER_SPEC + " bytes!");
        }

        Cipher cipher = Cipher.getInstance(TRANSFORMATION_AES_CBC_PKCS5_PADDING);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(algorithmParameterSpecString.getBytes()));
        byte[] decodedBytes = cipher.doFinal(Base64.decode(encryptedData, Base64.DEFAULT));
        return new String(decodedBytes, StandardCharsets.UTF_8);
      } else {
        int splitPosition = encryptedData.indexOf('\n');

        if (splitPosition == -1) {
          throw new IllegalArgumentException("wrong encryptedData");
        }

        String iv = encryptedData.substring(0, splitPosition);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION_AES_CBC_PKCS7_PADDING);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(Base64.decode(iv, Base64.DEFAULT)));
        byte[] decodedBytes = cipher.doFinal(Base64.decode(encryptedData.substring(splitPosition + 1), Base64.DEFAULT));
        return new String(decodedBytes, StandardCharsets.UTF_8);
      }
    } else return encryptedData;
  }

  /**
   * This method encrypts an input text and returns encrypted data.
   * It uses RSA for the older versions (which have such support) or AES.
   *
   * @param plainData The input text which will be encrypted.
   * @return <tt>String</tt> A base64 encoded encrypted result.
   * @throws Exception The encryption process can throw a lot of exceptions.
   */
  public String encryptWithRSAOrAES(String plainData) throws Exception {
    if (!TextUtils.isEmpty(plainData)) {
      if (isOldLogicUsed) {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION_TYPE_RSA_ECB_PKCS1_PADDING);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] plainDataBytes = plainData.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedBytes = cipher.doFinal(plainDataBytes);

        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
      } else {
        return encrypt(plainData, null);
      }
    } else return plainData;
  }

  /**
   * This method decrypts an input encrypted text and returns decrypted data.
   * It uses RSA for the older versions (which have such support) or AES.
   *
   * @param context       Interface to global information about an application environment;
   * @param encryptedData - The input encrypted text, which must be encrypted and encoded in base64.
   * @return <tt>String</tt> Return decrypt
   * @throws Exception The decryption process can throw a lot of exceptions.
   */
  public String decryptWithRSAOrAES(Context context, String encryptedData) throws Exception {
    if (!TextUtils.isEmpty(encryptedData)) {
      if (isOldLogicUsed) {
        synchronized (decryptLock) {
          Cipher cipher = Cipher.getInstance(TRANSFORMATION_TYPE_RSA_ECB_PKCS1_PADDING);
          cipher.init(Cipher.DECRYPT_MODE, privateKey);

          byte[] encryptedBytes = Base64.decode(encryptedData, Base64.DEFAULT);
          byte[] decryptedBytes;
          try {
            decryptedBytes = cipher.doFinal(encryptedBytes);
          } catch (BadPaddingException | RuntimeException e) {
            e.printStackTrace();

            String runtimeMsg = "error:04000044:RSA routines:OPENSSL_internal:internal error";
            if (e instanceof RuntimeException && runtimeMsg.equals(e.getMessage())) {
              context.sendBroadcast(new Intent(context, CorruptedStorageBroadcastReceiver.class));
              throw new RuntimeException("Storage was corrupted", e);
            }

            String badPaddingMsg = "error:0407109F:rsa routines:RSA_padding_check_PKCS1_type_2:pkcs decoding error";
            if (e instanceof BadPaddingException && badPaddingMsg.equals(e.getMessage())) {
              context.sendBroadcast(new Intent(context, CorruptedStorageBroadcastReceiver.class));
              throw new GeneralSecurityException("Storage was corrupted", e);
            }

            throw e;
          }

          return new String(decryptedBytes, StandardCharsets.UTF_8);
        }
      } else {
        return decrypt(encryptedData, null);
      }
    } else return encryptedData;
  }

  private void setup(Context context) throws Exception {
    if (isOldLogicUsed) {
      initRSAKeys();
      initAESSecretKey(context);
    } else {
      initAESSecretKey(context);
    }
  }

  /**
   * Do initialization of RSA keys.
   *
   * @throws Exception The initialization can throw a lot of exceptions.
   */
  private void initRSAKeys() throws Exception {
    try {
      this.privateKey = (PrivateKey) keyStore.getKey(ANDROID_KEY_STORE_RSA_ALIAS, null);
    } catch (UnrecoverableKeyException e) {
      e.printStackTrace();
      throw new ManualHandledException("Your device is currently not supported: KeystoreService not available.");
    }

    if (privateKey != null) {
      Certificate certificate = keyStore.getCertificate(ANDROID_KEY_STORE_RSA_ALIAS);
      if (certificate == null) {
        throw new ManualHandledException("Your device is currently not supported: KeystoreService not available.");
      }
      this.publicKey = certificate.getPublicKey();
    }
  }

  /**
   * Do initialization of AES {@link SecretKey} object.
   *
   * @param context Interface to global information about an application environment;
   * @throws Exception The initialization can throw a lot of exceptions.
   */
  private void initAESSecretKey(Context context) throws Exception {
    if (isOldLogicUsed) {
      initAESSecretKeyFromSharedPreferences(context);
    } else {
      if (!keyStore.containsAlias(ANDROID_KEY_STORE_AES_ALIAS)) {
        genAESSecretKey();
      }

      try {
        this.secretKey = (SecretKey) keyStore.getKey(ANDROID_KEY_STORE_AES_ALIAS, null);
      } catch (UnrecoverableKeyException e) {
        e.printStackTrace();
        throw new ManualHandledException("Your device is currently not supported: KeystoreService not available.");
      }
    }
  }

  /**
   * Do initialization of AES {@link SecretKey} from the shared preferences.
   *
   * @param context Interface to global information about an application environment;
   * @throws Exception The initialization can throw a lot of exceptions.
   */
  private void initAESSecretKeyFromSharedPreferences(Context context) throws Exception {
    String encryptedSecretKey = getSecretKeyFromSharedPreferences(context);
    String decryptedSecretKey = decryptWithRSAOrAES(context, encryptedSecretKey);

    secretKey = new SecretKeySpec(Base64.decode(decryptedSecretKey, Base64.DEFAULT), KeyProperties.KEY_ALGORITHM_AES);
  }

  /**
   * Get an encrypted secret key from SharedPreferences.
   *
   * @param context Interface to global information about an application environment;
   * @return <tt>{@link String}</tt> An encrypted secret key or null if it not found.
   */
  private String getSecretKeyFromSharedPreferences(Context context) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    return sharedPreferences.getString(PREFERENCE_KEY_SECRET, null);
  }

  /**
   * Generate {@link SecretKey} using AndroidKeyStore for the AES symmetric algorithm.
   */
  private void genAESSecretKey() throws NoSuchAlgorithmException, NoSuchProviderException,
      InvalidAlgorithmParameterException, ManualHandledException {

    KeyGenerator keyPairGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,
        PROVIDER_ANDROID_KEY_STORE);

    keyPairGenerator.init(new KeyGenParameterSpec.Builder(ANDROID_KEY_STORE_AES_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
        .build());

    try {
      keyPairGenerator.generateKey();
    } catch (ProviderException e) {
      e.printStackTrace();
      throw new ManualHandledException("Your device is currently not supported: KeystoreService not available.");
    }
  }
}
