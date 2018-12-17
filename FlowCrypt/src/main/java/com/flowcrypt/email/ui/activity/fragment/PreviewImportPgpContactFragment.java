/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment;

import android.app.Activity;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.MessageBlock;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.core.Js;
import com.flowcrypt.email.model.PublicKeyInfo;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment;
import com.flowcrypt.email.ui.adapter.ImportPgpContactsRecyclerViewAdapter;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * This fragment displays information about public keys owners and information about keys.
 *
 * @author Denis Bondarenko
 * Date: 05.06.2018
 * Time: 14:15
 * E-mail: DenBond7@gmail.com
 */
public class PreviewImportPgpContactFragment extends BaseFragment implements View.OnClickListener {
  private static final String KEY_EXTRA_PUBLIC_KEY_STRING = GeneralUtil.generateUniqueExtraKey
      ("KEY_EXTRA_PUBLIC_KEY_STRING", PreviewImportPgpContactFragment.class);

  private static final String KEY_EXTRA_PUBLIC_KEYS_FILE_URI
      = GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_PUBLIC_KEYS_FILE_URI", PreviewImportPgpContactFragment.class);

  private List<PublicKeyInfo> publicKeyInfoList;
  private RecyclerView recyclerView;
  private TextView btnImportAll;
  private TextView textViewProgressTitle;
  private ProgressBar progressBar;
  private View layoutContentView;
  private View layoutProgress;
  private View emptyView;

  private String publicKeysString;
  private Uri publicKeysFileUri;

  private boolean isParsingStarted;

  public static PreviewImportPgpContactFragment newInstance(String stringExtra, Parcelable parcelableExtra) {
    Bundle args = new Bundle();
    args.putString(KEY_EXTRA_PUBLIC_KEY_STRING, stringExtra);
    args.putParcelable(KEY_EXTRA_PUBLIC_KEYS_FILE_URI, parcelableExtra);

    PreviewImportPgpContactFragment previewImportPgpContactFragment = new PreviewImportPgpContactFragment();
    previewImportPgpContactFragment.setArguments(args);
    return previewImportPgpContactFragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRetainInstance(true);

    Bundle bundle = getArguments();

    if (bundle != null) {
      publicKeysString = bundle.getString(KEY_EXTRA_PUBLIC_KEY_STRING);
      publicKeysFileUri = bundle.getParcelable(KEY_EXTRA_PUBLIC_KEYS_FILE_URI);
    }
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_preview_import_pgp_contact, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initViews(view);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (TextUtils.isEmpty(publicKeysString) && publicKeysFileUri == null) {
      if (getActivity() != null) {
        getActivity().setResult(Activity.RESULT_CANCELED);
        getActivity().finish();
      }
    } else if (!isParsingStarted) {
      new PublicKeysParserAsyncTask(this, publicKeysString, publicKeysFileUri).execute();
      isParsingStarted = true;
    }
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.buttonImportAll:
        new SaveAllContactsAsyncTask(this, publicKeyInfoList).execute();
        break;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void onSuccess(int loaderId, Object result) {
    switch (loaderId) {
      case R.id.loader_id_parse_public_keys:
        publicKeyInfoList = (List<PublicKeyInfo>) result;
        if (!publicKeyInfoList.isEmpty()) {
          UIUtil.exchangeViewVisibility(getContext(), false, layoutProgress, layoutContentView);
          recyclerView.setAdapter(new ImportPgpContactsRecyclerViewAdapter(publicKeyInfoList));
          btnImportAll.setVisibility(publicKeyInfoList.size() > 1 ? View.VISIBLE : View.GONE);
        } else {
          UIUtil.exchangeViewVisibility(getContext(), false, layoutProgress, emptyView);
        }
        break;

      default:
        super.onSuccess(loaderId, result);
    }
  }

  @Override
  public void onError(int loaderId, Exception e) {
    switch (loaderId) {
      case R.id.loader_id_parse_public_keys:
        if (getActivity() != null) {
          getActivity().setResult(Activity.RESULT_CANCELED);
          Toast.makeText(getContext(), TextUtils.isEmpty(e.getMessage()) ? getString(R.string.unknown_error) :
              e.getMessage(), Toast.LENGTH_SHORT).show();
          getActivity().finish();
        }
        break;

      default:
        super.onError(loaderId, e);
    }
  }

  private void initViews(View root) {
    layoutContentView = root.findViewById(R.id.layoutContentView);
    layoutProgress = root.findViewById(R.id.layoutProgress);
    btnImportAll = root.findViewById(R.id.buttonImportAll);
    textViewProgressTitle = root.findViewById(R.id.textViewProgressTitle);
    progressBar = root.findViewById(R.id.progressBar);
    btnImportAll.setOnClickListener(this);
    recyclerView = root.findViewById(R.id.recyclerViewContacts);
    recyclerView.setHasFixedSize(true);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    emptyView = root.findViewById(R.id.emptyView);
  }

  private void handleImportAllResult(Boolean result) {
    if (isAdded()) {
      if (result) {
        Toast.makeText(getContext(), R.string.success, Toast.LENGTH_SHORT).show();
        if (getActivity() != null) {
          getActivity().setResult(Activity.RESULT_OK);
          getActivity().finish();
        }
      } else {
        UIUtil.exchangeViewVisibility(getContext(), false, layoutProgress, layoutContentView);
        Toast.makeText(getContext(), R.string.unknown_error, Toast.LENGTH_SHORT).show();
      }
    }
  }

  private static class PublicKeysParserAsyncTask extends BaseAsyncTask<Void, Integer, LoaderResult> {
    private final String publicKeysString;
    private final Uri publicKeysFileUri;

    PublicKeysParserAsyncTask(PreviewImportPgpContactFragment fragment, String publicKeysString, Uri uri) {
      super(fragment);
      this.publicKeysString = publicKeysString;
      this.publicKeysFileUri = uri;
    }

    @Override
    protected LoaderResult doInBackground(Void... uris) {
      String armoredKeys = publicKeysString;

      try {
        if (publicKeysFileUri != null && weakRef.get() != null) {
          armoredKeys = GeneralUtil.readFileFromUriToString(weakRef.get().getContext(), publicKeysFileUri);
        }
      } catch (IOException e) {
        e.printStackTrace();
        return new LoaderResult(null, e);
      }

      if (!TextUtils.isEmpty(armoredKeys) && weakRef.get() != null) {
        return parseKeys(armoredKeys);
      } else {
        return new LoaderResult(null, new NullPointerException("An input string is null!"));
      }
    }

    @Override
    protected void onPostExecute(LoaderResult loaderResult) {
      super.onPostExecute(loaderResult);
      if (weakRef.get() != null) {
        weakRef.get().handleLoaderResult(R.id.loader_id_parse_public_keys, loaderResult);
      }
    }

    @Override
    public int getProgressTitleResourcesId() {
      return R.string.parsing_public_keys;
    }

    @Override
    public void updateProgress(Integer integer) {
      if (weakRef.get() != null) {
        weakRef.get().progressBar.setProgress(integer);
      }
    }

    private LoaderResult parseKeys(String armoredKeys) {
      try {
        Js js = new Js(weakRef.get().getContext(), null);
        String normalizedArmoredKey = js.crypto_key_normalize(armoredKeys);
        PgpKey pgpKey = js.crypto_key_read(normalizedArmoredKey);

        if (js.is_valid_key(pgpKey, false)) {
          return new LoaderResult(parsePublicKeysInfo(js, armoredKeys), null);
        } else {
          if (weakRef.get() != null) {
            return new LoaderResult(null, new IllegalArgumentException(
                weakRef.get().getContext().getString(R.string.clipboard_has_wrong_structure,
                    weakRef.get().getContext().getString(R.string.public_))));
          } else {
            return new LoaderResult(null,
                new IllegalArgumentException("The content of your clipboard doesn't look like a valid PGP pubkey."));
          }
        }

      } catch (Exception e) {
        e.printStackTrace();
        ExceptionUtil.handleError(e);
        return new LoaderResult(null, e);
      }
    }

    private List<PublicKeyInfo> parsePublicKeysInfo(Js js, @NonNull String armoredKeys) {
      List<PublicKeyInfo> publicKeyInfoList = new ArrayList<>();

      if (TextUtils.isEmpty(armoredKeys)) {
        return publicKeyInfoList;
      }

      Set<String> emails = new HashSet<>();
      MessageBlock[] msgBlocks = js.crypto_armor_detect_blocks(armoredKeys);

      int blocksCount = msgBlocks.length;
      float progress;
      float lastProgress = 0;

      for (int i = 0; i < msgBlocks.length; i++) {
        MessageBlock messageBlock = msgBlocks[i];
        if (messageBlock != null && messageBlock.getType() != null) {
          switch (messageBlock.getType()) {
            case MessageBlock.TYPE_PGP_PUBLIC_KEY:
              PublicKeyInfo publicKeyInfo = getPublicKeyInfo(js, emails, messageBlock);

              if (publicKeyInfo != null) {
                publicKeyInfoList.add(publicKeyInfo);
              }
              break;
          }
        }

        progress = i * 100f / blocksCount;
        if (progress - lastProgress >= 1) {
          publishProgress((int) progress);
          lastProgress = progress;
        }
      }

      publishProgress(100);

      return publicKeyInfoList;
    }

    private PublicKeyInfo getPublicKeyInfo(Js js, Set<String> emails, MessageBlock messageBlock) {
      String content = messageBlock.getContent();
      String fingerprint = js.crypto_key_fingerprint(js.crypto_key_read(content));
      String longId = js.crypto_key_longid(fingerprint);
      String keyWords = js.mnemonic(longId);
      PgpKey pgpKey = js.crypto_key_read(content);
      String keyOwner = pgpKey.getPrimaryUserId().getEmail();

      if (keyOwner != null) {
        keyOwner = keyOwner.toLowerCase();

        if (emails.contains(keyOwner)) {
          return null;
        }

        emails.add(keyOwner);

        if (weakRef.get() != null) {
          PgpContact contact = new ContactsDaoSource().getPgpContact(weakRef.get().getContext(), keyOwner);
          return new PublicKeyInfo(keyWords, fingerprint, keyOwner, longId, contact, content);
        }
      }
      return null;
    }
  }

  private static class SaveAllContactsAsyncTask extends BaseAsyncTask<Void, Integer, Boolean> {
    private static final int STEP_AMOUNT = 50;

    private final List<PublicKeyInfo> publicKeyInfoList;

    SaveAllContactsAsyncTask(PreviewImportPgpContactFragment fragment, List<PublicKeyInfo> publicKeyInfoList) {
      super(fragment);
      this.publicKeyInfoList = publicKeyInfoList;
    }

    @Override
    protected Boolean doInBackground(Void... uris) {
      ContactsDaoSource source = new ContactsDaoSource();
      List<PgpContact> newCandidates = new ArrayList<>();
      List<PgpContact> updateCandidates = new ArrayList<>();

      for (PublicKeyInfo publicKeyInfo : publicKeyInfoList) {
        PgpContact pgpContact = new PgpContact(publicKeyInfo.getKeyOwner(), null, publicKeyInfo.getPublicKey(),
            true, null, false, publicKeyInfo.getFingerprint(), publicKeyInfo.getLongId(),
            publicKeyInfo.getKeyWords(), 0);

        if (publicKeyInfo.hasPgpContact()) {
          if (publicKeyInfo.isUpdateEnabled()) {
            updateCandidates.add(pgpContact);
          }
        } else {
          newCandidates.add(pgpContact);
        }
      }

      try {
        float progress;
        float lastProgress = 0f;
        int totalOperationsCount = newCandidates.size() + updateCandidates.size();

        for (int i = 0; i < newCandidates.size(); i++) {
          int start = i;
          int end = newCandidates.size() - i > STEP_AMOUNT ? i + STEP_AMOUNT : newCandidates.size();

          if (weakRef.get() != null) {
            source.addRowsUsingApplyBatch(weakRef.get().getContext(), newCandidates.subList(start, end));
          }
          i = end;

          progress = i * 100f / totalOperationsCount;
          if (progress - lastProgress >= 1) {
            publishProgress((int) progress);
            lastProgress = progress;
          }

          i--;
        }

        for (int i = 0; i < updateCandidates.size(); i++) {
          int start = i;
          int end = updateCandidates.size() - i > STEP_AMOUNT ? i + STEP_AMOUNT : updateCandidates.size() - 1;

          if (weakRef.get() != null) {
            source.updatePgpContacts(weakRef.get().getContext(), updateCandidates.subList(start, end + 1));
          }
          i = end + 1;

          progress = i * 100f / totalOperationsCount;
          if (progress - lastProgress >= 1) {
            publishProgress((int) progress);
            lastProgress = progress;
          }
        }

      } catch (RemoteException | OperationApplicationException e) {
        e.printStackTrace();
        return false;
      }

      publishProgress(100);
      return true;
    }


    @Override
    protected void onPostExecute(Boolean b) {
      super.onPostExecute(b);
      if (b != null && weakRef.get() != null) {
        weakRef.get().handleImportAllResult(b);
      }
    }

    @Override
    public int getProgressTitleResourcesId() {
      return R.string.importing_public_keys;
    }

    @Override
    public void updateProgress(Integer integer) {
      if (weakRef.get() != null) {
        weakRef.get().progressBar.setProgress(integer);
      }
    }
  }

  private abstract static class BaseAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
    final WeakReference<PreviewImportPgpContactFragment> weakRef;

    BaseAsyncTask(PreviewImportPgpContactFragment previewImportPgpContactFragment) {
      this.weakRef = new WeakReference<>(previewImportPgpContactFragment);
    }

    public abstract int getProgressTitleResourcesId();

    public abstract void updateProgress(Progress progress);

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      if (weakRef.get() != null) {
        weakRef.get().progressBar.setIndeterminate(true);
        weakRef.get().textViewProgressTitle.setText(getProgressTitleResourcesId());
        UIUtil.exchangeViewVisibility(weakRef.get().getContext(), true,
            weakRef.get().layoutProgress, weakRef.get().layoutContentView);
      }
    }

    @SafeVarargs
    @Override
    protected final void onProgressUpdate(Progress... values) {
      super.onProgressUpdate(values);
      if (weakRef.get() != null) {
        if (weakRef.get().progressBar.isIndeterminate()) {
          weakRef.get().progressBar.setIndeterminate(false);
        }
        updateProgress(values[0]);
      }
    }
  }
}
