package com.gianlu.pretendyourexyzzy.customdecks;

import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.gianlu.commonutils.dialogs.ActivityWithDialog;

import org.jetbrains.annotations.NotNull;

public abstract class AbsNewCustomDeckActivity extends ActivityWithDialog {
    private final SparseArray<Fragment> fragments = new SparseArray<>(3);
    private com.gianlu.pretendyourexyzzy.databinding.ActivityNewCustomDeckBinding binding;

    @Override
    @CallSuper
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = com.gianlu.pretendyourexyzzy.databinding.ActivityNewCustomDeckBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.customDeckClose.setOnClickListener(v -> finishAfterTransition());
        binding.customDeckMenu.setOnClickListener(v -> {
            // TODO: Load menu
        });

        binding.customDeckNavigation.setOnSelectionChangedListener(pos -> {
            if (navigateTo(pos)) {
                FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
                for (int i = 0; i < 3; i++) {
                    if (i != pos) trans.hide(fragments.get(i));
                }

                trans.show(fragments.get(pos)).commit();
            }
        });

        loading();
    }

    protected final void loading() {
        fragments.clear();

        // TODO: Show loading
    }

    protected final void loaded(@NotNull Fragment... frags) {
        if (frags.length != 3) throw new IllegalArgumentException();

        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
        for (int i = 0; i < 3; i++) {
            Fragment frag = frags[i];
            fragments.put(i, frag);
            trans.add(binding.customDeckContainer.getId(), frag)
                    .hide(frag);
        }

        trans.show(fragments.get(0));
        trans.commit();
    }

    protected final void setBottomButtonText(@StringRes int textRes) {
        binding.customDeckBottomButton.setText(textRes);
    }

    protected final void setBottomButtonMode(@NotNull Mode mode) {
        switch (mode) {
            case HIDDEN:
                binding.customDeckBottomButton.setVisibility(View.GONE);
                break;
            case CONTINUE:
                binding.customDeckBottomButton.setVisibility(View.VISIBLE);
                // TODO: Continue until last and save
                break;
            case SAVE:
                binding.customDeckBottomButton.setVisibility(View.VISIBLE);
                // TODO: Save and exit
                break;
            default:
                throw new IllegalArgumentException(mode.name());
        }
    }

    @Override
    public final void onBackPressed() {
        finishAfterTransition();
    }

    /**
     * @return Whether it should proceed in showing the fragment.
     */
    protected boolean navigateTo(@IntRange(from = 0, to = 2) int pos) {
        return true;
    }

    public enum Mode {
        HIDDEN, CONTINUE, SAVE
    }
}
