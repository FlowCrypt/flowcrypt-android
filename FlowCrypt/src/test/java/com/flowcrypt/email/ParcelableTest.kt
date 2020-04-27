/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.flextrade.jfixture.JFixture
import com.flowcrypt.email.api.retrofit.response.model.node.BaseMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.jfixture.MsgBlockGenerationCustomization
import com.flowcrypt.email.jfixture.SelectConstructorCustomisation
import io.github.classgraph.ClassGraph
import org.junit.Assert
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * @author Denis Bondarenko
 *         Date: 4/20/20
 *         Time: 7:31 PM
 *         E-mail: DenBond7@gmail.com
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.P])
class ParcelableTest(val name: String, private val currentClass: Class<Parcelable>) {

  private lateinit var objectInstance: Parcelable

  @Before
  fun setUp() {
    val fixture = JFixture()
    fixture.customise(SelectConstructorCustomisation(currentClass))
    fixture.customise(MsgBlockGenerationCustomization())
    fixture.customise().sameInstance(BaseMsgBlock::class.java, BaseMsgBlock(MsgBlock.Type.UNKNOWN, "someContent", false))
    objectInstance = currentClass.kotlin.objectInstance ?: fixture.create(currentClass)
  }

  @Test
  fun testParcelable() {
    val serializedBytes = Parcel.obtain().run {
      writeParcelable(objectInstance, 0)
      marshall()
    }

    assertNotNull(serializedBytes)

    val retainedParcelable = Parcel.obtain().run {
      unmarshall(serializedBytes, 0, serializedBytes.size)
      setDataPosition(0)
      readParcelable<Parcelable>(this::class.java.classLoader)
    }

    Assert.assertTrue("$name failed", objectInstance == retainedParcelable)
  }

  companion object {
    private val scanResult: List<Array<Any>> by lazy {
      val classLoader = Thread.currentThread().contextClassLoader
      ClassGraph()
          // Use contextClassLoader to avoid ClassCastExceptions
          .addClassLoader(classLoader)
          .enableAllInfo()
          .blacklistPackages("androidx", "android")
          .whitelistPackages("com.flowcrypt.email")
          .scan()
          .getClassesImplementing("android.os.Parcelable")
          .filter { (it.isInterface || it.isAbstract || it.typeSignature?.typeParameters?.size ?: 0 > 0).not() }
          .loadClasses()
          .map { arrayOf<Any>(it.name, it) }
    }

    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
    fun provideObjects(): List<Array<Any>> = scanResult
  }
}