/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node;

import com.flowcrypt.email.api.retrofit.Status;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author DenBond7
 */
public class NodeResponseWrapper<T extends BaseNodeResult> {
  @NonNull
  private final Status status;

  private int requestCode;

  @Nullable
  private T baseNodeResult;

  @Nullable
  private Throwable exception;

  public NodeResponseWrapper(int requestCode, @NonNull Status status, @Nullable T baseNodeResult,
                             @Nullable Throwable exception) {
    this.status = status;
    this.requestCode = requestCode;
    this.baseNodeResult = baseNodeResult;
    this.exception = exception;
  }

  public static <T extends BaseNodeResult> NodeResponseWrapper<T> success(int requestCode, @Nullable T data) {
    return new NodeResponseWrapper<>(requestCode, Status.SUCCESS, data, null);
  }

  public static <T extends BaseNodeResult> NodeResponseWrapper<T> error(int requestCode, @Nullable T data) {
    return new NodeResponseWrapper<>(requestCode, Status.ERROR, data, null);
  }

  public static <T extends BaseNodeResult> NodeResponseWrapper<T> loading(int requestCode) {
    return new NodeResponseWrapper<>(requestCode, Status.LOADING, null, null);
  }

  public static <T extends BaseNodeResult> NodeResponseWrapper<T> exception(int requestCode, Throwable throwable) {
    return new NodeResponseWrapper<>(requestCode, Status.EXCEPTION, null, throwable);
  }

  public int getRequestCode() {
    return requestCode;
  }

  public Throwable getException() {
    return exception;
  }

  public T getResult() {
    return baseNodeResult;
  }

  @NonNull
  public Status getStatus() {
    return status;
  }
}
