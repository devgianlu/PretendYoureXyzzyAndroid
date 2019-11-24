package com.gianlu.pretendyourexyzzy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.typography.FontsManager;
import com.gianlu.pretendyourexyzzy.LoadingActivity;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroBaseFragment;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.github.paolorotolo.appintro.model.SliderPage;

public class TutorialActivity extends AppIntro {

    @NonNull
    private static SliderPage getDefault() {
        SliderPage page = new SliderPage();
        page.setDescTypeface(FontsManager.ROBOTO_THIN);
        page.setTitleTypeface(FontsManager.ROBOTO_MEDIUM);
        return page;
    }

    @NonNull
    private Fragment newSlide(@StringRes int title, @StringRes int desc, @DrawableRes int image) {
        SliderPage page = getDefault();
        page.setTitle(getString(title));
        page.setDescription(getString(desc));
        page.setImageDrawable(image);
        return image == 0 ? NoImageAppIntroFragment.newInstance(page) : AppIntroFragment.newInstance(page);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar bar = getSupportActionBar();
        if (bar != null) bar.hide();

        showStatusBar(false);
        showSkipButton(true);
        setProgressButtonEnabled(true);

        addSlide(newSlide(R.string.tutorial_firstTitle, R.string.tutorial_firstDesc, R.drawable.ic_launcher));
        addSlide(newSlide(R.string.tutorial_secondTitle, R.string.tutorial_secondDesc, 0));
        addSlide(newSlide(R.string.tutorial_thirdTitle, R.string.tutorial_thirdDesc, 0));
        addSlide(newSlide(R.string.tutorial_fourthTitle, R.string.tutorial_fourthDesc, 0));
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        AnalyticsApplication.sendAnalytics(Utils.ACTION_SKIP_TUTORIAL);
        done();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        AnalyticsApplication.sendAnalytics(Utils.ACTION_DONE_TUTORIAL);
        done();
    }

    private void done() {
        Prefs.putBoolean(PK.FIRST_RUN, false);
        startActivity(new Intent(this, LoadingActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

    public static class NoImageAppIntroFragment extends AppIntroBaseFragment {

        static NoImageAppIntroFragment newInstance(SliderPage sliderPage) {
            NoImageAppIntroFragment slide = new NoImageAppIntroFragment();
            Bundle args = new Bundle();
            args.putString(ARG_TITLE, sliderPage.getTitleString());
            args.putString(ARG_TITLE_TYPEFACE, sliderPage.getTitleTypeface());
            args.putString(ARG_DESC, sliderPage.getDescriptionString());
            args.putString(ARG_DESC_TYPEFACE, sliderPage.getDescTypeface());
            args.putInt(ARG_BG_COLOR, sliderPage.getBgColor());
            args.putInt(ARG_TITLE_COLOR, sliderPage.getTitleColor());
            args.putInt(ARG_DESC_COLOR, sliderPage.getDescColor());
            slide.setArguments(args);

            return slide;
        }

        @Nullable
        @Override
        @SuppressWarnings("ConstantConditions")
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View v = super.onCreateView(inflater, container, savedInstanceState);
            ((ViewGroup) v).getChildAt(1).setVisibility(View.GONE);
            return v;
        }

        @Override
        protected int getLayoutId() {
            return com.github.paolorotolo.appintro.R.layout.appintro_fragment_intro;
        }
    }
}
