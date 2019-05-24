/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Arrays;

import okhttp3.internal.Util;
import okio.BufferedSource;

/**
 * It's a realization of {@link Reader} which will be used by {@link NodeResponseBodyConverter} to parse JSON from
 * the buffered input stream.
 *
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 11:54 AM
 * E-mail: DenBond7@gmail.com
 */
public final class NodeJsonReader extends Reader {
  private final Charset charset;

  private boolean closed;
  private Reader delegate;
  private boolean isStopped;
  private BufferedSource source;
  private BufferedInputStream bufferedInputStream;

  NodeJsonReader(BufferedInputStream bufferedInputStream, BufferedSource source, Charset charset) {
    this.bufferedInputStream = bufferedInputStream;
    this.charset = charset;
    this.source = source;
  }

  @Override
  public int read() throws IOException {
    return super.read();
  }

  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {
    if (closed) throw new IOException("Stream closed");

    if (isStopped) {
      return -1;
    }

    Reader delegate = this.delegate;
    if (delegate == null) {
      Charset charset = Util.bomAwareCharset(source, this.charset);
      delegate = this.delegate = new BufferedReader(new InputStreamReader(bufferedInputStream, charset));
    }

    bufferedInputStream.mark(0);
    delegate.mark(1);
    int count = delegate.read(cbuf, off, len);

    if (count != -1) {

      int position = 0;
      for (int i = 0; i < cbuf.length; i++) {
        char c = cbuf[i];

        if (c == '\n') {
          position = i;
          isStopped = true;
          break;
        }
      }

      if (position > 0) {
        delegate.reset();
        bufferedInputStream.reset();

        int c;
        while ((c = bufferedInputStream.read()) != -1) {
          if (c == '\n') {
            break;
          }
        }

        Arrays.fill(cbuf, Character.MIN_VALUE);
        count = delegate.read(cbuf, off, position);
        return count;
      } else {
        return count;
      }
    } else return count;
  }

  @Override
  public void close() throws IOException {
    closed = true;
    if (delegate != null) {
      delegate.close();
    } else {
      source.close();
    }
  }
}
