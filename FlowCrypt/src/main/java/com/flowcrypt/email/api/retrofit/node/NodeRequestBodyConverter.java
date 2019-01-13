package com.flowcrypt.email.api.retrofit.node;

import com.flowcrypt.email.api.retrofit.request.node.BaseNodeRequest;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import androidx.annotation.NonNull;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.ByteString;
import retrofit2.Converter;

/**
 * This class will be used by {@link NodeConverterFactory} for creating requests.
 *
 * @author Denis Bondarenko
 * Date: 1/10/19
 * Time: 5:24 PM
 * E-mail: DenBond7@gmail.com
 */
public final class NodeRequestBodyConverter<F> implements Converter<F, RequestBody> {
  private final Gson gson;
  private final TypeAdapter<F> adapter;

  NodeRequestBodyConverter(Gson gson, TypeAdapter<F> adapter) {
    this.gson = gson;
    this.adapter = adapter;
  }

  @Override
  public RequestBody convert(@NonNull F value) throws IOException {
    if (!(value instanceof BaseNodeRequest)) {
      throw new IllegalArgumentException("Support only classes that extend " + BaseNodeRequest.class.getSimpleName());
    }

    BaseNodeRequest baseNodeRequest = (BaseNodeRequest) value;

    Buffer buffer = new Buffer();
    Writer writer = new OutputStreamWriter(buffer.outputStream(), StandardCharsets.UTF_8);
    JsonWriter jsonWriter = gson.newJsonWriter(writer);
    adapter.write(jsonWriter, value);
    jsonWriter.close();
    ByteString json = buffer.readByteString();

    return new NodeRequestBody(baseNodeRequest.getEndpoint(), json, baseNodeRequest.getData());
  }
}