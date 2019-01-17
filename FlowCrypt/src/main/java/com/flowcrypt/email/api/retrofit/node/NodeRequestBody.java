package com.flowcrypt.email.api.retrofit.node;

import com.flowcrypt.email.api.retrofit.request.node.NodeRequest;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.NonNull;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.ByteString;
import okio.Okio;
import okio.Source;

/**
 * This is a custom realization of {@link RequestBody} which will be used by {@link NodeRequestBodyConverter}.
 * <p>
 * Every request body will have the next structure: "endpoint\njson\nbytes"( where "endpoint" is a required parameter):
 * <ul>
 * <li>UTF8-encoded request name before the first LF (ASCII code 10)
 * <li>UTF8-encoded request JSON metadata before the second LF (ASCII code 10)
 * <li>binary data afterwards until the end of stream
 * </ul>
 *
 * @author Denis Bondarenko
 * Date: 1/10/19
 * Time: 5:54 PM
 * E-mail: DenBond7@gmail.com
 */
public final class NodeRequestBody extends RequestBody {
  private ByteString json;
  private NodeRequest nodeRequest;

  NodeRequestBody(@NonNull NodeRequest nodeRequest, ByteString json) {
    this.nodeRequest = nodeRequest;
    this.json = json;
  }

  @Override
  public MediaType contentType() {
    return null;
  }

  @Override
  public void writeTo(@NonNull BufferedSink sink) throws IOException {
    sink.writeUtf8(nodeRequest.getEndpoint());
    sink.writeByte('\n');
    sink.write(json);
    sink.writeByte('\n');
    if (nodeRequest.getData() != null) {
      Source source = null;
      try {
        source = Okio.source(new ByteArrayInputStream(nodeRequest.getData()));
        sink.writeAll(source);
      } finally {
        Util.closeQuietly(source);
      }
    }

    if (nodeRequest.getUri() != null) {
      Source source = null;
      try {
        InputStream inputStream = nodeRequest.getContext().getContentResolver().openInputStream(nodeRequest.getUri());
        if (inputStream != null) {
          source = Okio.source(new BufferedInputStream(inputStream));
          sink.writeAll(source);
        }
      } finally {
        Util.closeQuietly(source);
      }
    }
  }
}
