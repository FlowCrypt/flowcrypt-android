package com.flowcrypt.email.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.node.Node;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

/**
 * @author DenBond7
 */
public class NodeSplashActivity extends AppCompatActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Node node = Node.getInstance();
    LiveData<Boolean> liveData = node.getLiveData();
    liveData.observe(this, new Observer<Boolean>() {
      @Override
      public void onChanged(@Nullable Boolean value) {
        if (value != null) {
          if (value) {
            startActivity(new Intent(NodeSplashActivity.this, NodeTestActivity.class));
            finish();
          } else {
            Toast.makeText(NodeSplashActivity.this, R.string.error_occurred_during_node_init, Toast.LENGTH_LONG).show();
          }
        } else {
          Toast.makeText(NodeSplashActivity.this, R.string.unknown_error, Toast.LENGTH_LONG).show();
        }
      }
    });
    Node.init(getApplication());
  }
}
