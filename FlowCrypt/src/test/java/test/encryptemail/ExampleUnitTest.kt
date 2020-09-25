/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package test.encryptemail

import org.junit.Test
import java.io.File
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {
  @Test
  fun additionIsCorrect() {
    val keyBytes: ByteArray = File(this.javaClass.classLoader?.getResource("mock_web_server_private_key.der")?.path
        ?: "").readBytes()
    val spec = PKCS8EncodedKeySpec(keyBytes)
    val kf = KeyFactory.getInstance("RSA")
    val s = kf.generatePrivate(spec)
    print("Hello")

    val keyBytess: ByteArray = File(this.javaClass.classLoader?.getResource("mock_web_server_public_key.der")?.path
        ?: "").readBytes()

    val spec1 = X509EncodedKeySpec(keyBytess)
    val m = kf.generatePublic(spec1)

    print("Hello2")
  }
}
