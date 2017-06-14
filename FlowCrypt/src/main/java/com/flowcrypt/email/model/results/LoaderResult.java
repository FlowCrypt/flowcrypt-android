/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model.results;

/**
 * This class used by Android loaders to create response result with an exception, if it happened.
 *
 * @author DenBond7
 *         Date: 09.05.2017
 *         Time: 16:02
 *         E-mail: DenBond7@gmail.com
 */

public class LoaderResult {
    /**
     * Some result which will be returned by loader.
     */
    private Object result;

    /**
     * Some exception which will be returned if it happened.
     */
    private Exception exception;

    public LoaderResult() {
    }

    public LoaderResult(Object result, Exception exception) {
        this.result = result;
        this.exception = exception;
    }

    @Override
    public String toString() {
        return "LoaderResult{" +
                "result=" + result +
                ", exception=" + exception +
                '}';
    }

    /**
     * Return a result after a loader will be executed.
     *
     * @return <tt>LoaderResult</tt> A result object.
     */
    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    /**
     * Return an exception if it will happened when loader will be executed.
     *
     * @return <tt>Exception</tt> A result object.
     */
    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}
