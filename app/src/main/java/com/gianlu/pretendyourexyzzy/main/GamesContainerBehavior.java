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
    public GamesContainerBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(@NonNull CoordinatorLayout parent, @NonNull LinearLayout child, @NonNull View dependency) {
        return dependency instanceof LinearLayout && dependency.getId() == R.id.gamesFragment_serverContainer;
    }

    @Override
    public boolean onDependentViewChanged(@NonNull CoordinatorLayout parent, @NonNull LinearLayout child, @NonNull View dependency) {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) child.getLayoutParams();
        params.topMargin = dependency.getHeight();
        return true;
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull LinearLayout child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull LinearLayout child, @NonNull View target,
                                  int dx, int dy, @NonNull int[] consumed, int type) {

    }

    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull LinearLayout child, @NonNull View target,
                               int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) child.getLayoutParams();
        if (dyUnconsumed < 0) {
            List<View> views = coordinatorLayout.getDependencies(child);
            if (views.isEmpty()) return;

            int height = views.get(0).getHeight();
            if (params.topMargin < height) {
                int toConsume = Math.min(-dyUnconsumed, height - params.topMargin);
                params.topMargin += toConsume;
                consumed[1] -= toConsume;
                child.requestLayout();
            }
        } else if (dyConsumed > 0 && params.topMargin > 0) {
            params.topMargin -= Math.min(dyConsumed, params.topMargin);
            child.requestLayout();
        }
    }
}
