/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.js.JsForUiManager;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.PgpKeyInfo;
import com.flowcrypt.email.js.core.Js;

import java.util.Date;


/**
 * This adapter helps to show information about imported key in the database.
 *
 * @author DenBond7
 * Date: 29.05.2017
 * Time: 11:33
 * E-mail: DenBond7@gmail.com
 */

public class PrivateKeysListCursorAdapter extends CursorAdapter {

  private java.text.DateFormat dateFormat;
  private Js js;

  public PrivateKeysListCursorAdapter(Context context, Cursor cursor) {
    super(context, cursor, false);
    this.dateFormat = DateFormat.getMediumDateFormat(context);
    this.js = JsForUiManager.getInstance(context).getJs();
  }

  @Override
  public View newView(Context context, Cursor cursor, ViewGroup parent) {
    return LayoutInflater.from(context).inflate(R.layout.key_item, parent, false);
  }

  @Override
  public void bindView(View view, Context context, Cursor cursor) {
    TextView textViewKeyOwner = view.findViewById(R.id.textViewKeyOwner);
    TextView textViewKeywords = view.findViewById(R.id.textViewKeywords);
    TextView textViewCreationDate = view.findViewById(R.id.textViewCreationDate);

    String longId = cursor.getString(cursor.getColumnIndex(KeysDaoSource.COL_LONG_ID));
    PgpKeyInfo keyInfo = js.getStorageConnector().getPgpPrivateKey(longId);
    PgpKey pgpKey = js.crypto_key_read(keyInfo.getPrivate());

    textViewKeyOwner.setText(pgpKey.getPrimaryUserId().getEmail());
    textViewKeywords.setText(js.mnemonic(longId));

    long timestamp = pgpKey.getCreated();

    if (timestamp != -1) {
      textViewCreationDate.setText(dateFormat.format(new Date(timestamp)));
    }
  }
}
