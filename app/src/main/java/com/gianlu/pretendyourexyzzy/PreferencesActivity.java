package com.gianlu.pretendyourexyzzy;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.commonutils.LogsActivity;
import com.gianlu.commonutils.Preferences.AppCompatPreferenceActivity;
import com.gianlu.commonutils.Preferences.AppCompatPreferenceFragment;
import com.gianlu.commonutils.Preferences.BaseAboutFragment;
import com.gianlu.commonutils.Preferences.BaseThirdPartProjectsFragment;
import com.gianlu.commonutils.Tutorial.TutorialManager;
import com.gianlu.pretendyourexyzzy.SpareActivities.TutorialActivity;

import java.util.List;

public class PreferencesActivity extends AppCompatPreferenceActivity {

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    @Override
    public void onHeaderClick(Header header, int position) {
        if (header.iconRes == R.drawable.baseline_announcement_24) {
            startActivity(new Intent(this, LogsActivity.class));
            return;
        }

        super.onHeaderClick(header, position);
    }

    public static class GeneralFragment extends AppCompatPreferenceFragment {

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            getActivity().setTitle(R.string.general);
            setHasOptionsMenu(true);

            findPreference("showTutorial").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(getActivity(), TutorialActivity.class));
                    return true;
                }
            });

            findPreference("restartTutorial").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    TutorialManager.restartTutorial(getActivity());
                    return true;
                }
            });
        }

        @Override
        protected Class getParent() {
            return PreferencesActivity.class;
        }
    }

    public static class ThirdPartFragment extends BaseThirdPartProjectsFragment {
        @NonNull
        @Override
        protected ThirdPartProject[] getProjects() {
            return new ThirdPartProject[]{
                    new ThirdPartProject(R.string.materialRatingBar, R.string.materialRatingBar_details, ThirdPartProject.License.APACHE),
                    new ThirdPartProject(R.string.tapTargetView, R.string.tapTargetView_details, ThirdPartProject.License.APACHE),
                    new ThirdPartProject(R.string.appIntro, R.string.appIntro_details, ThirdPartProject.License.APACHE),
                    new ThirdPartProject(R.string.glide, R.string.glide_details, ThirdPartProject.License.APACHE)
            };
        }

        @Override
        protected Class getParent() {
            return PreferencesActivity.class;
        }
    }

    public static class AboutFragment extends BaseAboutFragment {
        @Override
        protected int getAppNameRes() {
            return R.string.app_name;
        }

        @NonNull
        @Override
        protected String getPackageName() {
            return "com.gianlu.pretendyourexyzzy";
        }

        @Nullable
        @Override
        protected Uri getOpenSourceUrl() {
            return null;
        }


        @Override
        protected Class getParent() {
            return PreferencesActivity.class;
        }
    }
}
