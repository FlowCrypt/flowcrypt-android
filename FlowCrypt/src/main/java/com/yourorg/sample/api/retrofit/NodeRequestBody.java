package com.yourorg.sample.api.retrofit;

import android.content.Context;
import android.net.Uri;

import com.google.gson.GsonBuilder;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * @author DenBond7
 */
public class NodeRequestBody<T> extends RequestBody {
  private Context context;
  private String endpoint;
  private T request;
  private byte[] data;
  private Uri uri;
  private File file;

  public NodeRequestBody(String endpoint, T request, byte[] data) {
    this.endpoint = endpoint;
    this.request = request;
    this.data = data;
  }

  public NodeRequestBody(String endpoint, T request, File file) {
    this.endpoint = endpoint;
    this.request = request;
    this.file = file;
  }

  public NodeRequestBody(Context context, String endpoint, T request, Uri uri) {
    this.context = context;
    this.endpoint = endpoint;
    this.request = request;
    this.uri = uri;
  }

  @Override
  public MediaType contentType() {
    return null;
  }

  @Override
  public void writeTo(BufferedSink sink) throws IOException {
    sink.writeUtf8(endpoint);
    sink.writeByte('\n');
    if (request == null) {
      sink.writeUtf8("{}");
    } else {
      sink.writeUtf8(new GsonBuilder().create().toJson(request));
    }
    sink.writeByte('\n');
    if (data != null) {
      Source source = null;
      try {
        source = Okio.source(new BufferedInputStream(new ByteArrayInputStream(data)));
        sink.writeAll(source);
      } finally {
        Util.closeQuietly(source);
      }
    }

    if (uri != null) {
      Source source = null;
      try {
        source = Okio.source(context.getContentResolver().openInputStream(uri));
        sink.writeAll(source);
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

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public T getRequest() {
    return request;
  }

  public void setRequest(T request) {
    this.request = request;
  }
}
