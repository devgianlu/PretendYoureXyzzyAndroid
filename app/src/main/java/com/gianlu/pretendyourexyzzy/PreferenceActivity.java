package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutItem;
import com.gianlu.commonutils.Preferences.BasePreferenceActivity;
import com.gianlu.commonutils.Preferences.BasePreferenceFragment;
import com.gianlu.commonutils.Preferences.MaterialAboutPreferenceItem;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.pretendyourexyzzy.SpareActivities.TutorialActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.yarolegovich.mp.MaterialCheckboxPreference;
import com.yarolegovich.mp.MaterialStandardPreference;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class PreferenceActivity extends BasePreferenceActivity {
    @NonNull
    @Override
    protected List<MaterialAboutPreferenceItem> getPreferencesItems() {
        return Collections.singletonList(new MaterialAboutPreferenceItem(R.string.general, R.drawable.baseline_settings_24, GeneralFragment.class));
    }

    @Override
    protected int getAppIconRes() {
        return R.mipmap.ic_launcher;
    }

    @NonNull
    @Override
    protected List<MaterialAboutItem> customizeTutorialCard() {
        return Collections.singletonList(new MaterialAboutActionItem(R.string.showBeginnerTutorial, R.string.showBeginnerTutorial_summary, 0, () -> startActivity(new Intent(PreferenceActivity.this, TutorialActivity.class))));
    }

    @Override
    protected boolean hasTutorial() {
        return true;
    }

    @Nullable
    @Override
    protected String getOpenSourceUrl() {
        return "https://github.com/devgianlu/PretendYoureXyzzyAndroid";
    }

    @Override
    protected boolean disableOtherDonationsOnGooglePlay() {
        return true;
    }

    public static class GeneralFragment extends BasePreferenceFragment {

        private void showUnblockDialog(@NonNull Context context) {
            String[] entries = Prefs.getSet(PK.BLOCKED_USERS, new HashSet<>()).toArray(new String[0]);
            boolean[] checked = new boolean[entries.length];
            for (int i = 0; i < checked.length; i++) checked[i] = false;

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

        @Override
        protected void buildPreferences(@NonNull Context context) {
            MaterialCheckboxPreference nightMode = new MaterialCheckboxPreference.Builder(context)
                    .defaultValue(PK.NIGHT_MODE.fallback())
                    .key(PK.NIGHT_MODE.key())
                    .build();
            nightMode.setTitle(R.string.prefs_nightMode);
            nightMode.setSummary(R.string.prefs_nightMode_summary);
            addPreference(nightMode);

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
}
