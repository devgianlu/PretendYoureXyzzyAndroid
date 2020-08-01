package com.gianlu.pretendyourexyzzy.main;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Layout;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.tutorial.BaseTutorial;
import com.gianlu.commonutils.tutorial.TutorialManager;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksAdapter;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.CustomDeck;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.FloatingCustomDeck;
import com.gianlu.pretendyourexyzzy.customdecks.EditCustomDeckActivity;
import com.gianlu.pretendyourexyzzy.customdecks.ViewCustomDeckActivity;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;
import com.gianlu.pretendyourexyzzy.overloaded.SyncUtils;
import com.gianlu.pretendyourexyzzy.tutorial.CustomDecksTutorial;
import com.gianlu.pretendyourexyzzy.tutorial.Discovery;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import me.toptas.fancyshowcase.FocusShape;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;

public class CustomDecksFragment extends FragmentWithDialog implements OverloadedSyncApi.SyncStatusListener, CustomDecksAdapter.Listener, TutorialManager.Listener {
    private static final int RC_IMPORT_JSON = 2;
    private static final String TAG = CustomDecksFragment.class.getSimpleName();
    private RecyclerMessageView rmv;
    private CustomDecksDatabase db;
    private TextView syncStatus;
    private TutorialManager tutorialManager;
    private FloatingActionsMenu fab;

    @NonNull
    public static CustomDecksFragment getInstance() {
        return new CustomDecksFragment();
    }

    @Nullable
    @Override
    @SuppressWarnings("WrongConstant")
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        CoordinatorLayout layout = (CoordinatorLayout) inflater.inflate(R.layout.fragment_custom_decks, container, false);
        db = CustomDecksDatabase.get(requireContext());

        syncStatus = layout.findViewById(R.id.customDecks_sync);
        rmv = layout.findViewById(R.id.customDecks_list);
        rmv.linearLayoutManager(RecyclerView.VERTICAL, false);

        if (OverloadedUtils.isSignedIn())
            rmv.enableSwipeRefresh(this::refreshSync, R.color.appColorBright);
        else
            rmv.disableSwipeRefresh();


        fab = layout.findViewById(R.id.customDecks_fab);
        FloatingActionButton importDeck = layout.findViewById(R.id.customDecksFab_import);
        importDeck.setOnClickListener(v -> {
            fab.collapse();

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
            builder.setTitle(R.string.importCustomDeck)
                    .setMessage(R.string.importCustomDeck_details)
                    .setPositiveButton(android.R.string.ok,
                            (dialog, which) -> startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_GET_CONTENT).setType("*/*"), "Pick a JSON file..."), RC_IMPORT_JSON));

            Dialog dialog = builder.create();
            dialog.setOnShowListener(d -> {
                TextView text = dialog.getWindow().findViewById(android.R.id.message);
                text.setAutoLinkMask(Linkify.WEB_URLS);
                text.setMovementMethod(LinkMovementMethod.getInstance());

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    text.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
            });

            showDialog(dialog);
        });

        FloatingActionButton addDeck = layout.findViewById(R.id.customDecksFab_add);
        addDeck.setOnClickListener(v -> {
            EditCustomDeckActivity.startActivityNew(requireContext());
            fab.collapse();
        });

        FloatingActionButton recoverDeck = layout.findViewById(R.id.customDecksFab_recover);
        recoverDeck.setOnClickListener(v -> {
            EditText input = new EditText(requireContext());
            input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5), new InputFilter.AllCaps()});

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
            builder.setTitle(R.string.recoverCardcastDeck).setView(input)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.recover, (dialog, which) -> {
                        String code = input.getText().toString();
                        if (!code.matches("[A-Z0-9]{5}")) {
                            showToast(Toaster.build().message(R.string.invalidDeckCode));
                            return;
                        }

                        try {
                            recoverCardcastDeck(code);
                        } catch (LevelMismatchException ignored) {
                        }
                    });
            showDialog(builder);
            fab.collapse();
        });

        tutorialManager = new TutorialManager(this, Discovery.CUSTOM_DECKS);
        return layout;
    }

    private void recoverCardcastDeck(@NonNull String code) throws LevelMismatchException {
        showProgress(R.string.loading);
        Pyx.get().recoverCardcastDeck(code, requireContext(), new Pyx.OnRecoverResult() {
            @Override
            public void onDone(@NonNull File tmpFile) {
                dismissDialog();
                EditCustomDeckActivity.startActivityImport(requireContext(), false, tmpFile);
            }

            @Override
            public void notFound() {
                dismissDialog();
                showToast(Toaster.build().message(R.string.recoverDeckNotFound));
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Log.e(TAG, "Cannot recover deck.", ex);
                showToast(Toaster.build().message(R.string.failedRecoveringCardcastDeck));
                dismissDialog();
            }
        });
    }

    private void refreshSync() {
        if (getContext() == null)
            return;

        SyncUtils.syncCustomDecks(requireContext(), this::refreshList);
        SyncUtils.syncStarredCustomDecks(requireContext(), this::refreshList);
    }

    private void refreshList() {
        List<FloatingCustomDeck> decks = db.getAllDecks();
        rmv.loadListData(new CustomDecksAdapter(requireContext(), decks, this), false);
        if (decks.isEmpty()) rmv.showInfo(R.string.noCustomDecks_create);
        else rmv.showList();
    }

    @Override
    public void onStart() {
        super.onStart();
        OverloadedSyncApi.get().addSyncListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
        tryShowingTutorial();
    }

    public void tryShowingTutorial() {
        if (tutorialManager != null && getActivity() != null)
            tutorialManager.tryShowingTutorials(getActivity());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RC_IMPORT_JSON) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                try {
                    InputStream in = requireContext().getContentResolver().openInputStream(data.getData());
                    if (in == null) return;

                    File tmpFile = new File(requireContext().getCacheDir(), CommonUtils.randomString(6, "abcdefghijklmnopqrstuvwxyz"));
                    CommonUtils.copy(in, new FileOutputStream(tmpFile));
                    EditCustomDeckActivity.startActivityImport(requireContext(), true, tmpFile);
                } catch (IOException ex) {
                    Log.e(TAG, "Failed importing JSON file: " + data, ex);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void syncStatusUpdated(@NonNull OverloadedSyncApi.SyncProduct product, boolean isSyncing, boolean error) {
        if (syncStatus != null && (product == OverloadedSyncApi.SyncProduct.CUSTOM_DECKS || product == OverloadedSyncApi.SyncProduct.STARRED_CUSTOM_DECKS))
            SyncUtils.updateSyncText(syncStatus, product, isSyncing, error);
    }

    @Override
    public void onCustomDeckSelected(@NonNull FloatingCustomDeck deck) {
        if (deck instanceof CustomDeck)
            EditCustomDeckActivity.startActivityEdit(requireContext(), (CustomDeck) deck);
        else if (deck instanceof CustomDecksDatabase.StarredDeck && deck.owner != null)
            ViewCustomDeckActivity.startActivity(requireContext(), deck.owner, deck.name, ((CustomDecksDatabase.StarredDeck) deck).shareCode);
    }

    @Override
    public boolean canShow(@NonNull BaseTutorial tutorial) {
        return tutorial instanceof CustomDecksTutorial && getActivity() != null && CommonUtils.isVisible(this);
    }

    @Override
    public boolean buildSequence(@NonNull BaseTutorial tutorial) {
        if (!(tutorial instanceof CustomDecksTutorial))
            return false;

        tutorial.add(tutorial.forView(fab.getChildAt(0), R.string.tutorial_addCustomDeck)
                .enableAutoTextPosition()
                .fitSystemWindows(true)
                .focusShape(FocusShape.CIRCLE));
        return true;
    }
}
