package com.gianlu.pretendyourexyzzy.main;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.gianlu.commonutils.FossUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.logs.LogsHelper;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.commonutils.preferences.PreferencesBillingHelper;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.preferences.Translators;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.BlockedUsers;
import com.gianlu.pretendyourexyzzy.NewMainActivity;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewMainSettingsBinding;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewPrefsSettingsBinding;
import com.gianlu.pretendyourexyzzy.dialogs.Dialogs;
import com.gianlu.pretendyourexyzzy.main.settings.NewPlayersFragment;
import com.gianlu.pretendyourexyzzy.main.settings.OverloadedPrefsFragment;
import com.gianlu.pretendyourexyzzy.metrics.MetricsFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.yarolegovich.mp.AbsMaterialCheckablePreference;
import com.yarolegovich.mp.AbsMaterialPreference;
import com.yarolegovich.mp.MaterialCheckboxPreference;
import com.yarolegovich.mp.MaterialPreferenceScreen;
import com.yarolegovich.mp.MaterialStandardPreference;
import com.yarolegovich.mp.io.MaterialPreferences;
import com.yarolegovich.mp.io.StorageModule;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;

public class NewSettingsFragment extends NewMainActivity.ChildFragment {
    private static final String TAG = NewSettingsFragment.class.getSimpleName();
    private static final int CONTAINER_ID = 69;
    private final EnumMap<Page, ChildFragment> fragments = new EnumMap<>(Page.class);
    private Page currentPage;
    private RegisteredPyx pyx;
    private Page goToPage = null;
    private boolean mStarted = false;

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

    public void changePage(@NonNull Page page) {
        if (mStarted) {
            goToPage = null;
            replaceFragment(page);
        } else {
            goToPage = page;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mStarted = true;
        replaceFragment(goToPage != null ? goToPage : Page.MAIN);
    }

    @Override
    public void onPyxReady(@NotNull RegisteredPyx pyx) {
        this.pyx = pyx;

        for (ChildFragment frag : fragments.values())
            frag.callPyxReady(pyx);
    }

    @Override
    public void onPyxInvalid(@Nullable Exception ex) {
        for (ChildFragment frag : fragments.values())
            frag.callPyxInvalid(ex);

        this.pyx = null;
    }

    @Override
    public boolean goBack() {
        if (currentPage == null || currentPage == Page.MAIN)
            return false;

        ChildFragment frag = fragments.get(currentPage);
        if (frag == null) return false;

        return frag.goBack();
    }

    @NonNull
    private ChildFragment getFragment(@NonNull Page page) {
        ChildFragment frag = fragments.get(page);
        if (frag == null) {
            frag = page.init();
            fragments.put(page, frag);
        }

        return frag;
    }

    private void replaceFragment(@NonNull Page page) {
        ChildFragment frag = getFragment(page);
        getChildFragmentManager().beginTransaction()
                .replace(CONTAINER_ID, frag)
                .runOnCommit(() -> {
                    if (pyx != null) frag.callPyxReady(pyx);
                    else frag.callPyxInvalid(null);
                }).commit();

        currentPage = page;
    }

    public enum Page {
        MAIN(MainFragment.class), PLAYERS(NewPlayersFragment.class), TRANSLATORS(TranslatorsFragment.class),
        PREFS_GENERAL(GeneralPrefsFragment.class), PREFS_OVERLOADED(OverloadedPrefsFragment.class),
        METRICS(MetricsFragment.class);

        private final Class<? extends ChildFragment> clazz;

        Page(Class<? extends ChildFragment> clazz) {
            this.clazz = clazz;
        }

        @NonNull
        public ChildFragment init() {
            try {
                return clazz.newInstance();
            } catch (IllegalAccessException | java.lang.InstantiationException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    public abstract static class ChildFragment extends FragmentWithDialog {
        private boolean mStarted = false;
        private boolean callReady = false;
        private boolean callInvalid = false;
        private RegisteredPyx pyx = null;
        private Exception ex = null;

        protected final void openLink(@NonNull String uri) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
            } catch (ActivityNotFoundException ex) {
                showToast(Toaster.build().message(R.string.missingWebBrowser));
            }
        }

        protected boolean goBack() {
            replaceFragment(Page.MAIN);
            return true;
        }

        protected final void replaceFragment(@NonNull Page page) {
            Fragment frag = getParentFragment();
            if (frag instanceof NewSettingsFragment)
                ((NewSettingsFragment) frag).replaceFragment(page);
        }

        @Override
        @CallSuper
        public void onStart() {
            super.onStart();
            mStarted = true;

            if (callReady && pyx != null) onPyxReady(pyx);
            else if (callInvalid) onPyxInvalid(ex);

            callReady = false;
            callInvalid = false;
            pyx = null;
        }

        final void callPyxReady(@NonNull RegisteredPyx pyx) {
            if (mStarted && isAdded()) {
                onPyxReady(pyx);
            } else {
                callInvalid = false;
                callReady = true;
                this.pyx = pyx;
            }
        }

        final void callPyxInvalid(@Nullable Exception ex) {
            if (mStarted && isAdded()) {
                onPyxInvalid(ex);
            } else {
                callInvalid = true;
                callReady = false;
                this.pyx = null;
                this.ex = ex;
            }
        }

        protected void onPyxReady(@NonNull RegisteredPyx pyx) {
        }

        protected void onPyxInvalid(@Nullable Exception ex) {
        }
    }

    public abstract static class PrefsChildFragment extends ChildFragment {
        private MaterialPreferenceScreen lastCategory;
        private StorageModule storageModule;
        private FragmentNewPrefsSettingsBinding binding;

        protected abstract void buildPreferences(@NonNull Context context);

        public final void addCategory(@StringRes int title) {
            MaterialPreferenceScreen category = new MaterialPreferenceScreen(requireContext());
            category.useLinearLayout();
            category.setTag(title);
            category.addView(SuperTextView.builder(requireContext())
                    .text(title).typeface(R.font.montserrat_semibold).sizeSp(18)
                    .colorRes(R.color.appColor_600).paddingDp(16, 8, 0, 0)
                    .build());
            binding.settingsPrefsFragmentScreen.addView(lastCategory = category);
        }

        public final void removeCategory(@StringRes int titleRes) {
            ViewGroup parent = (ViewGroup) binding.settingsPrefsFragmentScreen.getChildAt(0);
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                if (child instanceof MaterialPreferenceScreen && (int) child.getTag() == titleRes) {
                    parent.removeViewAt(i);
                    break;
                }
            }
        }

        public final void addPreference(@NonNull AbsMaterialPreference<?> preference) {
            preference.setStorageModule(storageModule);
            if (lastCategory == null) binding.settingsPrefsFragmentScreen.addView(preference);
            else lastCategory.addView(preference);
        }

        public final void removePreference(@NonNull AbsMaterialPreference<?> preference) {
            if (lastCategory == null) binding.settingsPrefsFragmentScreen.removeView(preference);
            else lastCategory.removeView(preference);
        }

        public final void clearPreferences() {
            lastCategory = null;
            binding.settingsPrefsFragmentScreen.useLinearLayout();
        }

        public final void rebuildPreferences() {
            clearPreferences();
            buildPreferences(binding.settingsPrefsFragmentScreen.getContext());
        }

        public final void addController(AbsMaterialCheckablePreference controller, boolean showWhenChecked, AbsMaterialPreference<?>... dependent) {
            binding.settingsPrefsFragmentScreen.setVisibilityController(controller, dependent, showWhenChecked);
        }

        @Override
        public void onResume() {
            super.onResume();
            rebuildPreferences();
        }

        @NonNull
        @Override
        public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            binding = FragmentNewPrefsSettingsBinding.inflate(inflater, container, false);
            binding.settingsPrefsFragmentTitle.setText(getTitleRes());
            binding.settingsPrefsFragmentBack.setOnClickListener((v) -> goBack());

            storageModule = MaterialPreferences.getStorageModule(requireContext());
            return binding.getRoot();
        }

        @StringRes
        public abstract int getTitleRes();
    }

    public static class MainFragment extends ChildFragment {
        private PreferencesBillingHelper billingHelper;
        private FragmentNewMainSettingsBinding binding;

        @Override
        protected void onPyxReady(@NonNull RegisteredPyx pyx) {
            binding.settingsPlayers.setVisibility(View.VISIBLE);
            binding.settingsMetrics.setVisibility(pyx.hasMetrics() ? View.VISIBLE : View.GONE);
        }

        @Override
        protected void onPyxInvalid(@Nullable Exception ex) {
            binding.settingsMetrics.setVisibility(View.GONE);
            binding.settingsPlayers.setVisibility(View.GONE);
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            binding = FragmentNewMainSettingsBinding.inflate(inflater, container, false);

            binding.settingsPlayers.setOnClickListener(v -> replaceFragment(Page.PLAYERS));
            binding.settingsMetrics.setOnClickListener(v -> replaceFragment(Page.METRICS));
            binding.settingsMetrics.setVisibility(View.GONE);

            binding.settingsApp.setSubtitle(R.string.devgianluCopyright, Calendar.getInstance().get(Calendar.YEAR));
            binding.settingsVersion.setSubtitle(getVersion());
            binding.settingsDeveloper.setOnClickListener((v) -> openLink("https://gianlu.xyz"));
            binding.settingsOpenSource.setOnClickListener((v) -> openLink("https://github.com/devgianlu/PretendYoureXyzzyAndroid"));
            binding.settingsTranslators.setOnClickListener((v) -> replaceFragment(Page.TRANSLATORS));

            binding.settingsPrefGeneral.setOnClickListener(v -> replaceFragment(Page.PREFS_GENERAL));
            binding.settingsPrefOverloaded.setOnClickListener(v -> replaceFragment(Page.PREFS_OVERLOADED));

            binding.settingsRate.setOnClickListener(v -> {
                String pkg = requireContext().getPackageName();

                try {
                    openLink("market://details?id=" + pkg);
                } catch (ActivityNotFoundException ex) {
                    openLink("https://play.google.com/store/apps/details?id=" + pkg);
                }
            });
            binding.settingsDonate.setOnClickListener(v -> donate());

            binding.settingsOpenIssue.setOnClickListener(v -> LogsHelper.openGithubIssue(requireContext(), "PretendYoureXyzzyAndroid", null));
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

        @Override
        public void onStart() {
            super.onStart();

            if (billingHelper == null && getActivity() != null && FossUtils.hasGoogleBilling()) {
                billingHelper = new PreferencesBillingHelper(this, "donation.lemonade", "donation.coffee", "donation.hamburger", "donation.pizza", "donation.sushi", "donation.champagne");
                billingHelper.onStart(requireActivity());
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (billingHelper != null) billingHelper.onDestroy();
        }

        private void donate() {
            if (billingHelper != null && getActivity() != null)
                billingHelper.donate(requireActivity(), false);
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

    public static class GeneralPrefsFragment extends PrefsChildFragment {

        private void showUnblockDialog(@NonNull Context context) {
            String[] entries = Prefs.getSet(PK.BLOCKED_USERS, new HashSet<>()).toArray(new String[0]);
            boolean[] checked = new boolean[entries.length];

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle(R.string.unblockUser)
                    .setMultiChoiceItems(entries, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                    .setPositiveButton(R.string.unblock, (dialog, which) -> {
                        for (int i = 0; i < checked.length; i++) {
                            if (checked[i]) BlockedUsers.unblock(entries[i]);
                        }

                        if (Prefs.isSetEmpty(PK.BLOCKED_USERS)) onBackPressed();
                    }).setNegativeButton(android.R.string.cancel, null);

            showDialog(builder);
        }

        private void showEditCustomServersDialog(@NotNull Context context) {
            List<Pyx.Server> servers = Pyx.Server.loadCustomServers();
            String[] serversStr = new String[servers.size()];
            for (int i = 0; i < servers.size(); i++)
                serversStr[i] = servers.get(i).name;

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle(R.string.customServers)
                    .setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, serversStr), (di, which) -> {
                        di.dismiss();
                        showDialog(Dialogs.addServer(context, servers.get(which)));
                    })
                    .setNeutralButton(R.string.addServer, (di, which) -> {
                        di.dismiss();
                        showDialog(Dialogs.addServer(context, null));
                    })
                    .setNegativeButton(android.R.string.cancel, null);

            showDialog(builder);
        }

        @Override
        protected void buildPreferences(@NonNull Context context) {
            MaterialCheckboxPreference nightMode = new MaterialCheckboxPreference.Builder(context)
                    .defaultValue(PK.NIGHT_MODE.fallback())
                    .key(PK.NIGHT_MODE.key())
                    .build();
            nightMode.setTitle(R.string.prefs_nightMode);
            nightMode.setSummary(R.string.prefs_nightMode_summary);
            addPreference(nightMode);

            MaterialStandardPreference customServers = new MaterialStandardPreference(context);
            customServers.setTitle(R.string.customServers);
            customServers.setSummary(R.string.customServers_summary);
            customServers.setOnClickListener(v -> showEditCustomServersDialog(context));
            addPreference(customServers);

            if (!Prefs.isSetEmpty(PK.BLOCKED_USERS)) {
                MaterialStandardPreference unblock = new MaterialStandardPreference(context);
                unblock.setTitle(R.string.unblockUser);
                unblock.setSummary(R.string.unblockUser_summary);
                unblock.setOnClickListener(v -> showUnblockDialog(context));
                addPreference(unblock);
            }
        }

        @Override
        public int getTitleRes() {
            return R.string.general;
        }
    }

    public static class TranslatorsFragment extends PrefsChildFragment {

        @Override
        protected void buildPreferences(@NonNull Context context) {
            for (Translators.Item item : Translators.load(context)) {
                MaterialStandardPreference pref = new MaterialStandardPreference(context);
                pref.setTitle(item.name);
                pref.setSummary(item.languages);
                pref.setOnClickListener(v -> openLink(item.link));
                addPreference(pref);
            }
        }

        @Override
        public int getTitleRes() {
            return com.gianlu.commonutils.R.string.translators;
        }
    }
}
