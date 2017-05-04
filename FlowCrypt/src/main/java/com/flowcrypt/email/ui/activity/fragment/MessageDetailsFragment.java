package com.flowcrypt.email.ui.activity.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.model.MessageInfo;
import com.flowcrypt.email.ui.activity.fragment.base.BaseGmailFragment;
import com.flowcrypt.email.ui.loader.LoadMessageInfoAsyncTaskLoader;

/**
 * This fragment describe details of some message.
 *
 * @author DenBond7
 *         Date: 03.05.2017
 *         Time: 16:29
 *         E-mail: DenBond7@gmail.com
 */
public class MessageDetailsFragment extends BaseGmailFragment implements LoaderManager
        .LoaderCallbacks<MessageInfo> {
    public static final String KEY_GENERAL_MESSAGE_DETAILS = BuildConfig.APPLICATION_ID + "" +
            ".KEY_GENERAL_MESSAGE_DETAILS";

    private GeneralMessageDetails generalMessageDetails;

    public MessageDetailsFragment() {
    }

    public static MessageDetailsFragment newInstance(GeneralMessageDetails generalMessageDetails) {

        Bundle args = new Bundle();
        args.putParcelable(KEY_GENERAL_MESSAGE_DETAILS, generalMessageDetails);
        MessageDetailsFragment fragment = new MessageDetailsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();

        if (args != null) {
            this.generalMessageDetails = args.getParcelable(KEY_GENERAL_MESSAGE_DETAILS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_message_details, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        reloadMessageInfo();
    }

    @Override
    public Loader<MessageInfo> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_message_info:
                return new LoadMessageInfoAsyncTaskLoader(getContext(), getAccount(),
                        generalMessageDetails);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<MessageInfo> loader, MessageInfo messageInfo) {
        switch (loader.getId()) {
            case R.id.loader_id_load_message_info:
                Toast.makeText(getContext(), "" + messageInfo, Toast.LENGTH_SHORT).show();
                System.out.println(messageInfo);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<MessageInfo> loader) {

    }

    @Override
    public void onAccountUpdated() {
        reloadMessageInfo();
    }

    public void setGeneralMessageDetails(GeneralMessageDetails generalMessageDetails) {
        this.generalMessageDetails = generalMessageDetails;
    }

    private void reloadMessageInfo() {
        if (generalMessageDetails != null && getAccount() != null) {
            getLoaderManager().initLoader(R.id.loader_id_load_message_info, null, this);
        }
    }
}
