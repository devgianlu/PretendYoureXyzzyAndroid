package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import com.gianlu.pretendyourexyzzy.CardViews.GameCardView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public class CardsGridFixer implements View.OnLayoutChangeListener {
    private final int mCardWidth;

    public CardsGridFixer(@NonNull Context context) {
        mCardWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, GameCardView.WIDTH_DIP, context.getResources().getDisplayMetrics());
    }

    @Override
    public void onLayoutChange(final View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (!(v instanceof RecyclerView))
            throw new IllegalStateException("CardsGridFixer must be applied to the RecyclerView!");

        StaggeredGridLayoutManager lm;
        if (!(((RecyclerView) v).getLayoutManager() instanceof StaggeredGridLayoutManager))
            throw new IllegalStateException("Must be using a StaggeredGridLayoutManager!");

        lm = (StaggeredGridLayoutManager) ((RecyclerView) v).getLayoutManager();
        int spanCount = lm.getSpanCount();

        final View target;
        final ViewGroup.MarginLayoutParams params;
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            target = v;
            params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
        } else if (((View) v.getParent()).getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            target = (View) v.getParent();
            params = (ViewGroup.MarginLayoutParams) target.getLayoutParams();
        } else {
            params = null;
            target = null;
        }

        if (params != null) {
            final int padding = (v.getMeasuredWidth() - mCardWidth * spanCount) / (spanCount + 1);

            target.post(() -> {
                params.setMarginStart(padding);
                target.setLayoutParams(params);
            });

            v.removeOnLayoutChangeListener(this);
        }
    }
}
