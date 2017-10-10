/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Base64;

import com.flowcrypt.email.R;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

/**
 * This class use Android Keystore System for encrypt/decrypt information. Since encryption which uses the RSA has a
 * limit on the maximum size of the data that can be encrypted("The RSA algorithm can only encrypt data that has a
 * maximum byte length of the RSA key length in bits
 * divided with eight minus eleven padding bytes, i.e. number of maximum bytes = key length in bits / 8 - 11.", see
 * http://stackoverflow.com/questions/10007147/getting-a-illegalblocksizeexception-data-must-not-be-longer-than-256
 * -bytes-when), we use the following algorithm:
 * <ul>
 * <li>Generate a RSA key pair via {@code KeyPairGenerator.getInstance(
 * KeyProperties.KEY_ALGORITHM_RSA, PROVIDER_ANDROID_KEY_STORE)}</li>
 * <li>Generate a 128 bits symmetric key with use {@link SecureRandom}</li>
 * <li>Encrypt and save the symmetric key with the RSA key from Android Keystore System to the shared preferences</li>
 * <li>Encrypt the data with the decrypted symmetric key</li>
 * <li>Decrypt the data with the decrypted symmetric key</li>
 * </ul>
 *
 * @author DenBond7
 *         Date: 12.05.2017
 *         Time: 12:29
 *         E-mail: DenBond7@gmail.com
 */

public class KeyStoreCryptoManager {
    public static final int SIZE_OF_ALGORITHM_PARAMETER_SPEC = 16;
    public static final String PREFERENCE_KEY_SECRET = "preference_key_secret";
    private static final String TRANSFORMATION_TYPE_RSA_ECB_PKCS1Padding = "RSA/ECB/PKCS1Padding";
    private static final String TRANSFORMATION_AES_CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding";
    private static final String ALGORITHM_RSA = "RSA";
    private static final String ALGORITHM_SHA1PRNG = "SHA1PRNG";
    private static final String ALGORITHM_AES = "AES";
    private static final String PROVIDER_ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String ANDROID_KEY_STORE_RSA_ALIAS = "flowcrypt_main";
    private static final int KEY_SIZE_128 = 128;

    private Context context;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private SecretKeySpec secretKeySpec;

    /**
     * This constructor do initialization of symmetric (AES) and asymmetric keys (RSA).
     *
     * @param context Interface to global information about an application environment. Need to
     *                use an application context.
     * @throws Exception Initialization can throw exceptions.
     */
    public KeyStoreCryptoManager(Context context) throws Exception {
        if (context != null) {
            this.context = context.getApplicationContext();
        } else {
            throw new IllegalArgumentException("The context can not be null!");
        }
        initRsaKeyPair();
        initAesSecretKeySpec();
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
        } else
            throw new IllegalArgumentException("The rawString must be equals or longer then " +
                    SIZE_OF_ALGORITHM_PARAMETER_SPEC + " bytes");
    }

    /**
     * This method does encrypt via AES symmetric algorithm the input text and returns an
     * encrypted data.
     * <p>
     * For encrypt will be created a Cipher object with transformation
     * {@link KeyStoreCryptoManager#TRANSFORMATION_AES_CBC_PKCS5_PADDING} and initialized as
     * {@link Cipher#ENCRYPT_MODE} with the SecretKeySpec (AES key) and an algorithm parameter spec. Then the
     * plainData which will be as input will be convert to byte[] and encrypt via cipher.doFinal. After this we will
     * return a base64 encoded encrypted result.
     *
     * @param plainData                    The input text which will be encrypted.
     * @param algorithmParameterSpecString The algorithm parameter spec which will be used to randomize encryption.
     *                                     The size must be equal 16 byte
     * @return <tt>String</tt> A base64 encoded encrypted result.
     * @throws Exception The encryption process can throw a lot of exceptions.
     */
    public String encrypt(String plainData, String algorithmParameterSpecString) throws Exception {
        if (TextUtils.isEmpty(algorithmParameterSpecString)) {
            throw new IllegalArgumentException("The algorithm parameter spec must not be null!");
        }

        if (algorithmParameterSpecString.length() != SIZE_OF_ALGORITHM_PARAMETER_SPEC) {
            throw new IllegalArgumentException("The algorithm parameter spec size must be equal "
                    + SIZE_OF_ALGORITHM_PARAMETER_SPEC + " bytes!");
        }

        if (!TextUtils.isEmpty(plainData)) {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION_AES_CBC_PKCS5_PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec
                    (algorithmParameterSpecString.getBytes()));
            byte[] encryptedBytes = cipher.doFinal(plainData.getBytes(StandardCharsets.UTF_8));

            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } else return plainData;
    }

    /**
     * This method does decrypt via AES symmetric algorithm of the input encrypted text return a
     * decrypted data.
     * <p>
     * will be created a Cipher object with transformation
     * {@link KeyStoreCryptoManager#TRANSFORMATION_AES_CBC_PKCS5_PADDING} and initialized as
     * {@link Cipher#ENCRYPT_MODE} with the SecretKeySpec (AES key) and an algorithm parameter spec. Then the
     * encryptedData which will be as input will be decode to byte[] and decrypt via cipher.doFinal. After this we
     * will return a decrypted result.
     *
     * @param encryptedData                The input encrypted text, which must be encrypted and encoded in base64.
     * @param algorithmParameterSpecString The algorithm parameter spec which will be used to randomize encryption.
     *                                     The size must be equal 16 byte.
     * @return <tt>String</tt> Return a decrypted data.
     * @throws Exception The encryption process can throw a lot of exceptions.
     */
    public String decrypt(String encryptedData, String algorithmParameterSpecString) throws Exception {
        if (TextUtils.isEmpty(algorithmParameterSpecString)) {
            throw new IllegalArgumentException("The algorithm parameter spec must not be null!");
        }

        if (algorithmParameterSpecString.length() != SIZE_OF_ALGORITHM_PARAMETER_SPEC) {
            throw new IllegalArgumentException("The algorithm parameter spec size must be equal "
                    + SIZE_OF_ALGORITHM_PARAMETER_SPEC + " bytes!");
        }

        if (!TextUtils.isEmpty(encryptedData)) {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION_AES_CBC_PKCS5_PADDING);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec
                    (algorithmParameterSpecString.getBytes()));
            byte[] decodedBytes = cipher.doFinal(Base64.decode(encryptedData, Base64.DEFAULT));
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } else return encryptedData;
    }

    /**
     * This method does encrypt of the input text and returns an encrypted data.
     * <p>
     * For encrypt will be created a Cipher object with transformation
     * {@link KeyStoreCryptoManager#TRANSFORMATION_TYPE_RSA_ECB_PKCS1Padding} and initialized as
     * {@link Cipher#ENCRYPT_MODE} with a public key. Then the plainData which will be as input will be convert to
     * byte[] and encryptWithRSA via cipher.doFinal. After this we will return a base64 encoded encrypted result.
     *
     * @param plainData The input text which will be encrypted.
     * @return <tt>String</tt> A base64 encoded encrypted result.
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws IOException
     */
    public String encryptWithRSA(String plainData) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException, IOException {
        if (!TextUtils.isEmpty(plainData)) {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION_TYPE_RSA_ECB_PKCS1Padding);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            byte[] plainDataBytes = plainData.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedBytes = cipher.doFinal(plainDataBytes);

            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } else return plainData;
    }

    /**
     * This method does decrypt of the input encrypted text and return a decrypted data.
     * <p>
     * For decrypt will be created a Cipher object with transformation
     * {@link KeyStoreCryptoManager#TRANSFORMATION_TYPE_RSA_ECB_PKCS1Padding} and initialized as
     * {@link Cipher#DECRYPT_MODE} with a private key. Then the encryptedData which will be as input will be decode
     * to byte[] and decrypt via cipher.doFinal. After this we will return a decrypted result.
     *
     * @param encryptedData - The input encrypted text, which must be encrypted and encoded in
     *                      base64.
     * @return <tt>String</tt> Return a decrypted data.
     * @throws InvalidKeyException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws IOException
     */
    public String decryptWithRSA(String encryptedData) throws InvalidKeyException, NoSuchPaddingException,
            NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, IOException {
        if (!TextUtils.isEmpty(encryptedData)) {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION_TYPE_RSA_ECB_PKCS1Padding);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            byte[] encryptedBytes = Base64.decode(encryptedData, Base64.DEFAULT);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } else return encryptedData;
    }

    /**
     * Do initialization of an AES SecretKeySpec object.
     *
     * @throws Exception The initialization can throw a lot of exceptions.
     */
    private void initAesSecretKeySpec() throws Exception {
        String encryptedSecretKey = getSecretKeyFromSharedPreferences();
        if (getSecretKeyFromSharedPreferences() == null) {
            encryptedSecretKey = generateEncodedSecretKey();
            saveSecretKeyToSharedPreferences(encryptedSecretKey);
        }

        String decryptedSecretKey = decryptWithRSA(encryptedSecretKey);

        secretKeySpec = new SecretKeySpec(Base64.decode(decryptedSecretKey, Base64.DEFAULT), ALGORITHM_AES);
    }

    /**
     * Do initialization of a Rsa KeyPair object.
     *
     * @throws Exception The initialization can throw a lot of exceptions.
     */
    private void initRsaKeyPair() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(PROVIDER_ANDROID_KEY_STORE);
        keyStore.load(null);

        if (!keyStore.containsAlias(ANDROID_KEY_STORE_RSA_ALIAS)) {
            createRSAKeyPair();
        }

        this.privateKey = (PrivateKey) keyStore.getKey(ANDROID_KEY_STORE_RSA_ALIAS, null);
        if (privateKey != null) {
            this.publicKey = keyStore.getCertificate(ANDROID_KEY_STORE_RSA_ALIAS).getPublicKey();
        }
    }

    /**
     * Create KeyPair for alias {@link KeyStoreCryptoManager#ANDROID_KEY_STORE_RSA_ALIAS}.
     *
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     */
    private void createRSAKeyPair() throws NoSuchProviderException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, NoSuchPaddingException,
            IllegalBlockSizeException, IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            generateKeyIfAndroidVersionEqualOrHigherThenMarshmallow();
        } else {
            generateKeyIfAndroidVersionLessThenMarshmallow();
        }
    }

    /**
     * Generate a KeyPair for Android version equal or higher then {@link Build.VERSION_CODES#M}.
     *
     * @return <tt>{@link KeyPair}</tt> Generated KeyPair object with a private key.
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private KeyPair generateKeyIfAndroidVersionEqualOrHigherThenMarshmallow() throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidAlgorithmParameterException {

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA, PROVIDER_ANDROID_KEY_STORE);

        keyPairGenerator.initialize(
                new KeyGenParameterSpec.Builder(ANDROID_KEY_STORE_RSA_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                        .build());

        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Generate a KeyPair for Android version less then {@link Build.VERSION_CODES#M}.
     *
     * @return tt>{@link KeyPair}</tt> Generated KeyPair object with a private key.
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     */
    @SuppressWarnings("deprecation")
    private KeyPair generateKeyIfAndroidVersionLessThenMarshmallow() throws
            NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {

        Calendar startDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();
        endDate.add(Calendar.YEAR, 25);

        KeyPairGeneratorSpec keyPairGeneratorSpec =
                new KeyPairGeneratorSpec.Builder(context)
                        .setAlias(ANDROID_KEY_STORE_RSA_ALIAS)
                        .setSubject(new X500Principal("CN=" + context.getString(R.string
                                .app_name) + ", OU=cryptup.org, O=Android Authority, C=US"))
                        .setStartDate(startDate.getTime())
                        .setEndDate(endDate.getTime())
                        .setSerialNumber(BigInteger.valueOf(startDate.getTimeInMillis()))
                        .build();

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM_RSA,
                PROVIDER_ANDROID_KEY_STORE);
        keyPairGenerator.initialize(keyPairGeneratorSpec);

        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Get an encrypted secret key from SharedPreferences.
     *
     * @return <tt>{@link String}</tt> An encrypted secret key or null if it not found.
     */
    private String getSecretKeyFromSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREFERENCE_KEY_SECRET, null);
    }

    /**
     * Save an encrypted secret key to SharedPreferences
     *
     * @param newSecretKey A new secret key, which must be encrypted.
     * @return <tt>{@link Boolean}</tt> true if saving operation success, false otherwise..
     */
    private boolean saveSecretKeyToSharedPreferences(String newSecretKey) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putString(PREFERENCE_KEY_SECRET, newSecretKey);
        return edit.commit();
    }

    /**
     * Generate an encrypted secret key for the AES symmetric algorithm.
     *
     * @return <tt>{@link String}</tt> An encrypted secret key with the RSA asymmetric algorithm.
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws NoSuchPaddingException
     * @throws IOException
     */
    private String generateEncodedSecretKey() throws NoSuchProviderException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, InvalidKeyException,
            BadPaddingException, NoSuchPaddingException, IOException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM_AES);
        SecureRandom secureRandom = SecureRandom.getInstance(ALGORITHM_SHA1PRNG);
        keyGenerator.init(KEY_SIZE_128, secureRandom);
        SecretKey secretKey = keyGenerator.generateKey();

        String originalSecretKey = Base64.encodeToString(secretKey.getEncoded(), Base64.DEFAULT);
        return encryptWithRSA(originalSecretKey);
    }
}
