/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.node.gson.NodeGson;
import com.flowcrypt.email.api.retrofit.response.model.node.BaseMsgBlock;
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * It's a result for "parseDecryptMsg" requests.
 *
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 3:48 PM
 * E-mail: DenBond7@gmail.com
 */
public class ParseDecryptedMsgResult extends BaseNodeResult {

  public static final Creator<ParseDecryptedMsgResult> CREATOR = new Creator<ParseDecryptedMsgResult>() {
    @Override
    public ParseDecryptedMsgResult createFromParcel(Parcel source) {
      return new ParseDecryptedMsgResult(source);
    }

    @Override
    public ParseDecryptedMsgResult[] newArray(int size) {
      return new ParseDecryptedMsgResult[size];
    }
  };

  private List<MsgBlock> msgBlocks;

  public ParseDecryptedMsgResult() {
    this.msgBlocks = new ArrayList<>();
  }

  public ParseDecryptedMsgResult(Parcel in) {
    super(in);
    this.msgBlocks = in.createTypedArrayList(BaseMsgBlock.CREATOR);
  }

  @Override
  public void handleRawData(BufferedInputStream bufferedInputStream) throws IOException {
    boolean isEnabled = true;
    Gson gson = NodeGson.getGson();

    while (isEnabled) {
      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
           BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
        int c;

        //find the end of the next part of data
        while ((c = bufferedInputStream.read()) != -1) {
          if (c == '\n') {
            break;
          }
          bufferedOutputStream.write((byte) c);
        }

        bufferedOutputStream.flush();
        JsonReader jsonReader = gson.newJsonReader(new StringReader(outputStream.toString()));
        MsgBlock block = NodeGson.getGson().fromJson(jsonReader, MsgBlock.class);

        if (block != null) {
          msgBlocks.add(block);
        }

        if (c == -1) {
          isEnabled = false;
        }
      }
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeTypedList(this.msgBlocks);
  }

  public List<MsgBlock> getMsgBlocks() {
    return msgBlocks;
  }
}
