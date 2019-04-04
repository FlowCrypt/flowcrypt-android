/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog;

import android.os.Bundle;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.node.Node;
import com.flowcrypt.email.util.idling.NodeIdlingResource;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.loader.content.Loader;

/**
 * The base dialog fragment.
 *
 * @author Denis Bondarenko
 * Date: 01.08.2017
 * Time: 10:04
 * E-mail: DenBond7@gmail.com
 */

public class BaseDialogFragment extends DialogFragment {
  protected NodeIdlingResource nodeIdlingResource;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    registerNodeIdlingResources();
  }

  public void handleLoaderResult(Loader loader, LoaderResult loaderResult) {
    if (loaderResult != null) {
      if (loaderResult.getResult() != null) {
        onSuccess(loader.getId(), loaderResult.getResult());
      } else if (loaderResult.getException() != null) {
        onError(loader.getId(), loaderResult.getException());
      } else {
        showToast(getString(R.string.unknown_error));
      }
    } else {
      showToast(getString(R.string.unknown_error));
    }
  }

  public void onError(int loaderId, Exception e) {

  }

  public void onSuccess(int loaderId, Object result) {

  }

  public void showToast(String string) {
    Toast.makeText(getContext(), string, Toast.LENGTH_SHORT).show();
  }

  @VisibleForTesting
  public NodeIdlingResource getNodeIdlingResource() {
    return nodeIdlingResource;
  }

  protected boolean isNodeReady() {
    if (Node.getInstance() == null || Node.getInstance().getLiveData() == null
        || Node.getInstance().getLiveData().getValue() == null) {
      return false;
    }

    return Node.getInstance().getLiveData().getValue();
  }

  protected void onNodeStateChanged(Boolean newState) {

  }

  private void registerNodeIdlingResources() {
    nodeIdlingResource = new NodeIdlingResource();
    Node.getInstance().getLiveData().observe(this, new Observer<Boolean>() {
      @Override
      public void onChanged(Boolean aBoolean) {
        nodeIdlingResource.setIdleState(aBoolean);
        onNodeStateChanged(aBoolean);
      }
    });
  }
}
