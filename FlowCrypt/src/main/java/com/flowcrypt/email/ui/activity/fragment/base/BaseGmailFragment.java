/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.sync.SyncErrorTypes;
import com.flowcrypt.email.util.UIUtil;

/**
 * The base fragment which must used when we will work with Gmail email.
 *
 * @author DenBond7
 *         Date: 04.05.2017
 *         Time: 10:29
 *         E-mail: DenBond7@gmail.com
 */

public abstract class BaseGmailFragment extends BaseFragment {

    protected View progressView;
    protected View statusView;
    protected TextView textViewStatusInfo;

    /**
     * Get a content view which contains a UI.
     *
     * @return <tt>View</tt> Return a progress view.
     */
    public abstract View getContentView();

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressView = view.findViewById(R.id.viewIdProgressView);
        statusView = view.findViewById(R.id.viewIdStatusView);
        textViewStatusInfo = (TextView) view.findViewById(R.id.viewIdTextViewStatusInfo);
        if (progressView == null || statusView == null || textViewStatusInfo == null) {
            throw new IllegalArgumentException("The layout file of this fragment not contains " +
                    "some needed views");
        }
    }

    /**
     * Handle an error from the sync service.
     *
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     * @param errorType   The {@link SyncErrorTypes}
     * @param e           The exception which happened.
     */
    public void onErrorOccurred(int requestCode, int errorType, Exception e) {
        getContentView().setVisibility(View.GONE);

        switch (errorType) {
            case SyncErrorTypes.CONNECTION_TO_STORE_IS_LOST:
                textViewStatusInfo.setText(R.string.there_was_syncing_problem);
                break;

            default:
                if (e != null && !TextUtils.isEmpty(e.getMessage())) {
                    textViewStatusInfo.setText(e.getMessage());
                } else {
                    textViewStatusInfo.setText(R.string.unknown_error);
                }
                break;
        }

        UIUtil.exchangeViewVisibility(getContext(), false, progressView, statusView);
        if (getSnackBar() != null) {
            getSnackBar().dismiss();
        }
    }
}
