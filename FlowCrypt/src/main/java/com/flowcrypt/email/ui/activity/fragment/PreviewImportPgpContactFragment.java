/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.MessageBlock;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpKey;
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

/**
 * This fragment displays information about public keys owners and information about keys.
 *
 * @author Denis Bondarenko
 * Date: 05.06.2018
 * Time: 14:15
 * E-mail: DenBond7@gmail.com
 */
public class PreviewImportPgpContactFragment extends BaseFragment implements View.OnClickListener {
    private static final String KEY_EXTRA_PUBLIC_KEY_STRING
            = GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_PUBLIC_KEY_STRING",
            PreviewImportPgpContactFragment.class);

    private static final String KEY_EXTRA_PUBLIC_KEYS_FILE_URI
            = GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_PUBLIC_KEYS_FILE_URI",
            PreviewImportPgpContactFragment.class);

    private List<PublicKeyInfo> publicKeyInfoList;
    private RecyclerView recyclerViewContacts;
    private TextView buttonImportAll;
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
                break;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_parse_public_keys:
                publicKeyInfoList = (List<PublicKeyInfo>) result;
                if (!publicKeyInfoList.isEmpty()) {
                    UIUtil.exchangeViewVisibility(getContext(), false, layoutProgress, layoutContentView);
                    recyclerViewContacts.setAdapter(new ImportPgpContactsRecyclerViewAdapter(publicKeyInfoList));
                    buttonImportAll.setVisibility(publicKeyInfoList.size() > 1 ? View.VISIBLE : View.GONE);
                } else {
                    UIUtil.exchangeViewVisibility(getContext(), false, layoutProgress, emptyView);
                }
                break;

            default:
                super.handleSuccessLoaderResult(loaderId, result);
        }
    }

    @Override
    public void handleFailureLoaderResult(int loaderId, Exception e) {
        switch (loaderId) {
            case R.id.loader_id_parse_public_keys:
                if (getActivity() != null) {
                    getActivity().setResult(Activity.RESULT_CANCELED);
                    Toast.makeText(getContext(), TextUtils.isEmpty(e.getMessage())
                            ? getString(R.string.unknown_error) : e.getMessage(), Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
                break;

            default:
                super.handleFailureLoaderResult(loaderId, e);
        }
    }

    private void initViews(View root) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());

        layoutContentView = root.findViewById(R.id.layoutContentView);
        layoutProgress = root.findViewById(R.id.layoutProgress);
        buttonImportAll = root.findViewById(R.id.buttonImportAll);
        progressBar = root.findViewById(R.id.progressBar);
        buttonImportAll.setOnClickListener(this);
        recyclerViewContacts = root.findViewById(R.id.recyclerViewContacts);
        recyclerViewContacts.setHasFixedSize(true);
        recyclerViewContacts.setLayoutManager(layoutManager);
        emptyView = root.findViewById(R.id.emptyView);
    }

    private static class PublicKeysParserAsyncTask extends AsyncTask<Void, Integer, LoaderResult> {
        private final WeakReference<PreviewImportPgpContactFragment> weakReference;
        private final String publicKeysString;
        private final Uri publicKeysFileUri;

        PublicKeysParserAsyncTask(PreviewImportPgpContactFragment previewImportPgpContactFragment,
                                  String publicKeysString, Uri publicKeysFileUri) {
            this.weakReference = new WeakReference<>(previewImportPgpContactFragment);
            this.publicKeysString = publicKeysString;
            this.publicKeysFileUri = publicKeysFileUri;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (weakReference.get() != null) {
                UIUtil.exchangeViewVisibility(weakReference.get().getContext(), true,
                        weakReference.get().layoutProgress, weakReference.get().layoutContentView);
            }
        }

        @Override
        protected LoaderResult doInBackground(Void... uris) {
            String armoredKeys = publicKeysString;

            try {
                if (publicKeysFileUri != null && weakReference.get() != null) {
                    armoredKeys = GeneralUtil.readFileFromUriToString(weakReference.get().getContext(),
                            publicKeysFileUri);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return new LoaderResult(null, e);
            }

            if (!TextUtils.isEmpty(armoredKeys) && weakReference.get() != null) {
                try {
                    Js js = new Js(weakReference.get().getContext(), null);
                    String normalizedArmoredKey = js.crypto_key_normalize(armoredKeys);
                    PgpKey pgpKey = js.crypto_key_read(normalizedArmoredKey);

                    if (js.is_valid_key(pgpKey, false)) {
                        return new LoaderResult(parsePublicKeysInfo(js, armoredKeys), null);
                    } else {
                        return new LoaderResult(null, new IllegalArgumentException(
                                weakReference.get().getContext().getString(R.string.clipboard_has_wrong_structure,
                                        weakReference.get().getContext().getString(R.string.public_))));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    ExceptionUtil.handleError(e);
                    return new LoaderResult(null, e);
                }
            } else {
                return new LoaderResult(null, new NullPointerException("An input string is null!"));
            }
        }

        @Override
        protected void onPostExecute(LoaderResult loaderResult) {
            super.onPostExecute(loaderResult);
            if (weakReference.get() != null) {
                weakReference.get().handleLoaderResult(R.id.loader_id_parse_public_keys, loaderResult);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (weakReference.get() != null) {
                if (weakReference.get().progressBar.isIndeterminate()) {
                    weakReference.get().progressBar.setIndeterminate(false);
                }
                weakReference.get().progressBar.setProgress(values[0]);
            }
        }

        private List<PublicKeyInfo> parsePublicKeysInfo(Js js, @NonNull String publicKey) {
            List<PublicKeyInfo> publicKeyInfoList = new ArrayList<>();
            Set<String> emails = new HashSet<>();
            MessageBlock[] messageBlocks = js.crypto_armor_detect_blocks(publicKey);

            int blocksCount = messageBlocks.length;
            float progress;
            float lastProgress = 0;

            for (int i = 0; i < messageBlocks.length; i++) {
                MessageBlock messageBlock = messageBlocks[i];
                if (messageBlock != null && messageBlock.getType() != null) {
                    switch (messageBlock.getType()) {
                        case MessageBlock.TYPE_PGP_PUBLIC_KEY:
                            String content = messageBlock.getContent();
                            String fingerprint = js.crypto_key_fingerprint(js.crypto_key_read(content));
                            String longId = js.crypto_key_longid(fingerprint);
                            String keyWords = js.mnemonic(longId);
                            PgpKey pgpKey = js.crypto_key_read(content);
                            String keyOwner = pgpKey.getPrimaryUserId().getEmail();

                            if (emails.contains(keyOwner)) {
                                continue;
                            }

                            emails.add(keyOwner);

                            if (weakReference.get() != null) {
                                PgpContact pgpContact = new ContactsDaoSource().getPgpContact(weakReference.get()
                                        .getContext(), keyOwner);

                                PublicKeyInfo messagePartPgpPublicKey = new PublicKeyInfo(keyWords, fingerprint,
                                        keyOwner, longId, pgpContact, publicKey);

                                publicKeyInfoList.add(messagePartPgpPublicKey);
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
    }
}
