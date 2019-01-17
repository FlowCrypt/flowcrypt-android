package com.flowcrypt.email.api.retrofit.node;

import android.content.Context;
import android.net.Uri;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
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
  private Context context;
  private String endpoint;
  private byte[] data;
  private Uri uri;
  private File file;

  NodeRequestBody(String endpoint, ByteString json, byte[] data) {
    this.endpoint = endpoint;
    this.json = json;
    this.data = data;
  }

  public NodeRequestBody(String endpoint, ByteString json, File file) {
    this.endpoint = endpoint;
    this.json = json;
    this.file = file;
  }

  public NodeRequestBody(Context context, String endpoint, ByteString json, Uri uri) {
    this.context = context;
    this.endpoint = endpoint;
    this.json = json;
    this.uri = uri;
  }

  @Override
  public MediaType contentType() {
    return null;
  }

  @Override
  public void writeTo(@NonNull BufferedSink sink) throws IOException {
    sink.writeUtf8(endpoint);
    sink.writeByte('\n');
    sink.write(json);
    sink.writeByte('\n');
    if (data != null) {
      Source source = null;
      try {
        source = Okio.source(new ByteArrayInputStream(data));
        sink.writeAll(source);
      } finally {
        Util.closeQuietly(source);
      }
    }

    if (uri != null) {
      Source source = null;
      try {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream != null) {
          source = Okio.source(inputStream);
          sink.writeAll(source);
        }
      } finally {
        Util.closeQuietly(source);
      }
    }

    if (file != null) {
      Source source = null;
      try {
        source = Okio.source(new FileInputStream(file));
        sink.writeAll(source);
      } finally {
        Util.closeQuietly(source);
      }
    }
  }
}
