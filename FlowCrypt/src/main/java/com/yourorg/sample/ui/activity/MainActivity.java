package com.yourorg.sample.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.yourorg.sample.TestData;
import com.yourorg.sample.api.retrofit.request.RequestsManager;
import com.yourorg.sample.api.retrofit.response.base.NodeResponse;
import com.yourorg.sample.node.Node;
import com.yourorg.sample.node.results.DecryptFileResult;
import com.yourorg.sample.node.results.DecryptMsgResult;
import com.yourorg.sample.node.results.EncryptFileResult;
import com.yourorg.sample.node.results.EncryptMsgResult;
import com.yourorg.sample.node.results.MsgBlock;
import com.yourorg.sample.node.results.RawNodeResult;
import com.yourorg.sample.node.results.VersionResult;

import java.util.Arrays;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, Observer<NodeResponse> {
  private static final int REQUEST_CODE_CHOOSE_FILE = 10;

  private static final String TEST_MSG = "this is ~\na test for\n\ndecrypting\nunicode:\u03A3\nthat's all";
  private static final String TEST_MSG_HTML = "this is ~<br>a test for<br><br>decrypting<br>unicode:\u03A3<br>that&#39;s all";

  private String resultText = "";
  private TextView tvResult;
  private boolean hasTestFailure;
  private String encryptedMsg;
  private byte[] encryptedFileBytes;
  private byte[][] payloads = new byte[][]{TestData.payload(1), TestData.payload(3), TestData.payload(5)};
  private long allTestsStartTime;
  private RequestsManager requestsManager;

  public MainActivity() {
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
  public void onChanged(@Nullable NodeResponse nodeResponse) {
    if (nodeResponse != null) {
      switch (nodeResponse.getRequestCode()) {
        case R.id.req_id_get_version:
          VersionResult testNodeResult = (VersionResult) nodeResponse.getRawNodeResult();
          resultText = testNodeResult.debugGetRawJson() + "\n\n";
          addResultLine("version", nodeResponse.getRawNodeResult());
          break;

        case R.id.req_id_encrypt_msg:
          EncryptMsgResult encryptMsgResult = (EncryptMsgResult) nodeResponse.getRawNodeResult();
          addResultLine("encrypt-msg", encryptMsgResult);
          encryptedMsg = encryptMsgResult.getEncryptedString();
          requestsManager.decryptMsg(R.id.req_id_decrypt_msg_ecc, TestData.eccPrvKeyInfo(), encryptedMsg);
          break;

        case R.id.req_id_decrypt_msg_ecc:
          DecryptMsgResult eccDecryptMsgResult = (DecryptMsgResult) nodeResponse.getRawNodeResult();
          printDecryptMsgResult("decrypt-msg-ecc", eccDecryptMsgResult);
          requestsManager.decryptMsg(R.id.req_id_decrypt_msg_rsa_2048, TestData.rsa2048PrvKeyInfo(), encryptedMsg);
          break;

        case R.id.req_id_decrypt_msg_rsa_2048:
          DecryptMsgResult rsa2048DecryptMsgResult = (DecryptMsgResult) nodeResponse.getRawNodeResult();
          printDecryptMsgResult("decrypt-msg-rsa2048", rsa2048DecryptMsgResult);
          requestsManager.decryptMsg(R.id.req_id_decrypt_msg_rsa_4096, TestData.rsa4096PrvKeyInfo(), encryptedMsg);
          break;

        case R.id.req_id_decrypt_msg_rsa_4096:
          DecryptMsgResult rsa4096DecryptMsgResult = (DecryptMsgResult) nodeResponse.getRawNodeResult();
          printDecryptMsgResult("decrypt-msg-rsa4096", rsa4096DecryptMsgResult);
          requestsManager.encryptFile(R.id.req_id_encrypt_file, encryptedMsg.getBytes());
          break;

        case R.id.req_id_encrypt_file:
          EncryptFileResult encryptFileResult = (EncryptFileResult) nodeResponse.getRawNodeResult();
          addResultLine("encrypt-file", encryptFileResult);
          encryptedFileBytes = encryptFileResult.getEncryptedDataBytes();
          requestsManager.decryptFile(R.id.req_id_decrypt_file_ecc, encryptedFileBytes, TestData.eccPrvKeyInfo());
          break;

        case R.id.req_id_decrypt_file_ecc:
          DecryptFileResult eccDecryptFileResult = (DecryptFileResult) nodeResponse.getRawNodeResult();
          printDecryptFileResult("decrypt-file-ecc", encryptedMsg.getBytes(), eccDecryptFileResult);
          requestsManager.decryptFile(R.id.req_id_decrypt_file_rsa_2048, encryptedFileBytes, TestData.rsa2048PrvKeyInfo());
          break;

        case R.id.req_id_decrypt_file_rsa_2048:
          DecryptFileResult rsa2048DecryptFileResult = (DecryptFileResult) nodeResponse.getRawNodeResult();
          printDecryptFileResult("decrypt-file-rsa2048", encryptedMsg.getBytes(), rsa2048DecryptFileResult);
          requestsManager.decryptFile(R.id.req_id_decrypt_file_rsa_4096, encryptedFileBytes, TestData.rsa4096PrvKeyInfo());
          break;

        case R.id.req_id_decrypt_file_rsa_4096:
          DecryptFileResult rsa4096DecryptFileResult = (DecryptFileResult) nodeResponse.getRawNodeResult();
          printDecryptFileResult("decrypt-file-rsa4096", encryptedMsg.getBytes(), rsa4096DecryptFileResult);
          requestsManager.encryptFile(R.id.req_id_encrypt_file_rsa_2048_1mb, payloads[0]);
          break;

        case R.id.req_id_encrypt_file_rsa_2048_1mb:
          EncryptFileResult encryptFileResult1Mb = (EncryptFileResult) nodeResponse.getRawNodeResult();
          addResultLine("encrypt-file-" + 1 + "m" + "-rsa2048", encryptFileResult1Mb);
          requestsManager.decryptFile(R.id.req_id_decrypt_file_rsa_2048_1mb, encryptFileResult1Mb.getEncryptedDataBytes(), TestData.eccPrvKeyInfo());
          break;

        case R.id.req_id_decrypt_file_rsa_2048_1mb:
          DecryptFileResult rsa2048DecryptFileResult1Mb = (DecryptFileResult) nodeResponse.getRawNodeResult();
          printDecryptFileResult("decrypt-file-" + 1 + "m" + "-rsa2048", payloads[0], rsa2048DecryptFileResult1Mb);
          requestsManager.encryptFile(R.id.req_id_encrypt_file_rsa_2048_3mb, payloads[1]);
          break;

        case R.id.req_id_encrypt_file_rsa_2048_3mb:
          EncryptFileResult encryptFileResult3Mb = (EncryptFileResult) nodeResponse.getRawNodeResult();
          addResultLine("encrypt-file-" + 3 + "m" + "-rsa2048", encryptFileResult3Mb);
          requestsManager.decryptFile(R.id.req_id_decrypt_file_rsa_2048_3mb, encryptFileResult3Mb.getEncryptedDataBytes(), TestData.eccPrvKeyInfo());
          break;

        case R.id.req_id_decrypt_file_rsa_2048_3mb:
          DecryptFileResult rsa2048DecryptFileResult3Mb = (DecryptFileResult) nodeResponse.getRawNodeResult();
          printDecryptFileResult("decrypt-file-" + 3 + "m" + "-rsa2048", payloads[1], rsa2048DecryptFileResult3Mb);
          requestsManager.encryptFile(R.id.req_id_encrypt_file_rsa_2048_5mb, payloads[2]);
          break;

        case R.id.req_id_encrypt_file_rsa_2048_5mb:
          EncryptFileResult encryptFileResult5Mb = (EncryptFileResult) nodeResponse.getRawNodeResult();
          addResultLine("encrypt-file-" + 5 + "m" + "-rsa2048", encryptFileResult5Mb);
          requestsManager.decryptFile(R.id.req_id_decrypt_file_rsa_2048_5mb, encryptFileResult5Mb.getEncryptedDataBytes(), TestData.eccPrvKeyInfo());
          break;

        case R.id.req_id_decrypt_file_rsa_2048_5mb:
          DecryptFileResult rsa2048DecryptFileResult5Mb = (DecryptFileResult) nodeResponse.getRawNodeResult();
          printDecryptFileResult("decrypt-file-" + 5 + "m" + "-rsa2048", payloads[2], rsa2048DecryptFileResult5Mb);

          if (!hasTestFailure) {
            addResultLine("all-tests", System.currentTimeMillis() - allTestsStartTime, "success", true);
          } else {
            addResultLine("all-tests", System.currentTimeMillis() - allTestsStartTime, "hasTestFailure", true);
          }
          break;

        case R.id.req_id_encrypt_file_from_uri:
          EncryptFileResult encryptFileFromUriResult = (EncryptFileResult) nodeResponse.getRawNodeResult();
          addResultLine("encrypt-file", encryptFileFromUriResult);
          requestsManager.decryptFile(R.id.req_id_decrypt_file_rsa_2048_from_uri, encryptFileFromUriResult.getEncryptedDataBytes(), TestData.rsa2048PrvKeyInfo());
          break;

        case R.id.req_id_decrypt_file_rsa_2048_from_uri:
          DecryptFileResult rsa2048DecryptFileFromUriResult3Mb = (DecryptFileResult) nodeResponse.getRawNodeResult();
          printDecryptFileResult("decrypt-file-rsa2048", null, rsa2048DecryptFileFromUriResult3Mb);
          break;
      }
    }
  }

  private void addResultLine(String actionName, long ms, String result, boolean isFinal) {
    if (!result.equals("ok") && !result.equals("success")) {
      hasTestFailure = true;
      result = "***FAIL*** " + result;
    }
    String line = (isFinal ? "-----------------\n" : "") + actionName + " [" + ms + "ms] " + result + "\n";
    System.out.print(line);
    resultText += line;
    tvResult.setText(resultText);
  }

  private void addResultLine(String actionName, long ms, Throwable e) {
    addResultLine(actionName, ms, e.getClass().getName() + ": " + e.getMessage(), false);
  }

  private void addResultLine(String actionName, RawNodeResult result) {
    if (result.getErr() != null) {
      addResultLine(actionName, result.ms, result.getErr());
    } else {
      addResultLine(actionName, result.ms, "ok", false);
    }
  }

  private void printDecryptMsgResult(String actionName, DecryptMsgResult r) {
    if (r.getErr() != null) {
      addResultLine(actionName, r.ms, r.getErr());
    } else if (r.getDecryptErr() != null) {
      addResultLine(actionName, r.ms, r.getDecryptErr().type + ":" + r.getDecryptErr().error, false);
    } else if (r.getAllBlockMetas().length != 1) {
      addResultLine(actionName, r.ms, "wrong amount of block metas: " + r.getAllBlockMetas().length, false);
    } else if (r.getAllBlockMetas()[0].length != TEST_MSG_HTML.length()) {
      addResultLine(actionName, r.ms, "wrong meta block len " + r.getAllBlockMetas()[0].length + "!=" + TEST_MSG_HTML.length(), false);
    } else if (!r.getAllBlockMetas()[0].type.equals(MsgBlock.TYPE_HTML)) {
      addResultLine(actionName, r.ms, "wrong meta block type: " + r.getAllBlockMetas()[0].type, false);
    } else {
      MsgBlock block = r.getNextBlock();
      if (block == null) {
        addResultLine(actionName, r.ms, "getNextBlock unexpectedly null", false);
      } else if (!block.getType().equals(MsgBlock.TYPE_HTML)) {
        addResultLine(actionName, r.ms, "wrong block type: " + r.getAllBlockMetas()[0].length, false);
      } else if (!block.getContent().equals(TEST_MSG_HTML)) {
        addResultLine(actionName, r.ms, "block content mismatch", false);
      } else if (r.getNextBlock() != null) {
        addResultLine(actionName, r.ms, "unexpected second block", false);
      } else {
        addResultLine(actionName, r);
      }
    }
  }

  private void printDecryptFileResult(String actionName, byte[] originalData, DecryptFileResult r) {
    if (r.getErr() != null) {
      addResultLine(actionName, r.ms, r.getErr());
    } else if (r.getDecryptErr() != null) {
      addResultLine(actionName, r.ms, r.getDecryptErr().type + ":" + r.getDecryptErr().error, false);
    } else if (!"file.txt".equals(r.getName())) {
      addResultLine(actionName, r.ms, "wrong filename", false);
    } else if (originalData != null && !Arrays.equals(r.getDecryptedDataBytes(), originalData)) {
      addResultLine(actionName, r.ms, "decrypted file content mismatch", false);
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
