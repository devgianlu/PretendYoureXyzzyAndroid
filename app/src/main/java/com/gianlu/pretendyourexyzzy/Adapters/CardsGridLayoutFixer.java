package com.gianlu.pretendyourexyzzy.Adapters;

import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;

import com.gianlu.pretendyourexyzzy.CardViews.GameCardView;

public class CardsGridLayoutFixer implements View.OnLayoutChangeListener { // FIXME: Should be done with offsetChildrenHorizontal (??)
    public CardsGridLayoutFixer() {
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (!(v instanceof RecyclerView))
            throw new IllegalStateException("CardsGridLayoutFixer must be applied to the RecyclerView!");

        int screenWidth = v.getMeasuredWidth();
        int cardWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, GameCardView.WIDTH_DIP, v.getResources().getDisplayMetrics());
        int padding = (screenWidth - cardWidth * 2) / 3;
        v.setPaddingRelative(padding, 0, 0, 0);
        v.removeOnLayoutChangeListener(this);
    }
}
