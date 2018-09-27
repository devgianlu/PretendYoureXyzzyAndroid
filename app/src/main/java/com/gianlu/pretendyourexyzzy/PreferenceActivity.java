package com.gianlu.pretendyourexyzzy;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutItemOnClickAction;
import com.gianlu.commonutils.Preferences.BasePreferenceActivity;
import com.gianlu.commonutils.Preferences.MaterialAboutPreferenceItem;
import com.gianlu.pretendyourexyzzy.SpareActivities.TutorialActivity;

import java.util.Collections;
import java.util.List;

public class PreferenceActivity extends BasePreferenceActivity {
    @NonNull
    @Override
    protected List<MaterialAboutPreferenceItem> getPreferencesItems() {
        return Collections.emptyList();
    }

    @Override
    protected int getAppIconRes() {
        return R.mipmap.ic_launcher;
    }

    @NonNull
    @Override
    protected List<MaterialAboutItem> customizeTutorialCard() {
        return Collections.<MaterialAboutItem>singletonList(new MaterialAboutActionItem(R.string.showBeginnerTutorial, R.string.showBeginnerTutorial_summary, 0, new MaterialAboutItemOnClickAction() {
            @Override
            public void onClick() {
                startActivity(new Intent(PreferenceActivity.this, TutorialActivity.class));
            }
        }));
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
    protected boolean disablePayPalOnGooglePlay() {
        return true;
    }
}
