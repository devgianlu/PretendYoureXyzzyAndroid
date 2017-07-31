package com.gianlu.pretendyourexyzzy.Main;

import android.content.Context;
import android.support.annotation.Keep;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

@Keep
@SuppressWarnings("unused")
public class FloatingActionButtonBehavior extends CoordinatorLayout.Behavior {
    public FloatingActionButtonBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private static void kickOut(View fab) {
        fab.setTag(false);

        fab.animate()
                .translationY(fab.getHeight() * 2)
                .setDuration(150)
                .start();
    }

    private static void kickIn(View fab) {
        fab.setTag(true);

        fab.animate()
                .translationY(0)
                .setDuration(150)
                .start();
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return dependency instanceof RecyclerView;
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, View child, View directTargetChild, View target, int nestedScrollAxes) {
        return true;
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, View child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        if (child.getTag() == null)
            child.setTag(true);

        if (dyConsumed > 0 && ((boolean) child.getTag()))
            kickOut(child);
        else if (dyConsumed < 0 && (!(boolean) child.getTag()))
            kickIn(child);
    }
}
