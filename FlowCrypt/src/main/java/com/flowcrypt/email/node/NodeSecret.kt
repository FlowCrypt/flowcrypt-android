/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.node

import android.util.Base64
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.util.io.pem.PemWriter
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.StringWriter
import java.math.BigInteger
import java.net.ServerSocket
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Security
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509CRL
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * This class describes a logic where we create some security things for communication between the Node.js server and
 * the app.
 */
// todo - one of the main class which should be reviewed
class NodeSecret @JvmOverloads internal constructor(
  writablePath: String,
  nodeSecretCertsCache: NodeSecretCerts? = null
) {

  val port: Int
  var ca: String? = null
    private set
  var key: String? = null
    private set
  var crt: String? = null
    private set
  var authPwd: String? = null
    private set
  lateinit var authHeader: String
    private set
  val unixSocketFilePath: String

  private val secureRandom: SecureRandom
  private var issuer: X500Name? = null
  private var caCrt: X509Certificate? = null
  private var srvCrt: X509Certificate? = null
  private var srvKey: PrivateKey? = null
  lateinit var sslSocketFactory: SSLSocketFactory
    private set
  lateinit var sslTrustManager: X509TrustManager
    private set
  var sslCrtSerialNumber: BigInteger? = null
    private set

  val cache: NodeSecretCerts
    get() = NodeSecretCerts.fromNodeSecret(this)

  init {
    val ss = ServerSocket(0)
    port = ss.localPort
    ss.close()

    unixSocketFilePath = "$writablePath/flowcrypt-node.sock" // potentially usefull in the future
    secureRandom = SecureRandom()
    initCerts(nodeSecretCertsCache)
    genAuthPwdAndHeader()
    createSslAttributes()
  }

  private fun initCerts(nodeSecretCertsCache: NodeSecretCerts?) {
    if (nodeSecretCertsCache != null) {
      ca = nodeSecretCertsCache.ca
      crt = nodeSecretCertsCache.crt
      key = nodeSecretCertsCache.key
      caCrt = parseCert(ca)
      srvCrt = parseCert(crt)
      srvKey = parseKey(key)
    } else {
      genCerts()
      ca = toString(caCrt)
      crt = toString(srvCrt)
      key = toString(srvKey)
    }
  }

  private fun createSslAttributes() {
    try {
      // create trust manager that trusts ca to verify server crt
      val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
      tmf.init(newKeyStore("ca", caCrt, null)) // trust our ca
      // create key manager to supply client key and crt (client and server use the same keypair)
      val clientKmFactory = KeyManagerFactory.getInstance("X509")
      clientKmFactory.init(newKeyStore("crt", srvCrt, srvKey), null) // slow
      // new sslContext for http client that trusts the ca and provides client cert
      val sslContext = SSLContext.getInstance("TLS")
      val tms = tmf.trustManagers
      if (tms.size != 1 || tms[0] !is X509TrustManager) {
        throw IllegalStateException("Unexpected default trust managers:" + Arrays.toString(tms))
      }
      sslContext.init(clientKmFactory.keyManagers, tms, secureRandom) // slow
      sslSocketFactory = sslContext.socketFactory
      sslTrustManager = tms[0] as X509TrustManager
      sslCrtSerialNumber = srvCrt!!.serialNumber
    } catch (e: Exception) {
      throw RuntimeException("failed to create ssl attributes for node", e)
    }
  }

  private fun parseCert(certString: String?): X509Certificate {
    ByteArrayInputStream(certString!!.toByteArray()).use { inputStream ->
      return CertificateFactory.getInstance("X.509")
        .generateCertificate(inputStream) as X509Certificate
    }
  }

  private fun parseKey(keyString: String?): PrivateKey {
    ByteArrayInputStream(keyString!!.toByteArray()).use { inputStream ->
      BufferedReader(InputStreamReader(inputStream)).use { reader ->
        PEMParser(reader).use { pemParser ->
          val o = pemParser.readObject()

          if (o is PEMKeyPair) {
            val converter = JcaPEMKeyConverter()
            return converter.getPrivateKey(o.privateKeyInfo)
          } else
            throw IllegalArgumentException("The given string doesn't contain a valid private key")
        }
      }
    }
  }

  private fun genCerts() {
    issuer = X500Name("CN=CA Cert")

    // new rsa key generator
    val keyGen = KeyPairGenerator.getInstance("RSA")
    keyGen.initialize(2048, secureRandom)

    // new self-signed ca keypair and crt
    val caKeypair = keyGen.generateKeyPair()
    val caKeyUsage = KeyUsage.digitalSignature or KeyUsage.keyCertSign
    caCrt = newSignedCrt(caKeypair, caKeypair, issuer!!, caKeyUsage)

    // new ca-signed srv crt and key (also used for client)
    val srvKeypair = keyGen.generateKeyPair()
    val srvKeyUsage = KeyUsage.digitalSignature or KeyUsage.keyEncipherment or
        KeyUsage.dataEncipherment or KeyUsage.keyAgreement
    srvCrt = newSignedCrt(caKeypair, srvKeypair, X500Name(CRT_SUBJECT), srvKeyUsage)
    srvKey = srvKeypair.private
  }

  private fun genAuthPwdAndHeader() {
    this.authPwd = genPwd()
    this.authHeader = "Basic " + String(Base64.encode(this.authPwd!!.toByteArray(), Base64.NO_WRAP))
  }

  private fun newKeyStore(alias: String, crt: Certificate?, prv: PrivateKey?): KeyStore {
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
    keyStore.load(null, null)
    keyStore.setCertificateEntry(alias, crt) // todo - possible fail point
    if (prv != null) { // todo - most likely current fail point is here or line above for client certs
      keyStore.setKeyEntry(alias, prv, null, arrayOf(crt!!))
    }
    return keyStore
  }

  private fun newSignedCrt(
    issuerKeyPair: KeyPair,
    subjectKeyPair: KeyPair,
    subject: X500Name,
    keyUsage: Int
  )
      : X509Certificate {
    val calendar = Calendar.getInstance()
    val from = calendar.time
    calendar.add(Calendar.YEAR, 25)
    val to = calendar.time

    val info = SubjectPublicKeyInfo.getInstance(subjectKeyPair.public.encoded)
    val serial = BigInteger.valueOf(System.currentTimeMillis())

    val subjectCertBuilder =
      X509v3CertificateBuilder(issuer, serial, from, to, Locale.US, subject, info)
    subjectCertBuilder.addExtension(Extension.keyUsage, true, KeyUsage(keyUsage))
    val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption").build(issuerKeyPair.private)
    val crtHolder = subjectCertBuilder.build(signer)

    ByteArrayInputStream(crtHolder.encoded).use { inputStream ->
      if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
        Security.addProvider(BouncyCastleProvider()) // takes about 150ms and is not always needed
      }
      val cf = CertificateFactory.getInstance("X.509")
      return cf.generateCertificate(inputStream) as X509Certificate
    }
  }

  /**
   * Make PEM formatted string representation of the given object.
   *
   * @param o The given object. It can be only [X509Certificate], [X509CRL], [KeyPair],
   * [PrivateKey], [PublicKey]
   * @return PEM formatted string
   * @throws IOException Such errors can occur during the creation of a string.
   */
  private fun toString(o: Any?): String {
    StringWriter().use { stringWriter ->
      PemWriter(stringWriter).use { pemWriter ->
        val generator = JcaMiscPEMGenerator(o)
        pemWriter.writeObject(generator)
        pemWriter.flush()
        return stringWriter.toString()
      }
    }
  }

  private fun genPwd(): String {
    val bytes = ByteArray(32)
    secureRandom.nextBytes(bytes)
    return String(Base64.encode(bytes, Base64.NO_WRAP))
  }

  companion object {
    const val HOSTNAME = "localhost"
    const val CRT_SUBJECT = "CN=localhost"
  }
}
