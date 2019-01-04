package com.yourorg.sample;

import android.app.Application;
import android.content.Context;

import com.yourorg.sample.node.Node;

/**
 * @author DenBond7
 */
public class App extends Application {
  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    Node.init(this);
  }
}
