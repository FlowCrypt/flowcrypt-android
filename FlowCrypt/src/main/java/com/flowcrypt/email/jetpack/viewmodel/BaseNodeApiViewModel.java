/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel;

import android.app.Application;

import com.flowcrypt.email.api.retrofit.response.node.NodeResponseWrapper;
import com.flowcrypt.email.jetpack.livedata.SingleLiveEvent;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

/**
 * It's a base implementation of {@link androidx.lifecycle.ViewModel} which is going to handle requests to Node.js
 *
 * @author Denis Bondarenko
 * Date: 2/15/19
 * Time: 3:35 PM
 * E-mail: DenBond7@gmail.com
 */
public abstract class BaseNodeApiViewModel extends AndroidViewModel {
  protected SingleLiveEvent<NodeResponseWrapper> responsesLiveData;

  public BaseNodeApiViewModel(@NonNull Application application) {
    super(application);
    this.responsesLiveData = new SingleLiveEvent<>();
  }

  public SingleLiveEvent<NodeResponseWrapper> getResponsesLiveData() {
    return responsesLiveData;
  }
}
