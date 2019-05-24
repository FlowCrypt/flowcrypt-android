/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node;

import com.flowcrypt.email.api.retrofit.response.node.BaseNodeResponse;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import androidx.annotation.NonNull;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Converter;

/**
 * This class will be used by {@link NodeConverterFactory} for creating responses.
 * <p>
 * Every response body will have the next structure: "json\nbytes":
 * <ul>
 * <li>UTF8-encoded request JSON metadata before the first LF (ASCII code 10)
 * <li>binary data afterwards until the end of stream
 * </ul>
 *
 * @author Denis Bondarenko
 * Date: 1/10/19
 * Time: 5:21 PM
 * E-mail: DenBond7@gmail.com
 */
public final class NodeResponseBodyConverter<T> implements Converter<ResponseBody, T> {
  private final Gson gson;
  private final TypeAdapter<T> adapter;

  NodeResponseBodyConverter(Gson gson, TypeAdapter<T> adapter) {
    this.gson = gson;
    this.adapter = adapter;
  }

  @Override
  public T convert(@NonNull ResponseBody value) throws IOException {
    try (BufferedInputStream bufferedInputStream = new BufferedInputStream(value.source().inputStream());
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
      int c;

      //find UTF8-encoded request JSON metadata
      while ((c = bufferedInputStream.read()) != -1) {
        if (c == '\n') {
          break;
        }
        bufferedOutputStream.write((byte) c);
      }

      bufferedOutputStream.flush();

      JsonReader jsonReader = gson.newJsonReader(new StringReader(outputStream.toString(getCharset(value).name())));

      try {
        T result = adapter.read(jsonReader);
        if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
          throw new JsonIOException("JSON document was not fully consumed.");
        }

        if (result instanceof BaseNodeResponse) {
          BaseNodeResponse baseNodeResponse = (BaseNodeResponse) result;
          baseNodeResponse.handleRawData(bufferedInputStream);
        }

        return result;
      } finally {
        value.close();
      }
    }
  }

  private void forceFirstData(BufferedInputStream bufferedInputStream) throws IOException {
    bufferedInputStream.mark(0);
    int b = bufferedInputStream.read();
    if (b != -1) {
      bufferedInputStream.reset();
    }
  }

  private Charset getCharset(ResponseBody responseBody) {
    MediaType contentType = responseBody.contentType();
    return contentType != null ? contentType.charset(StandardCharsets.UTF_8) : StandardCharsets.UTF_8;
  }
}
