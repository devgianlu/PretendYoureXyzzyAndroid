package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.gianlu.commonutils.CommonUtils;

public final class PreferenceItemView extends LinearLayout {
    public PreferenceItemView(Context context) {
        this(context, null, 0);
    }

    public PreferenceItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreferenceItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        View.inflate(context, R.layout.view_preference_item, this);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PreferenceItemView, defStyleAttr, 0);
        try {
            Drawable icon = a.getDrawable(R.styleable.PreferenceItemView_icon);
            ((ImageView) findViewById(R.id.preferenceItem_icon)).setImageDrawable(icon);

            String title = a.getString(R.styleable.PreferenceItemView_title);
            ((TextView) findViewById(R.id.preferenceItem_title)).setText(title);

            String subtitle = a.getString(R.styleable.PreferenceItemView_subtitle);
            setSubtitle(subtitle);
        } finally {
            a.recycle();
        }

        setOrientation(HORIZONTAL);
        setBackground(CommonUtils.resolveAttrAsDrawable(context, android.R.attr.selectableItemBackground));

        int dp4 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, context.getResources().getDisplayMetrics());
        setPaddingRelative(dp4 * 4, dp4, dp4 * 4, dp4);
    }

    public void setSubtitle(@StringRes int textRes, Object... args) {
        setSubtitle(getContext().getString(textRes, args));
    }

    public void setSubtitle(String text) {
        if (text == null) {
            findViewById(R.id.preferenceItem_subtitle).setVisibility(GONE);
        } else {
            findViewById(R.id.preferenceItem_subtitle).setVisibility(VISIBLE);
            ((TextView) findViewById(R.id.preferenceItem_subtitle)).setText(text);
        }
    }
}
