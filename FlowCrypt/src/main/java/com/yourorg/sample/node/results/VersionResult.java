package com.yourorg.sample.node.results;

import java.io.InputStream;

/**
 * @author DenBond7
 */
public class VersionResult extends RawNodeResult {
  public VersionResult(Exception err, InputStream inputStream, long ms) {
    super(err, inputStream, ms);
  }

  public String debugGetRawJson() {
    throwIfErrNotTested();
    return jsonResponseRaw;
  }
}
