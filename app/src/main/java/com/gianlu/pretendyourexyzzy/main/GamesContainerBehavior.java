package com.gianlu.pretendyourexyzzy.main;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;

import com.gianlu.pretendyourexyzzy.R;

import java.util.List;

public final class GamesContainerBehavior extends CoordinatorLayout.Behavior<LinearLayout> {
    private int mCurrentOffset = -1;

    public GamesContainerBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(@NonNull CoordinatorLayout parent, @NonNull LinearLayout child, @NonNull View dependency) {
        return dependency instanceof LinearLayout && dependency.getId() == R.id.gamesFragment_serverContainer;
    }

    @Override
    public boolean onDependentViewChanged(@NonNull CoordinatorLayout parent, @NonNull LinearLayout child, @NonNull View dependency) {
        int height = dependency.getHeight();
        if (height > 0) {
            if (mCurrentOffset == -1) mCurrentOffset = height;
            int offset = mCurrentOffset - child.getTop();
            child.offsetTopAndBottom(offset);
            return offset != 0;
        } else {
            return false;
        }
    }

    @Override
    public boolean onLayoutChild(@NonNull CoordinatorLayout parent, @NonNull LinearLayout child, int layoutDirection) {
        List<View> views = parent.getDependencies(child);
        if (!views.isEmpty()) {
            parent.onLayoutChild(child, layoutDirection);
            if (mCurrentOffset == -1) mCurrentOffset = views.get(0).getHeight();
            child.offsetTopAndBottom(mCurrentOffset - child.getTop());
            return true;
        }

        return false;
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull LinearLayout child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull LinearLayout child, @NonNull View target,
                               int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {
        if (dyUnconsumed < 0) {
            List<View> views = coordinatorLayout.getDependencies(child);
            if (views.isEmpty()) return;

            int height = views.get(0).getHeight();
            if (mCurrentOffset < height) {
                int toConsume = Math.min(-dyUnconsumed, height - mCurrentOffset);
                mCurrentOffset += toConsume;
                consumed[1] -= toConsume;
                child.offsetTopAndBottom(toConsume);
            }
        } else if (dyConsumed > 0 && mCurrentOffset > 0) {
            int toConsume = -Math.min(dyConsumed, mCurrentOffset);
            mCurrentOffset += toConsume;
            child.offsetTopAndBottom(toConsume);
        }
    }
}
