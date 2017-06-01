package com.flowcrypt.email.model.results;

/**
 * This class used by Android loaders to create response result with an exception, if it happened.
 *
 * @author DenBond7
 *         Date: 09.05.2017
 *         Time: 16:02
 *         E-mail: DenBond7@gmail.com
 */

public class ActionResult<T> {
    /**
     * Some result which will be returned by loader.
     */
    private T result;

    /**
     * Some exception which will be returned if it happened.
     */
    private Exception exception;

    public ActionResult() {
    }

    public ActionResult(T result, Exception exception) {
        this.result = result;
        this.exception = exception;
    }

    @Override
    public String toString() {
        return "ActionResult{" +
                "result=" + result +
                ", exception=" + exception +
                '}';
    }

    /**
     * Return a result after a loader will be executed.
     *
     * @return <tt>T</tt> A result object.
     */
    public T getResult() {
        return result;
    }

    public void setResult(T result) {
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
