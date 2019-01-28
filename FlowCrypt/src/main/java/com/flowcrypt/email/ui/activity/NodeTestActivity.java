/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.retrofit.node.RequestsManager;
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock;
import com.flowcrypt.email.api.retrofit.response.node.BaseNodeResult;
import com.flowcrypt.email.api.retrofit.response.node.DecryptedFileResult;
import com.flowcrypt.email.api.retrofit.response.node.DecryptedMsgResult;
import com.flowcrypt.email.api.retrofit.response.node.EncryptedFileResult;
import com.flowcrypt.email.api.retrofit.response.node.EncryptedMsgResult;
import com.flowcrypt.email.api.retrofit.response.node.NodeResponseWrapper;
import com.flowcrypt.email.api.retrofit.response.node.VersionResult;
import com.flowcrypt.email.node.Node;
import com.flowcrypt.email.node.TestData;

import java.util.Arrays;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

public class NodeTestActivity extends AppCompatActivity implements View.OnClickListener, Observer<NodeResponseWrapper> {
  private static final int REQUEST_CODE_CHOOSE_FILE = 10;

  private static final String TEST_MSG = "this is ~\na test for\n\ndecrypting\nunicode:\u03A3\nthat's all";
  private static final String TEST_MSG_HTML = "this is ~<br>a test " +
      "for<br><br>decrypting<br>unicode:\u03A3<br>that&#39;s all";

  private String resultText = "";
  private TextView tvResult;
  private boolean hasTestFailure;
  private String encryptedMsg;
  private byte[] encryptedBytes;
  private byte[][] payloads = new byte[][]{TestData.payload(1), TestData.payload(3), TestData.payload(5)};
  private long allTestsStartTime;
  private RequestsManager requestsManager;

  public NodeTestActivity() {
    Node node = Node.getInstance();
    requestsManager = node.getRequestsManager();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initViews();
    requestsManager.getData().observe(this, this);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    switch (requestCode) {
      case REQUEST_CODE_CHOOSE_FILE:
        switch (resultCode) {
          case Activity.RESULT_OK:
            if (data != null) {
              requestsManager.encryptFile(R.id.req_id_encrypt_file_from_uri, getApplicationContext(), data.getData());
            }
            break;
        }
        break;

      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.btnVersion:
        requestsManager.getVersion(R.id.req_id_get_version);
        break;

      case R.id.btnAllTests:
        allTestsStartTime = System.currentTimeMillis();
        runAllTests();
        break;

      case R.id.btnChooseFile:
        resultText = "";
        chooseFile();
        break;
    }
  }

  @Override
  public void onChanged(@Nullable NodeResponseWrapper responseWrapper) {
    if (responseWrapper != null) {
      if (responseWrapper.getException() != null) {
        addResultLine("FAIL", 0, "exception: " + responseWrapper.getException().getMessage(), true);
        return;
      }

      if (responseWrapper.getResult() == null) {
        addResultLine("FAIL", 0, "result == null ", true);
        return;
      }

      if (responseWrapper.getResult().getError() != null) {
        addResultLine("ERROR", responseWrapper.getResult().getTime(),
            "error: " + responseWrapper.getResult().getError(), true);
        return;
      }

      switch (responseWrapper.getRequestCode()) {
        case R.id.req_id_get_version:
          VersionResult testNodeResult = (VersionResult) responseWrapper.getResult();
          resultText = testNodeResult + "\n\n";
          addResultLine("version", responseWrapper);
          break;

        case R.id.req_id_encrypt_msg:
          EncryptedMsgResult encryptMsgResult = (EncryptedMsgResult) responseWrapper.getResult();
          addResultLine("encrypt-msg", responseWrapper);
          encryptedMsg = encryptMsgResult.getEncryptedMsg();
          requestsManager.decryptMsg(R.id.req_id_decrypt_msg_ecc, encryptedMsg, TestData.eccPrvKeyInfo());
          break;

        case R.id.req_id_decrypt_msg_ecc:
          DecryptedMsgResult eccDecryptMsgResult = (DecryptedMsgResult) responseWrapper.getResult();
          printDecryptMsgResult("decrypt-msg-ecc", eccDecryptMsgResult);
          requestsManager.decryptMsg(R.id.req_id_decrypt_msg_rsa_2048, encryptedMsg, TestData.rsa2048PrvKeyInfo());
          break;

        case R.id.req_id_decrypt_msg_rsa_2048:
          DecryptedMsgResult rsa2048DecryptMsgResult = (DecryptedMsgResult) responseWrapper.getResult();
          printDecryptMsgResult("decrypt-msg-rsa2048", rsa2048DecryptMsgResult);
          requestsManager.decryptMsg(R.id.req_id_decrypt_msg_rsa_4096, encryptedMsg, TestData.rsa4096PrvKeyInfo());
          break;

        case R.id.req_id_decrypt_msg_rsa_4096:
          DecryptedMsgResult rsa4096DecryptMsgResult = (DecryptedMsgResult) responseWrapper.getResult();
          printDecryptMsgResult("decrypt-msg-rsa4096", rsa4096DecryptMsgResult);
          requestsManager.encryptFile(R.id.req_id_encrypt_file, TEST_MSG.getBytes());
          break;

        case R.id.req_id_encrypt_file:
          EncryptedFileResult encryptFileResult = (EncryptedFileResult) responseWrapper.getResult();
          addResultLine("encrypt-file", encryptFileResult);
          encryptedBytes = encryptFileResult.getEncryptedBytes();
          requestsManager.decryptFile(R.id.req_id_decrypt_file_ecc, encryptedBytes, TestData.getMixedPrvKeys());
          break;

        case R.id.req_id_decrypt_file_ecc:
          DecryptedFileResult eccDecryptedFileResult = (DecryptedFileResult) responseWrapper.getResult();
          printDecryptFileResult("decrypt-file-ecc", TEST_MSG.getBytes(), eccDecryptedFileResult);
          requestsManager.decryptFile(R.id.req_id_decrypt_file_rsa_2048, encryptedBytes, TestData.rsa2048PrvKeyInfo());
          break;

        case R.id.req_id_decrypt_file_rsa_2048:
          DecryptedFileResult rsa2048DecryptedFileResult = (DecryptedFileResult) responseWrapper.getResult();
          printDecryptFileResult("decrypt-file-rsa2048", TEST_MSG.getBytes(), rsa2048DecryptedFileResult);
          requestsManager.decryptFile(R.id.req_id_decrypt_file_rsa_4096, encryptedBytes, TestData.rsa4096PrvKeyInfo());
          break;

        case R.id.req_id_decrypt_file_rsa_4096:
          DecryptedFileResult rsa4096DecryptedFileResult = (DecryptedFileResult) responseWrapper.getResult();
          printDecryptFileResult("decrypt-file-rsa4096", TEST_MSG.getBytes(), rsa4096DecryptedFileResult);
          requestsManager.encryptFile(R.id.req_id_encrypt_file_rsa_2048_1mb, payloads[0]);
          break;

        case R.id.req_id_encrypt_file_rsa_2048_1mb:
          EncryptedFileResult encryptedFileResult1Mb = (EncryptedFileResult) responseWrapper.getResult();
          addResultLine("encrypt-file-" + 1 + "m" + "-rsa2048", encryptedFileResult1Mb);
          requestsManager.decryptFile(R.id.req_id_decrypt_file_rsa_2048_1mb, encryptedFileResult1Mb.getEncryptedBytes(),
              TestData.rsa2048PrvKeyInfo());
          break;

        case R.id.req_id_decrypt_file_rsa_2048_1mb:
          DecryptedFileResult rsa2048DecryptedFileResult1Mb = (DecryptedFileResult) responseWrapper.getResult();
          printDecryptFileResult("decrypt-file-" + 1 + "m" + "-rsa2048", payloads[0], rsa2048DecryptedFileResult1Mb);
          requestsManager.encryptFile(R.id.req_id_encrypt_file_rsa_2048_3mb, payloads[1]);
          break;

        case R.id.req_id_encrypt_file_rsa_2048_3mb:
          EncryptedFileResult encryptedFileResult3Mb = (EncryptedFileResult) responseWrapper.getResult();
          addResultLine("encrypt-file-" + 3 + "m" + "-rsa2048", encryptedFileResult3Mb);
          requestsManager.decryptFile(R.id.req_id_decrypt_file_rsa_2048_3mb, encryptedFileResult3Mb
              .getEncryptedBytes(), TestData.eccPrvKeyInfo());
          break;

        case R.id.req_id_decrypt_file_rsa_2048_3mb:
          DecryptedFileResult rsa2048DecryptedFileResult3Mb = (DecryptedFileResult) responseWrapper.getResult();
          printDecryptFileResult("decrypt-file-" + 3 + "m" + "-rsa2048", payloads[1], rsa2048DecryptedFileResult3Mb);
          requestsManager.encryptFile(R.id.req_id_encrypt_file_rsa_2048_5mb, payloads[2]);
          break;

        case R.id.req_id_encrypt_file_rsa_2048_5mb:
          EncryptedFileResult encryptedFileResult5Mb = (EncryptedFileResult) responseWrapper.getResult();
          addResultLine("encrypt-file-" + 5 + "m" + "-rsa2048", encryptedFileResult5Mb);
          requestsManager.decryptFile(R.id.req_id_decrypt_file_rsa_2048_5mb, encryptedFileResult5Mb
              .getEncryptedBytes(), TestData.eccPrvKeyInfo());
          break;

        case R.id.req_id_decrypt_file_rsa_2048_5mb:
          DecryptedFileResult rsa2048DecryptFileResult5Mb = (DecryptedFileResult) responseWrapper.getResult();
          printDecryptFileResult("decrypt-file-" + 5 + "m" + "-rsa2048", payloads[2], rsa2048DecryptFileResult5Mb);

          if (!hasTestFailure) {
            addResultLine("all-tests", System.currentTimeMillis() - allTestsStartTime, "success", true);
          } else {
            addResultLine("all-tests", System.currentTimeMillis() - allTestsStartTime, "hasTestFailure", true);
          }
          break;

        case R.id.req_id_encrypt_file_from_uri:
          EncryptedFileResult encryptFileFromUriResult = (EncryptedFileResult) responseWrapper.getResult();
          addResultLine("encrypt-file", encryptFileFromUriResult);
          requestsManager.decryptFile(R.id.req_id_decrypt_file_rsa_2048_from_uri, encryptFileFromUriResult
              .getEncryptedBytes(), TestData.rsa2048PrvKeyInfo());
          break;

        case R.id.req_id_decrypt_file_rsa_2048_from_uri:
          DecryptedFileResult rsa2048DecryptFileFromUriResult3Mb = (DecryptedFileResult) responseWrapper.getResult();
          printDecryptFileResult("decrypt-file-rsa2048", null, rsa2048DecryptFileFromUriResult3Mb);
          break;
      }
    }
  }

  private void addResultLine(String actionName, long ms, String result, boolean isFinal) {
    if (result == null || !"ok".equals(result) && !"success".equals(result)) {
      hasTestFailure = true;
    }

    String line = (isFinal ? "-----------------\n" : "") + actionName + " [" + ms + "ms] " +
        (hasTestFailure ? "***FAIL*** " + result : result) + "\n";
    System.out.print(line);
    resultText += line;
    tvResult.setText(resultText);
  }

  private void addResultLine(String actionName, long ms, Throwable e) {
    addResultLine(actionName, ms, e.getClass().getName() + ": " + e.getMessage(), false);
  }

  private void addResultLine(String actionName, NodeResponseWrapper result) {
    BaseNodeResult baseNodeResult = result.getResult();

    if (result.getException() != null) {
      addResultLine(actionName, 0, result.getException());
    } else {
      addResultLine(actionName, baseNodeResult.getTime(), "ok", false);
    }
  }

  private void addResultLine(String actionName, BaseNodeResult result) {
    if (result != null) {
      if (result.getError() != null) {
        addResultLine(actionName, result.getTime(), result.getError().getMsg(), false);
      } else {
        addResultLine(actionName, result.getTime(), "ok", false);
      }
    }
  }

  private void printDecryptMsgResult(String actionName, DecryptedMsgResult r) {
    if (r.getError() != null) {
      addResultLine(actionName, r);
    } else if (r.getBlockMetas().size() != 1) {
      addResultLine(actionName, r.getTime(), "wrong amount of block metas: " + r.getBlockMetas().size(), false);
    } else if (r.getMsgBlocks().get(0).getContent().length() != TEST_MSG_HTML.length()) {
      addResultLine(actionName, r.getTime(),
          "wrong meta block len " + r.getBlockMetas().get(0).getLength() + "!=" + TEST_MSG_HTML.length(), false);
    } else if (!r.getBlockMetas().get(0).getType().equals(MsgBlock.TYPE_HTML)) {
      addResultLine(actionName, r.getTime(), "wrong meta block type: " + r.getBlockMetas().get(0).getType(), false);
    } else {
      MsgBlock block = r.getMsgBlocks().get(0);
      if (block == null) {
        addResultLine(actionName, r.getTime(), "getNextBlock unexpectedly null", false);
      } else if (!block.getType().equals(MsgBlock.TYPE_HTML)) {
        addResultLine(actionName, r.getTime(), "wrong block type: " + r.getBlockMetas().get(0).getLength(), false);
      } else if (!block.getContent().equals(TEST_MSG_HTML)) {
        addResultLine(actionName, r.getTime(), "block content mismatch", false);
      } else if (r.getMsgBlocks().size() > 1) {
        addResultLine(actionName, r.getTime(), "unexpected second block", false);
      } else {
        addResultLine(actionName, r);
      }
    }
  }

  private void printDecryptFileResult(String actionName, byte[] originalData, DecryptedFileResult r) {
    if (r.getError() != null) {
      addResultLine(actionName, r);
    } else if (!"file.txt".equals(r.getName())) {
      addResultLine(actionName, r.getTime(), "wrong filename", false);
    } else if (originalData != null && !Arrays.equals(r.getDecryptedBytes(), originalData)) {
      addResultLine(actionName, r.getTime(), "decrypted file content mismatch", false);
    } else {
      addResultLine(actionName, r);
    }
  }

  private void runAllTests() {
    resultText = "";
    hasTestFailure = false;
    requestsManager.encryptMsg(R.id.req_id_encrypt_msg, TEST_MSG);
  }

  private void chooseFile() {
    Intent intent = new Intent();
    intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("*/*");
    startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_file)), REQUEST_CODE_CHOOSE_FILE);
  }

  private void initViews() {
    setContentView(R.layout.activity_main);
    tvResult = findViewById(R.id.tvResult);

    findViewById(R.id.btnVersion).setOnClickListener(this);
    findViewById(R.id.btnAllTests).setOnClickListener(this);
    findViewById(R.id.btnChooseFile).setOnClickListener(this);
  }
}
