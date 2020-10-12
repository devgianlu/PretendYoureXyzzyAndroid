package com.gianlu.pretendyourexyzzy.main;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.logs.LogsHelper;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.NewMainActivity;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewMainSettingsBinding;
import com.gianlu.pretendyourexyzzy.metrics.MetricsActivity;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;

public class NewSettingsFragment extends FragmentWithDialog implements NewMainActivity.MainFragment {
    private static final String TAG = NewSettingsFragment.class.getSimpleName();
    private static final int CONTAINER_ID = 69;
    private Page currentPage;
    private MainFragment mainFragment;
    private NewPlayersFragment playersFragment;
    private RegisteredPyx pyx;

    @NonNull
    public static NewSettingsFragment get() {
        return new NewSettingsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FrameLayout layout = new FrameLayout(inflater.getContext());
        layout.setId(CONTAINER_ID);
        return layout;
    }

    @Override
    public void onStart() {
        super.onStart();
        replaceFragment(Page.MAIN);
    }

    @Override
    public void onPyxReady(@NotNull RegisteredPyx pyx) {
        this.pyx = pyx;

        if (mainFragment != null) mainFragment.callPyxReady(pyx);
        if (playersFragment != null) playersFragment.callPyxReady(pyx);
    }

    @Override
    public void onPyxInvalid() {
        if (mainFragment != null) mainFragment.callPyxInvalid();
        if (playersFragment != null) playersFragment.callPyxInvalid();

        this.pyx = null;
    }

    @Override
    public boolean goBack() {
        if (currentPage == null || currentPage == Page.MAIN)
            return false;

        replaceFragment(Page.MAIN);
        return true;
    }

    private void replaceFragment(@NonNull Page page) {
        boolean created = false;
        ChildFragment frag;
        switch (page) {
            default:
            case MAIN:
                if (mainFragment == null) mainFragment = new MainFragment();
                frag = mainFragment;
                break;
            case PLAYERS:
                if (playersFragment == null) playersFragment = new NewPlayersFragment();
                frag = playersFragment;
                break;
        }

        getChildFragmentManager().beginTransaction()
                .replace(CONTAINER_ID, frag)
                .runOnCommit(() -> {
                    if (pyx != null) frag.callPyxReady(pyx);
                    else frag.callPyxInvalid();
                }).commit();

        currentPage = page;
    }

    private enum Page {
        MAIN, PLAYERS

        // TODO: Move metrics to a fragment?
    }

    public abstract static class ChildFragment extends FragmentWithDialog {
        private boolean callReady = false;
        private RegisteredPyx pyx = null;
        private boolean callInvalid = false;

        protected final void goBack() {
            replaceFragment(Page.MAIN);
        }

        protected final void replaceFragment(@NonNull Page page) {
            Fragment frag = getParentFragment();
            if (frag instanceof NewSettingsFragment)
                ((NewSettingsFragment) frag).replaceFragment(page);
        }

        @Override
        public void onStart() {
            super.onStart();

            if (callReady && pyx != null) onPyxReady(pyx);
            else if (callInvalid) onPyxInvalid();

            callReady = false;
            callInvalid = false;
            pyx = null;
        }

        void callPyxReady(@NonNull RegisteredPyx pyx) {
            if (isAdded()) {
                onPyxReady(pyx);
            } else {
                callInvalid = false;
                callReady = true;
                this.pyx = pyx;
            }
        }

        void callPyxInvalid() {
            if (isAdded()) {
                onPyxInvalid();
            } else {
                callInvalid = true;
                callReady = false;
                this.pyx = null;
            }
        }

        protected void onPyxReady(@NonNull RegisteredPyx pyx) {
        }

        protected void onPyxInvalid() {
        }
    }

    public static class MainFragment extends ChildFragment {
        private FragmentNewMainSettingsBinding binding;

        private void openLink(@NonNull String uri) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
            } catch (ActivityNotFoundException ex) {
                showToast(Toaster.build().message(R.string.missingWebBrowser));
            }
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            binding = FragmentNewMainSettingsBinding.inflate(inflater, container, false);

            binding.settingsPlayers.setOnClickListener(v -> replaceFragment(Page.PLAYERS));
            binding.settingsMetrics.setOnClickListener(v -> MetricsActivity.startActivity(requireContext()));

            binding.settingsApp.setSubtitle(R.string.devgianluCopyright, Calendar.getInstance().get(Calendar.YEAR));
            binding.settingsVersion.setSubtitle(getVersion());
            binding.settingsDeveloper.setOnClickListener((v) -> openLink("https://gianlu.xyz"));
            binding.settingsEmailMe.setOnClickListener((v) -> LogsHelper.sendEmail(requireContext(), null));
            binding.settingsOpenSource.setOnClickListener((v) -> openLink("https://github.com/devgianlu/PretendYoureXyzzyAndroid"));
            binding.settingsTranslators.setOnClickListener((v) -> {
                // TODO: Translators
            });

            binding.settingsPrefGeneral.setOnClickListener(v -> {
                // TODO: General preferences
            });
            binding.settingsPrefOverloaded.setOnClickListener(v -> {
                // TODO: Overloaded preferences
            });

            binding.settingsRate.setOnClickListener(v -> {
                String pkg = requireContext().getPackageName();

                try {
                    openLink("market://details?id=" + pkg);
                } catch (ActivityNotFoundException ex) {
                    openLink("https://play.google.com/store/apps/details?id=" + pkg);
                }
            });
            binding.settingsDonate.setOnClickListener(v -> {
                // TODO: Donate
            });

            binding.settingsSendLogs.setOnClickListener(v -> {
                LogsHelper.sendEmail(requireContext(), null);
            });
            binding.settingsExportLogs.setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                Exception logsException = LogsHelper.exportLogFiles(requireContext(), shareIntent);
                if (logsException != null) {
                    showToast(Toaster.build().message(R.string.noLogs));
                    Log.e(TAG, "Failed exporting log files.", logsException);
                    return;
                }

                shareIntent.setType("text/plain");
                startActivity(Intent.createChooser(shareIntent, getString(R.string.exportLogFiles)));
            });

            return binding.getRoot();
        }

        @NotNull
        private String getVersion() {
            try {
                return requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException ex) {
                return getString(R.string.unknown);
            }
        }
    }
}
