/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
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
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.test.Js;
import com.flowcrypt.email.test.PgpKey;
import com.flowcrypt.email.test.PgpKeyInfo;

import java.util.Date;


/**
 * This adapter helps to show information about imported key in the database.
 *
 * @author DenBond7
 *         Date: 29.05.2017
 *         Time: 11:33
 *         E-mail: DenBond7@gmail.com
 */

public class PrivateKeysListCursorAdapter extends CursorAdapter {

    private java.text.DateFormat dateFormat;
    private Js js;

    public PrivateKeysListCursorAdapter(Context context, Cursor cursor, Js js) {
        super(context, cursor, false);
        this.dateFormat = DateFormat.getMediumDateFormat(context);
        this.js = js;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.key_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView textViewKeyOwner = (TextView) view.findViewById(R.id.textViewKeyOwner);
        TextView textViewKeywords = (TextView) view.findViewById(R.id.textViewKeywords);
        TextView textViewCreationDate = (TextView) view.findViewById(R.id.textViewCreationDate);

        String longId = cursor.getString(cursor.getColumnIndex(KeysDaoSource.COL_LONG_ID));
        PgpKeyInfo keyInfo = new SecurityStorageConnector(context).getPgpPrivateKey(longId);
        PgpKey pgpKey = js.crypto_key_read(keyInfo.getArmored());

        textViewKeyOwner.setText(pgpKey.getPrimaryUserId().getEmail());
        textViewKeywords.setText(js.mnemonic(longId));
        textViewCreationDate.setText(dateFormat.format(new Date(pgpKey.getCreated())));
    }
}
