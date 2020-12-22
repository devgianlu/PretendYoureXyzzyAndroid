package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.pretendyourexyzzy.databinding.ViewPreferenceItemBinding;

public final class PreferenceItemView extends LinearLayout {
    private final ViewPreferenceItemBinding binding;

    public PreferenceItemView(Context context) {
        this(context, null, 0);
    }

    public PreferenceItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreferenceItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        binding = ViewPreferenceItemBinding.inflate(LayoutInflater.from(context), this);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PreferenceItemView, defStyleAttr, 0);
        try {
            Drawable icon = a.getDrawable(R.styleable.PreferenceItemView_icon);
            binding.preferenceItemIcon.setImageDrawable(icon);

            String title = a.getString(R.styleable.PreferenceItemView_title);
            binding.preferenceItemTitle.setText(title);
            binding.preferenceItemTitle.setTextColor(a.getColor(R.styleable.PreferenceItemView_titleTextColor, Color.BLACK));

            binding.preferenceItemSubtitle.setTextColor(a.getColor(R.styleable.PreferenceItemView_subtitleTextColor, Color.GRAY));

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
            binding.preferenceItemSubtitle.setVisibility(GONE);
        } else {
            binding.preferenceItemSubtitle.setVisibility(VISIBLE);
            binding.preferenceItemSubtitle.setText(text);
        }
    }
}
