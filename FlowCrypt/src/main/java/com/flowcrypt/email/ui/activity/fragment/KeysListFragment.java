/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.retrofit.node.NodeRepository;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.api.retrofit.response.node.NodeResponseWrapper;
import com.flowcrypt.email.api.retrofit.response.node.ParseKeysResult;
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel;
import com.flowcrypt.email.ui.activity.ImportPrivateKeyActivity;
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment;
import com.flowcrypt.email.util.UIUtil;
import com.google.android.gms.common.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * This {@link Fragment} shows information about available private keys in the database.
 *
 * @author DenBond7
 * Date: 20.11.2018
 * Time: 10:30
 * E-mail: DenBond7@gmail.com
 */
public class KeysListFragment extends BaseFragment implements View.OnClickListener,
    PrivateKeysRecyclerViewAdapter.OnKeySelectedListener, Observer<NodeResponseWrapper> {

  private static final int REQUEST_CODE_START_IMPORT_KEY_ACTIVITY = 0;

  private View progressBar;
  private View emptyView;
  private View content;

  private PrivateKeysRecyclerViewAdapter recyclerViewAdapter;

  public static KeysListFragment newInstance() {
    return new KeysListFragment();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    PrivateKeysViewModel viewModel = ViewModelProviders.of(this).get(PrivateKeysViewModel.class);
    viewModel.init(new NodeRepository());
    viewModel.getResponsesLiveData().observe(this, this);
    recyclerViewAdapter = new PrivateKeysRecyclerViewAdapter(getContext(), new ArrayList<NodeKeyDetails>(), this);
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_private_keys, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initViews(view);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    if (getSupportActionBar() != null) {
      getSupportActionBar().setTitle(R.string.keys);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_CODE_START_IMPORT_KEY_ACTIVITY:
        switch (resultCode) {
          case Activity.RESULT_OK:
            Toast.makeText(getContext(), R.string.key_successfully_imported, Toast.LENGTH_SHORT).show();
            break;
        }
        break;

      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.floatActionButtonAddKey:
        runCreateOrImportKeyActivity();
        break;
    }
  }

  @Override
  public void onKeySelected(int position, NodeKeyDetails nodeKeyDetails) {
    getFragmentManager()
        .beginTransaction()
        .replace(R.id.layoutContent, KeyDetailsFragment.newInstance(nodeKeyDetails))
        .addToBackStack(null)
        .commit();
  }

  @Override
  public void onChanged(NodeResponseWrapper nodeResponseWrapper) {
    switch (nodeResponseWrapper.getRequestCode()) {
      case R.id.live_data_id_fetch_keys:
        switch (nodeResponseWrapper.getStatus()) {
          case LOADING:
            emptyView.setVisibility(View.GONE);
            UIUtil.exchangeViewVisibility(getContext(), true, progressBar, content);
            break;

          case SUCCESS:
            ParseKeysResult parseKeysResult = (ParseKeysResult) nodeResponseWrapper.getResult();
            List<NodeKeyDetails> nodeKeyDetailsList = parseKeysResult.getNodeKeyDetails();
            if (CollectionUtils.isEmpty(nodeKeyDetailsList)) {
              recyclerViewAdapter.swap(Collections.<NodeKeyDetails>emptyList());
              UIUtil.exchangeViewVisibility(getContext(), true, emptyView, content);
            } else {
              recyclerViewAdapter.swap(nodeKeyDetailsList);
              UIUtil.exchangeViewVisibility(getContext(), false, progressBar, content);
            }
            break;

          case ERROR:
            Toast.makeText(getContext(), nodeResponseWrapper.getResult().getError().toString(),
                Toast.LENGTH_SHORT).show();
            break;

          case EXCEPTION:
            Toast.makeText(getContext(), nodeResponseWrapper.getException().getMessage(), Toast.LENGTH_SHORT).show();
            break;
        }
        break;
    }
  }

  private void runCreateOrImportKeyActivity() {
    startActivityForResult(ImportPrivateKeyActivity.newIntent(getContext(), getString(R.string.import_private_key),
        true, ImportPrivateKeyActivity.class), REQUEST_CODE_START_IMPORT_KEY_ACTIVITY);
  }

  private void initViews(View root) {
    this.progressBar = root.findViewById(R.id.progressBar);
    this.content = root.findViewById(R.id.groupContent);
    this.emptyView = root.findViewById(R.id.emptyView);

    RecyclerView recyclerView = root.findViewById(R.id.recyclerViewKeys);
    recyclerView.setHasFixedSize(true);
    LinearLayoutManager manager = new LinearLayoutManager(getContext());
    DividerItemDecoration decoration = new DividerItemDecoration(recyclerView.getContext(), manager.getOrientation());
    decoration.setDrawable(getResources().getDrawable(R.drawable.divider_1dp_grey, getContext().getTheme()));
    recyclerView.addItemDecoration(decoration);
    recyclerView.setLayoutManager(manager);
    recyclerView.setAdapter(recyclerViewAdapter);

    if (recyclerViewAdapter.getItemCount() > 0) {
      progressBar.setVisibility(View.GONE);
    }

    if (root.findViewById(R.id.floatActionButtonAddKey) != null) {
      root.findViewById(R.id.floatActionButtonAddKey).setOnClickListener(this);
    }
  }
}
