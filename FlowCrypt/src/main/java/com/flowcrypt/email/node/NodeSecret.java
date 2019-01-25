/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.node;

import android.util.Base64;

import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.Extension;
import org.spongycastle.asn1.x509.KeyUsage;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openssl.PEMKeyPair;
import org.spongycastle.openssl.PEMParser;
import org.spongycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.spongycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;
import org.spongycastle.util.io.pem.PemObjectGenerator;
import org.spongycastle.util.io.pem.PemWriter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * This class describes a logic where we create some security things for communication between the Node.js server and
 * the app.
 */
public class NodeSecret {
  public static final String HOSTNAME = "localhost";
  public static final String CRT_SUBJECT = "CN=localhost";

  private int port;
  private String ca;
  private String key;
  private String crt;
  private String authPwd;
  private String authHeader;
  private String unixSocketFilePath;

  private SecureRandom secureRandom;
  private X500Name issuer;
  private X509Certificate caCrt;
  private X509Certificate srvCrt;
  private PrivateKey srvKey;
  private SSLSocketFactory sslSocketFactory;
  private X509TrustManager sslTrustManager;
  private BigInteger sslCrtSerialNumber;

  NodeSecret(String writablePath) throws Exception {
    this(writablePath, null);
  }

  NodeSecret(String writablePath, NodeSecretCerts nodeSecretCertsCache) throws Exception {
    ServerSocket ss = new ServerSocket(0);
    port = ss.getLocalPort();
    ss.close();

    unixSocketFilePath = writablePath + "/flowcrypt-node.sock"; // potentially usefull in the future
    secureRandom = new SecureRandom();
    initCerts(nodeSecretCertsCache);
    genAuthPwdAndHeader();
    createSslAttributes();
  }

  public NodeSecretCerts getCache() {
    return NodeSecretCerts.fromNodeSecret(this);
  }

  public SSLSocketFactory getSslSocketFactory() {
    return sslSocketFactory;
  }

  public X509TrustManager getSslTrustManager() {
    return sslTrustManager;
  }

  public BigInteger getSslCrtSerialNumber() {
    return sslCrtSerialNumber;
  }

  public int getPort() {
    return port;
  }

  public String getCa() {
    return ca;
  }

  public String getKey() {
    return key;
  }

  public String getCrt() {
    return crt;
  }

  public String getAuthPwd() {
    return authPwd;
  }

  public String getAuthHeader() {
    return authHeader;
  }

  public String getUnixSocketFilePath() {
    return unixSocketFilePath;
  }

  private void initCerts(NodeSecretCerts nodeSecretCertsCache) throws Exception {
    if (nodeSecretCertsCache != null) {
      ca = nodeSecretCertsCache.getCa();
      crt = nodeSecretCertsCache.getCrt();
      key = nodeSecretCertsCache.getKey();
      caCrt = parseCert(ca);
      srvCrt = parseCert(crt);
      srvKey = parseKey(key);
    } else {
      genCerts();
      ca = toString(caCrt);
      crt = toString(srvCrt);
      key = toString(srvKey);
    }
  }

  private void createSslAttributes() {
    if (sslSocketFactory == null || sslTrustManager == null) {
      try {
        // create trust manager that trusts ca to verify server crt
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(newKeyStore("ca", caCrt, null)); // trust our ca
        // create key manager to supply client key and crt (client and server use the same keypair)
        KeyManagerFactory clientKmFactory = KeyManagerFactory.getInstance("X509");
        clientKmFactory.init(newKeyStore("crt", srvCrt, srvKey), null); // slow
        // new sslContext for http client that trusts the ca and provides client cert
        SSLContext sslContext = SSLContext.getInstance("TLS");
        TrustManager[] tms = tmf.getTrustManagers();
        if (tms.length != 1 || !(tms[0] instanceof X509TrustManager)) {
          throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(tms));
        }
        sslContext.init(clientKmFactory.getKeyManagers(), tms, secureRandom); // slow
        sslSocketFactory = sslContext.getSocketFactory();
        sslTrustManager = (X509TrustManager) tms[0];
        sslCrtSerialNumber = srvCrt.getSerialNumber();
      } catch (Exception e) {
        throw new RuntimeException("failed to create ssl attributes for node", e);
      }
    }
  }

  private X509Certificate parseCert(String certString) throws IOException, CertificateException {
    try (InputStream inputStream = new ByteArrayInputStream(certString.getBytes())) {
      return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(inputStream);
    }
  }

  private PrivateKey parseKey(String keyString) throws IOException {
    try (InputStream inputStream = new ByteArrayInputStream(keyString.getBytes());
         Reader reader = new BufferedReader(new InputStreamReader(inputStream));
         PEMParser pemParser = new PEMParser(reader)) {
      Object o = pemParser.readObject();

      if (o instanceof PEMKeyPair) {
        PEMKeyPair pemKeyPair = (PEMKeyPair) o;
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        return converter.getPrivateKey(pemKeyPair.getPrivateKeyInfo());
      } else throw new IllegalArgumentException("The given string doesn't contain a valid private key");
    }
  }

  private void genCerts() throws Exception {
    issuer = new X500Name("CN=CA Cert");

    // new rsa key generator
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048, secureRandom);

    // new self-signed ca keypair and crt
    KeyPair caKeypair = keyGen.generateKeyPair();
    int caKeyUsage = KeyUsage.digitalSignature | KeyUsage.keyCertSign;
    caCrt = newSignedCrt(caKeypair, caKeypair, issuer, caKeyUsage);

    // new ca-signed srv crt and key (also used for client)
    KeyPair srvKeypair = keyGen.generateKeyPair();
    int srvKeyUsage = KeyUsage.digitalSignature | KeyUsage.keyEncipherment |
        KeyUsage.dataEncipherment | KeyUsage.keyAgreement;
    srvCrt = newSignedCrt(caKeypair, srvKeypair, new X500Name(CRT_SUBJECT), srvKeyUsage);
    srvKey = srvKeypair.getPrivate();
  }

  private void genAuthPwdAndHeader() {
    this.authPwd = genPwd();
    this.authHeader = "Basic " + new String(Base64.encode(this.authPwd.getBytes(), Base64.NO_WRAP));
  }

  private KeyStore newKeyStore(String alias, Certificate crt, PrivateKey prv) throws Exception {
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    keyStore.load(null, null);
    keyStore.setCertificateEntry(alias, crt); // todo - possible fail point
    if (prv != null) { // todo - most likely current failpoint is here or line above for client certs
      keyStore.setKeyEntry(alias, prv, null, new Certificate[]{crt});
    }
    return keyStore;
  }

  private X509Certificate newSignedCrt(KeyPair issuerKeyPair, KeyPair subjectKeyPair, X500Name subject, int keyUsage)
      throws Exception {
    Calendar calendar = Calendar.getInstance();
    Date from = calendar.getTime();
    calendar.add(Calendar.YEAR, 25);
    Date to = calendar.getTime();

    SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(subjectKeyPair.getPublic().getEncoded());
    BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

    X509v3CertificateBuilder subjectCertBuilder = new X509v3CertificateBuilder(issuer, serial, from, to, subject, info);
    subjectCertBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(keyUsage));
    ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(issuerKeyPair.getPrivate());
    X509CertificateHolder crtHolder = subjectCertBuilder.build(signer);

    try (InputStream inputStream = new ByteArrayInputStream(crtHolder.getEncoded())) {
      if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
        Security.addProvider(new BouncyCastleProvider()); // takes about 150ms and is not always needed
      }
      CertificateFactory cf = CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
      return (X509Certificate) cf.generateCertificate(inputStream);
    }
  }

  /**
   * Make PEM formatted string representation of the given object.
   *
   * @param o The given object. It can be only {@link X509Certificate}, {@link X509CRL}, {@link KeyPair},
   *          {@link PrivateKey}, {@link PublicKey}
   * @return PEM formatted string
   * @throws IOException Such errors can occur during the creation of a string.
   */
  private String toString(Object o) throws IOException {
    try (StringWriter stringWriter = new StringWriter(); PemWriter pemWriter = new PemWriter(stringWriter)) {
      PemObjectGenerator generator = new JcaMiscPEMGenerator(o);
      pemWriter.writeObject(generator);
      pemWriter.flush();
      return stringWriter.toString();
    }
  }

  private String genPwd() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return new String(Base64.encode(bytes, Base64.NO_WRAP));
  }
}
