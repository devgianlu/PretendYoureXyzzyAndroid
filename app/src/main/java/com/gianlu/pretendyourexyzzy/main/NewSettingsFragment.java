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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.logs.LogsHelper;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.NewMainActivity;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewSettingsBinding;
import com.gianlu.pretendyourexyzzy.metrics.MetricsActivity;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;

public class NewSettingsFragment extends FragmentWithDialog implements NewMainActivity.MainFragment {
    private static final String TAG = NewSettingsFragment.class.getSimpleName();
    private FragmentNewSettingsBinding binding;

    @NonNull
    public static NewSettingsFragment get() {
        return new NewSettingsFragment();
    }

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
        binding = FragmentNewSettingsBinding.inflate(inflater, container, false);

        binding.settingsPlayers.setOnClickListener(v -> {
            // TODO: Players
        });
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

    @Override
    public void onPyxReady(@NotNull RegisteredPyx pyx) {
    }

    @Override
    public void onPyxInvalid() {
    }
}
