package com.flowcrypt.email.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;

import com.hootsuite.nachos.NachoTextView;

/**
 * The custom realization of {@link NachoTextView}.
 *
 * @author DenBond7
 *         Date: 19.05.2017
 *         Time: 8:52
 *         E-mail: DenBond7@gmail.com
 */

public class CustomNachoTextView extends NachoTextView {

    public CustomNachoTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * This method prevents add a duplicate email from the dropdown to TextView.
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

        CharSequence text =
                this.getFilter().convertResultToString(this.getAdapter().getItem(position));

        if (!getText().toString().contains(text)) {
            super.onItemClick(adapterView, view, position, id);
        }

    }
}
