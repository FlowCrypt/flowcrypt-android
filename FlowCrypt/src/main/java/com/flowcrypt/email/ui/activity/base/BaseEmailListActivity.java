/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.idling.CountingIdlingResource;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.sync.SyncErrorTypes;
import com.flowcrypt.email.service.EmailSyncService;
import com.flowcrypt.email.ui.activity.EmailManagerActivity;
import com.flowcrypt.email.ui.activity.fragment.EmailListFragment;
import com.flowcrypt.email.util.GeneralUtil;

/**
 * The base {@link android.app.Activity} for displaying messages.
 *
 * @author Denis Bondarenko
 * Date: 26.04.2018
 * Time: 16:45
 * E-mail: DenBond7@gmail.com
 */
public abstract class BaseEmailListActivity extends BaseSyncActivity implements
        EmailListFragment.OnManageEmailsListener {
    protected CountingIdlingResource countingIdlingResourceForMessages;

    public abstract void refreshFoldersInfoFromCache();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        countingIdlingResourceForMessages = new CountingIdlingResource(GeneralUtil.generateNameForIdlingResources
                (EmailManagerActivity.class), BuildConfig.DEBUG);

    }

    @Override
    public void onReplyFromServiceReceived(int requestCode, int resultCode, Object obj) {
        switch (requestCode) {
            case R.id.syns_request_code_load_next_messages:
                refreshFoldersInfoFromCache();
                onNextMessagesLoaded(resultCode == EmailSyncService.REPLY_RESULT_CODE_NEED_UPDATE);
                if (!countingIdlingResourceForMessages.isIdleNow()) {
                    countingIdlingResourceForMessages.decrement();
                }
                break;
        }
    }

    @Override
    public void onErrorFromServiceReceived(int requestCode, int errorType, Exception e) {
        switch (requestCode) {
            case R.id.syns_request_code_load_next_messages:
                if (!countingIdlingResourceForMessages.isIdleNow()) {
                    countingIdlingResourceForMessages.decrement();
                }
                notifyEmailListFragmentAboutError(requestCode, errorType, e);
                break;
        }
    }

    @Override
    public void onProgressReplyFromServiceReceived(int requestCode, int resultCode, Object obj) {
        switch (requestCode) {
            case R.id.syns_request_code_load_next_messages:
                switch (resultCode) {
                    case R.id.progress_id_start_of_loading_new_messages:
                        updateActionProgressState(0, "Starting");
                        break;

                    case R.id.progress_id_adding_task_to_queue:
                        updateActionProgressState(10, "Queuing");
                        break;

                    case R.id.progress_id_queue_is_not_empty:
                        updateActionProgressState(15, "Queue is not empty");
                        break;

                    case R.id.progress_id_thread_is_cancalled_and_done:
                        updateActionProgressState(15, "Thread is cancelled and done");
                        break;

                    case R.id.progress_id_thread_is_done:
                        updateActionProgressState(15, "Thread is done");
                        break;

                    case R.id.progress_id_thread_is_cancalled:
                        updateActionProgressState(15, "Thread is cancelled");
                        break;

                    case R.id.progress_id_running_task:
                        updateActionProgressState(20, "Running task");
                        break;

                    case R.id.progress_id_resetting_connection:
                        updateActionProgressState(30, "Resetting connection");
                        break;

                    case R.id.progress_id_connecting_to_email_server:
                        updateActionProgressState(40, "Connecting");
                        break;

                    case R.id.progress_id_running_smtp_action:
                        updateActionProgressState(50, "Running SMTP action");
                        break;

                    case R.id.progress_id_running_imap_action:
                        updateActionProgressState(60, "Running IMAP action");
                        break;

                    case R.id.progress_id_opening_store:
                        updateActionProgressState(70, "Opening store");
                        break;

                    case R.id.progress_id_getting_list_of_emails:
                        updateActionProgressState(80, "Getting list of emails");
                        break;
                }
                break;

        }
    }

    @Override
    @VisibleForTesting
    public CountingIdlingResource getCountingIdlingResourceForMessages() {
        return countingIdlingResourceForMessages;
    }

    /**
     * Handle an error from the sync service.
     *
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     * @param errorType   The {@link SyncErrorTypes}
     * @param e           The exception which happened.
     */
    protected void notifyEmailListFragmentAboutError(int requestCode, int errorType, Exception e) {
        EmailListFragment emailListFragment = (EmailListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.emailListFragment);

        if (emailListFragment != null) {
            emailListFragment.onErrorOccurred(requestCode, errorType, e);
            updateActionProgressState(100, null);
        }
    }

    /**
     * Update a progress of some action.
     *
     * @param progress The action progress.
     * @param message  The user friendly message.
     */
    protected void updateActionProgressState(int progress, String message) {
        EmailListFragment emailListFragment = (EmailListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.emailListFragment);

        if (emailListFragment != null) {
            emailListFragment.setActionProgress(progress, message);
        }
    }

    /**
     * Handle a result from the load next messages action.
     *
     * @param needToRefreshList true if we must reload the emails list.
     */
    protected void onNextMessagesLoaded(boolean needToRefreshList) {
        EmailListFragment emailListFragment = (EmailListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.emailListFragment);

        if (emailListFragment != null) {
            emailListFragment.onNextMessagesLoaded(needToRefreshList);
            emailListFragment.setActionProgress(100, null);
        }
    }

}
