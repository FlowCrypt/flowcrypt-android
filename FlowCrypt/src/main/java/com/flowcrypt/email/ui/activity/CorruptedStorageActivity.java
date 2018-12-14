package com.flowcrypt.email.ui.activity;

import android.os.Bundle;

import com.flowcrypt.email.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/**
 * This {@link android.app.Activity} handles situations when the app storage space was corrupted. This can
 * happen on certain rooted or unofficial systems, due to user intervention, etc.
 *
 * @author DenBond7
 * Date: 12/14/2018
 * Time: 12:20
 * E-mail: DenBond7@gmail.com
 */
public class CorruptedStorageActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_corrupted_storage);

    Toolbar toolbar = findViewById(R.id.toolbar);
    if (toolbar != null) {
      setSupportActionBar(toolbar);
    }
  }
}
