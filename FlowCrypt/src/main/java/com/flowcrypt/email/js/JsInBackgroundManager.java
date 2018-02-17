/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import android.util.Log;

import com.flowcrypt.email.js.tasks.DecryptRawMimeMessageJsTask;
import com.flowcrypt.email.js.tasks.JsTask;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.flowcrypt.email.util.exception.ManualHandledException;

import org.acra.ACRA;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This manager creates and manages {@link JsTask}s using a background thread.
 *
 * @author Denis Bondarenko
 *         Date: 15.02.2018
 *         Time: 13:04
 *         E-mail: DenBond7@gmail.com
 */

public class JsInBackgroundManager {
    private static final String TAG = JsInBackgroundManager.class.getSimpleName();

    private BlockingQueue<JsTask> blockingQueue;
    private ExecutorService executorService;
    private Future<?> future;

    /**
     * This fields created as volatile because will be used in different threads.
     */
    private volatile JsListener jsListener;

    public JsInBackgroundManager() {
        this.blockingQueue = new LinkedBlockingQueue<>();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Init a current instance.
     */
    public void init() {
        Log.d(TAG, "init");
        if (!isThreadAlreadyWork(future)) {
            future = executorService.submit(new JsRunnable());
        }
    }

    /**
     * Stop an active action.
     */
    public void stop() {
        Log.d(TAG, "stop");
        reset();

        if (executorService != null) {
            executorService.shutdown();
        }
    }

    /**
     * Clear the tasks queue.
     */
    public void cancelAllTasks() {
        Log.d(TAG, "cancelAllTasks");
        if (blockingQueue != null) {
            blockingQueue.clear();
        }
    }

    /**
     * Set the {@link JsListener} for current {@link JsInBackgroundManager}
     *
     * @param jsListener A new listener.
     */
    public void setJsListener(JsListener jsListener) {
        this.jsListener = jsListener;
    }

    /**
     * Decrypt an incoming raw message.
     *
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for identify the current action.
     * @param rawMessage  The incoming raw message.
     */
    public void decryptMessage(String ownerKey, int requestCode, String rawMessage) {
        try {
            blockingQueue.put(new DecryptRawMimeMessageJsTask(ownerKey, requestCode, rawMessage));
        } catch (InterruptedException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    /**
     * Check a pull thread state.
     *
     * @return true if already work, otherwise false.
     */
    private boolean isThreadAlreadyWork(Future<?> future) {
        return future != null && !future.isCancelled() && !future.isDone();
    }

    /**
     * Reset a current pull thread.
     */
    private void reset() {
        cancelAllTasks();

        if (future != null) {
            future.cancel(true);
        }
    }

    /**
     * Remove old tasks from the queue.
     *
     * @param cls           The task type.
     * @param blockingQueue The queue of the tasks.
     */
    private void removeOldTasksFromBlockingQueue(Class<?> cls, BlockingQueue<JsTask> blockingQueue) {
        Iterator<?> iterator = blockingQueue.iterator();
        while (iterator.hasNext()) {
            if (cls.isInstance(iterator.next())) {
                iterator.remove();
            }
        }
    }

    /**
     * An implementation of the worker thread.
     */
    private class JsRunnable implements Runnable {
        private final String TAG = JsRunnable.class.getSimpleName();
        private Js js;

        @Override
        public void run() {
            Log.d(TAG, " run!");
            Thread.currentThread().setName(getClass().getSimpleName());
            try {
                js = new Js(jsListener.getContext(), new SecurityStorageConnector(jsListener.getContext()));
                while (!Thread.interrupted()) {
                    try {
                        Log.d(TAG, "blockingQueue size = " + blockingQueue.size());
                        JsTask jsTask = blockingQueue.take();

                        if (jsTask != null) {
                            runJsTask(jsTask);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d(TAG, " stopped!");
        }

        /**
         * Run the incoming {@link JsTask}
         *
         * @param jsTask The incoming {@link JsTask}
         */
        void runJsTask(JsTask jsTask) {
            try {
                Log.d(TAG, "Start a new task = " + jsTask.getClass().getSimpleName());
                jsTask.runAction(js, jsListener);
                Log.d(TAG, "The task = " + jsTask.getClass().getSimpleName() + " completed");
            } catch (Exception e) {
                e.printStackTrace();
                if (ExceptionUtil.isErrorHandleWithACRA(e)) {
                    if (ACRA.isInitialised()) {
                        ACRA.getErrorReporter().handleException(new ManualHandledException(e));
                    }
                }
                jsTask.handleException(e, jsListener);
            }
        }
    }
}

