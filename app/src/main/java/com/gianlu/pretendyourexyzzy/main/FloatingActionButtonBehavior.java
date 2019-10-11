package com.gianlu.pretendyourexyzzy.main;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

@Keep
public class FloatingActionButtonBehavior extends CoordinatorLayout.Behavior<FloatingActionButton> {
    private static final int DURATION = 150;

    public FloatingActionButtonBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private static void hide(FloatingActionButton fab) {
        fab.setTag(false);
        fab.animate().scaleX(0).scaleY(0).setDuration(DURATION).start();
        fab.setClickable(false);
    }

    private static void show(FloatingActionButton fab) {
        fab.setTag(true);
        fab.animate().scaleX(1).scaleY(1).setDuration(DURATION).start();
        fab.setClickable(true);
    }

    @Override
    public boolean layoutDependsOn(@NonNull CoordinatorLayout parent, @NonNull FloatingActionButton child, @NonNull View dependency) {
        return dependency instanceof RecyclerView;
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull FloatingActionButton child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
        return true;
    }


    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull FloatingActionButton child, @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        if (child.getTag() == null) child.setTag(true);
        if (dyConsumed > 0 && ((boolean) child.getTag())) hide(child);
        else if (dyConsumed < 0 && (!(boolean) child.getTag())) show(child);
    }
}
