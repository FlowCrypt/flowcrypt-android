package com.flowcrypt.email.security;

import android.content.Context;
import android.os.Build;
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
import java.util.Calendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;

/**
 * This class use Android Keystore System for encrypt/decrypt an information.
 *
 * @author DenBond7
 *         Date: 12.05.2017
 *         Time: 12:29
 *         E-mail: DenBond7@gmail.com
 */

public class KeyStoreCryptoManager {
    private static final String TRANSFORMATION_TYPE_RSA_ECB_OAEPWITH_SHA_256_AND_MGF1_PADDING =
            "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String KEY_ALGORITHM_RSA = "RSA";
    private static final String PROVIDER_ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String ALIAS = "flowcrypt_main";

    private final PrivateKey privateKey;
    private PublicKey publicKey;
    private Context context;
    private KeyStore keyStore;

    /**
     * This constructor do initialization of KeyStore which will be used to generates/gets keys.
     *
     * @param context Interface to global information about an application environment. Need to
     *                use an application context.
     * @throws Exception Initialization can throw exceptions.
     */
    public KeyStoreCryptoManager(Context context) throws Exception {
        this.context = context;
        this.keyStore = KeyStore.getInstance(PROVIDER_ANDROID_KEY_STORE);
        keyStore.load(null);

        if (!keyStore.containsAlias(ALIAS)) {
            createKeyInAndroidKeyStore();
        }

        this.privateKey = (PrivateKey) keyStore.getKey(ALIAS, null);
        if (keyStore != null && privateKey != null) {
            this.publicKey = keyStore.getCertificate(ALIAS).getPublicKey();
        }
    }

    /**
     * This method do encrypt of the input text and return an encrypted data.
     * <p>
     * For encrypt will be created a Cipher object with transformation
     * {@link KeyStoreCryptoManager#TRANSFORMATION_TYPE_RSA_ECB_OAEPWITH_SHA_256_AND_MGF1_PADDING
     * } and initialized as {@link Cipher#ENCRYPT_MODE} with a public key. Then the plainData
     * which will be as
     * input will be convert to byte[] and encrypt via cipher.doFinal. After this we will return
     * a base64 encoded encrypted result.
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
    public String encrypt(String plainData) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException, IOException {
        if (!TextUtils.isEmpty(plainData)) {
            Cipher cipher = Cipher.getInstance
                    (TRANSFORMATION_TYPE_RSA_ECB_OAEPWITH_SHA_256_AND_MGF1_PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            byte[] plainDataBytes = plainData.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedBytes = cipher.doFinal(plainDataBytes);

            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } else return plainData;
    }

    /**
     * This method do decrypt of the input encrypted text and return a decrypted data.
     * <p>
     * For decrypt will be created a Cipher object with transformation
     * {@link KeyStoreCryptoManager#TRANSFORMATION_TYPE_RSA_ECB_OAEPWITH_SHA_256_AND_MGF1_PADDING
     * } and initialized as {@link Cipher#DECRYPT_MODE} with a private key. Then the
     * encryptedData which will be as input will be decode to byte[] and decrypt via cipher
     * .doFinal. After this we will return a decrypted result.
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
    public String decrypt(String encryptedData) throws InvalidKeyException,
            NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException,
            IllegalBlockSizeException, IOException {
        if (!TextUtils.isEmpty(encryptedData)) {
            Cipher cipher = Cipher.getInstance
                    (TRANSFORMATION_TYPE_RSA_ECB_OAEPWITH_SHA_256_AND_MGF1_PADDING);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            byte[] encryptedBytes = Base64.decode(encryptedData, Base64.DEFAULT);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } else return encryptedData;
    }

    /**
     * Create KeyPair for alias {@link KeyStoreCryptoManager#ALIAS}.
     *
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     */
    private void createKeyInAndroidKeyStore() throws NoSuchProviderException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException {
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
    private KeyPair generateKeyIfAndroidVersionEqualOrHigherThenMarshmallow() throws
            NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA, PROVIDER_ANDROID_KEY_STORE);

        keyPairGenerator.initialize(
                new KeyGenParameterSpec.Builder(ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
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
                        .setAlias(ALIAS)
                        .setSubject(new X500Principal("CN=" + context.getString(R.string
                                .app_name) + ", OU=cryptup.org, O=Android Authority, C=US"))
                        .setStartDate(startDate.getTime())
                        .setEndDate(endDate.getTime())
                        .setSerialNumber(BigInteger.valueOf(startDate.getTimeInMillis()))
                        .build();

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM_RSA,
                PROVIDER_ANDROID_KEY_STORE);
        keyPairGenerator.initialize(keyPairGeneratorSpec);

        return keyPairGenerator.generateKeyPair();
    }
}
