/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.node.RequestsManager
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.api.retrofit.response.node.BaseNodeResponse
import com.flowcrypt.email.api.retrofit.response.node.DecryptedFileResult
import com.flowcrypt.email.api.retrofit.response.node.EncryptedFileResult
import com.flowcrypt.email.api.retrofit.response.node.EncryptedMsgResult
import com.flowcrypt.email.api.retrofit.response.node.NodeResponseWrapper
import com.flowcrypt.email.api.retrofit.response.node.ParseDecryptedMsgResult
import com.flowcrypt.email.api.retrofit.response.node.VersionResult
import com.flowcrypt.email.node.Node
import com.flowcrypt.email.node.TestData
import java.util.*

class NodeTestActivity : AppCompatActivity(), View.OnClickListener, Observer<NodeResponseWrapper<*>> {

  private var resultText = ""
  private var tvResult: TextView? = null
  private var hasTestFailure: Boolean = false
  private var encryptedMsg: String? = null
  private var encryptBytes: ByteArray? = null
  private val payloads = arrayOf(TestData.payload(1), TestData.payload(3), TestData.payload(5))
  private var allTestsStartTime: Long = 0
  private var requestsManager: RequestsManager? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initViews()
    val node = Node.getInstance(application)
    requestsManager = node.requestsManager
    requestsManager!!.getData()!!.observe(this, this)
  }

  public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_CHOOSE_FILE -> when (resultCode) {
        Activity.RESULT_OK -> if (data != null) {
          requestsManager!!.encryptFile(R.id.req_id_encrypt_file_from_uri, applicationContext, data.data!!)
        }
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.btnVersion -> requestsManager!!.getVersion(R.id.req_id_get_version)

      R.id.btnAllTests -> {
        allTestsStartTime = System.currentTimeMillis()
        runAllTests()
      }

      R.id.btnChooseFile -> {
        resultText = ""
        chooseFile()
      }
    }
  }

  override fun onChanged(responseWrapper: NodeResponseWrapper<*>?) {
    if (responseWrapper != null) {
      if (responseWrapper.exception != null) {
        addResultLine("FAIL", 0, "exception: " + responseWrapper.exception?.message, true)
        return
      }

      if (responseWrapper.result == null) {
        addResultLine("FAIL", 0, "result == null ", true)
        return
      }

      if (responseWrapper.result?.error != null) {
        addResultLine("ERROR", responseWrapper.executionTime,
            "error: " + responseWrapper.result?.error!!, true)
        return
      }

      when (responseWrapper.requestCode) {
        R.id.req_id_get_version -> {
          val testNodeResult = responseWrapper.result as VersionResult?
          resultText = testNodeResult!!.toString() + "\n\n"
          addResultLine("version", responseWrapper)
        }

        R.id.req_id_encrypt_msg -> {
          val encryptMsgResult = responseWrapper.result as EncryptedMsgResult?
          addResultLine("encrypt-msg", responseWrapper)
          encryptedMsg = encryptMsgResult!!.encryptedMsg
          requestsManager!!.decryptMsg(R.id.req_id_decrypt_msg_ecc, encryptedMsg!!.toByteArray(), TestData
              .eccPrvKeyInfo())
        }

        R.id.req_id_decrypt_msg_ecc -> {
          val eccDecryptMsgResult = responseWrapper.result as ParseDecryptedMsgResult?
          printDecryptMsgResult("decrypt-msg-ecc", eccDecryptMsgResult!!, responseWrapper.executionTime)
          requestsManager!!.decryptMsg(R.id.req_id_decrypt_msg_rsa_2048, encryptedMsg!!.toByteArray(), TestData
              .rsa2048PrvKeyInfo())
        }

        R.id.req_id_decrypt_msg_rsa_2048 -> {
          val rsa2048DecryptMsgResult = responseWrapper.result as ParseDecryptedMsgResult?
          printDecryptMsgResult("decrypt-msg-rsa2048", rsa2048DecryptMsgResult!!, responseWrapper.executionTime)
          requestsManager!!.decryptMsg(R.id.req_id_decrypt_msg_rsa_4096, encryptedMsg!!.toByteArray(), TestData
              .rsa4096PrvKeyInfo())
        }

        R.id.req_id_decrypt_msg_rsa_4096 -> {
          val rsa4096DecryptMsgResult = responseWrapper.result as ParseDecryptedMsgResult?
          printDecryptMsgResult("decrypt-rawMimeBytes-rsa4096", rsa4096DecryptMsgResult!!, responseWrapper.executionTime)
          requestsManager!!.encryptFile(R.id.req_id_encrypt_file, TEST_MSG.toByteArray())
        }

        R.id.req_id_encrypt_file -> {
          val encryptFileResult = responseWrapper.result as EncryptedFileResult?
          addResultLine("encrypt-file", encryptFileResult, responseWrapper.executionTime)
          encryptBytes = encryptFileResult!!.encryptBytes
          requestsManager!!.decryptFile(R.id.req_id_decrypt_file_ecc, encryptBytes!!, TestData.mixedPrvKeys)
        }

        R.id.req_id_decrypt_file_ecc -> {
          val eccDecryptedFileResult = responseWrapper.result as DecryptedFileResult?
          printDecryptFileResult("decrypt-file-ecc", TEST_MSG.toByteArray(), eccDecryptedFileResult!!,
              responseWrapper.executionTime)
          requestsManager!!.decryptFile(R.id.req_id_decrypt_file_rsa_2048, encryptBytes!!, TestData.rsa2048PrvKeyInfo())
        }

        R.id.req_id_decrypt_file_rsa_2048 -> {
          val rsa2048DecryptedFileResult = responseWrapper.result as DecryptedFileResult?
          printDecryptFileResult("decrypt-file-rsa2048", TEST_MSG.toByteArray(), rsa2048DecryptedFileResult!!,
              responseWrapper.executionTime)
          requestsManager!!.decryptFile(R.id.req_id_decrypt_file_rsa_4096, encryptBytes!!, TestData.rsa4096PrvKeyInfo())
        }

        R.id.req_id_decrypt_file_rsa_4096 -> {
          val rsa4096DecryptedFileResult = responseWrapper.result as DecryptedFileResult?
          printDecryptFileResult("decrypt-file-rsa4096", TEST_MSG.toByteArray(), rsa4096DecryptedFileResult!!,
              responseWrapper.executionTime)
          requestsManager!!.encryptFile(R.id.req_id_encrypt_file_rsa_2048_1mb, payloads[0])
        }

        R.id.req_id_encrypt_file_rsa_2048_1mb -> {
          val encryptedFileResult1Mb = responseWrapper.result as EncryptedFileResult?
          addResultLine("encrypt-file-" + 1 + "m" + "-rsa2048", encryptedFileResult1Mb,
              responseWrapper.executionTime)
          requestsManager!!.decryptFile(R.id.req_id_decrypt_file_rsa_2048_1mb, encryptedFileResult1Mb!!.encryptBytes!!,
              TestData.rsa2048PrvKeyInfo())
        }

        R.id.req_id_decrypt_file_rsa_2048_1mb -> {
          val rsa2048DecryptedFileResult1Mb = responseWrapper.result as DecryptedFileResult?
          printDecryptFileResult("decrypt-file-" + 1 + "m" + "-rsa2048", payloads[0], rsa2048DecryptedFileResult1Mb!!,
              responseWrapper.executionTime)
          requestsManager!!.encryptFile(R.id.req_id_encrypt_file_rsa_2048_3mb, payloads[1])
        }

        R.id.req_id_encrypt_file_rsa_2048_3mb -> {
          val encryptedFileResult3Mb = responseWrapper.result as EncryptedFileResult?
          addResultLine("encrypt-file-" + 3 + "m" + "-rsa2048", encryptedFileResult3Mb,
              responseWrapper.executionTime)
          requestsManager!!.decryptFile(R.id.req_id_decrypt_file_rsa_2048_3mb, encryptedFileResult3Mb!!
              .encryptBytes!!, TestData.eccPrvKeyInfo())
        }

        R.id.req_id_decrypt_file_rsa_2048_3mb -> {
          val rsa2048DecryptedFileResult3Mb = responseWrapper.result as DecryptedFileResult?
          printDecryptFileResult("decrypt-file-" + 3 + "m" + "-rsa2048", payloads[1], rsa2048DecryptedFileResult3Mb!!,
              responseWrapper.executionTime)
          requestsManager!!.encryptFile(R.id.req_id_encrypt_file_rsa_2048_5mb, payloads[2])
        }

        R.id.req_id_encrypt_file_rsa_2048_5mb -> {
          val encryptedFileResult5Mb = responseWrapper.result as EncryptedFileResult?
          addResultLine("encrypt-file-" + 5 + "m" + "-rsa2048", encryptedFileResult5Mb,
              responseWrapper.executionTime)
          requestsManager!!.decryptFile(R.id.req_id_decrypt_file_rsa_2048_5mb, encryptedFileResult5Mb!!
              .encryptBytes!!, TestData.eccPrvKeyInfo())
        }

        R.id.req_id_decrypt_file_rsa_2048_5mb -> {
          val rsa2048DecryptFileResult5Mb = responseWrapper.result as DecryptedFileResult?
          printDecryptFileResult("decrypt-file-" + 5 + "m" + "-rsa2048", payloads[2], rsa2048DecryptFileResult5Mb!!,
              responseWrapper.executionTime)

          if (!hasTestFailure) {
            addResultLine("all-tests", System.currentTimeMillis() - allTestsStartTime, "success", true)
          } else {
            addResultLine("all-tests", System.currentTimeMillis() - allTestsStartTime, "hasTestFailure", true)
          }
        }

        R.id.req_id_encrypt_file_from_uri -> {
          val encryptFileFromUriResult = responseWrapper.result as EncryptedFileResult?
          addResultLine("encrypt-file", encryptFileFromUriResult, responseWrapper.executionTime)
          requestsManager!!.decryptFile(R.id.req_id_decrypt_file_rsa_2048_from_uri, encryptFileFromUriResult!!
              .encryptBytes!!, TestData.rsa2048PrvKeyInfo())
        }

        R.id.req_id_decrypt_file_rsa_2048_from_uri -> {
          val rsa2048DecryptFileFromUriResult3Mb = responseWrapper.result as DecryptedFileResult?
          printDecryptFileResult("decrypt-file-rsa2048", null, rsa2048DecryptFileFromUriResult3Mb!!,
              responseWrapper.executionTime)
        }
      }
    }
  }

  private fun addResultLine(actionName: String, ms: Long, result: String?, isFinal: Boolean) {
    if (result == null || "ok" != result && "success" != result) {
      hasTestFailure = true
    }

    val line = (if (isFinal) "-----------------\n" else "") + actionName + " [" + ms + "ms] " +
        (if (hasTestFailure) "***FAIL*** " + result!! else result) + "\n"
    print(line)
    resultText += line
    tvResult!!.text = resultText
  }

  private fun addResultLine(actionName: String, ms: Long, e: Throwable) {
    addResultLine(actionName, ms, e.javaClass.name + ": " + e.message, false)
  }

  private fun addResultLine(actionName: String, result: NodeResponseWrapper<*>) {
    if (result.exception != null) {
      addResultLine(actionName, 0, result.exception!!)
    } else {
      addResultLine(actionName, result.executionTime, "ok", false)
    }
  }

  private fun addResultLine(actionName: String, result: BaseNodeResponse?, executionTime: Long) {
    if (result != null) {
      if (result.error != null) {
        addResultLine(actionName, executionTime, result.error!!.msg, false)
      } else {
        addResultLine(actionName, executionTime, "ok", false)
      }
    }
  }

  private fun printDecryptMsgResult(actionName: String, r: ParseDecryptedMsgResult, executionTime: Long) {
    if (r.error != null) {
      addResultLine(actionName, r, executionTime)
    } else if (Html.fromHtml(r.text).length != TEST_MSG_HTML.length) {
      addResultLine(actionName, executionTime,
          "wrong meta block len " + r.msgBlocks!!.size + "!=" + TEST_MSG_HTML.length, false)
    } else if (r.msgBlocks!![0].type !== MsgBlock.Type.PLAIN_HTML) {
      addResultLine(actionName, executionTime, "wrong meta block type: " + r.msgBlocks!![0].type,
          false)
    } else {
      val block = r.msgBlocks!![0]
      when {
        block.type !== MsgBlock.Type.PLAIN_HTML ->
          addResultLine(actionName, executionTime, "wrong block type: " + r.msgBlocks!!.size, false)
        Html.fromHtml(r.text).toString() != TEST_MSG_HTML -> addResultLine(actionName, executionTime, "block content mismatch",
            false)
        r.msgBlocks!!.size > 1 -> addResultLine(actionName, executionTime, "unexpected second block", false)
        else -> addResultLine(actionName, r, executionTime)
      }
    }
  }

  private fun printDecryptFileResult(actionName: String, originalData: ByteArray?, r: DecryptedFileResult,
                                     executionTime: Long) {
    if (r.error != null) {
      addResultLine(actionName, r, executionTime)
    } else if ("file.txt" != r.name) {
      addResultLine(actionName, executionTime, "wrong filename", false)
    } else if (originalData != null && !Arrays.equals(r.decryptedBytes, originalData)) {
      addResultLine(actionName, executionTime, "decrypted file content mismatch", false)
    } else {
      addResultLine(actionName, r, executionTime)
    }
  }

  private fun runAllTests() {
    resultText = ""
    hasTestFailure = false
    requestsManager!!.encryptMsg(R.id.req_id_encrypt_msg, TEST_MSG_HTML)
  }

  private fun chooseFile() {
    val intent = Intent()
    intent.action = Intent.ACTION_OPEN_DOCUMENT
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.type = "*/*"
    startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_file)), REQUEST_CODE_CHOOSE_FILE)
  }

  private fun initViews() {
    setContentView(R.layout.activity_main)
    tvResult = findViewById(R.id.tvResult)

    findViewById<View>(R.id.btnVersion).setOnClickListener(this)
    findViewById<View>(R.id.btnAllTests).setOnClickListener(this)
    findViewById<View>(R.id.btnChooseFile).setOnClickListener(this)
  }

  companion object {
    private const val REQUEST_CODE_CHOOSE_FILE = 10

    private const val TEST_MSG = "this is ~\na test for\n\ndecrypting\nunicode:\u03A3\nthat's all"
    private const val TEST_MSG_HTML =
        "this is ~<br>a test " + "for<br><br>decrypting<br>unicode:\u03A3<br>that&#39;s all"
  }
}
