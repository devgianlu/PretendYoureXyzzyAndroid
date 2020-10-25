package com.gianlu.pretendyourexyzzy.customdecks;

import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.pretendyourexyzzy.R;

import org.jetbrains.annotations.NotNull;

public abstract class AbsNewCustomDeckActivity extends ActivityWithDialog {
    private final SparseArray<Fragment> fragments = new SparseArray<>(3);
    private com.gianlu.pretendyourexyzzy.databinding.ActivityNewCustomDeckBinding binding;
    private Mode mode = Mode.HIDDEN;

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

        binding.customDeckNavigation.setOnSelectionChangedListener(this::switchFragment);

        loading();
    }

    private boolean switchFragment(int pos) {
        if (fragments.size() == 0) return false;

        if (navigateTo(pos)) {
            FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
            for (int i = 0; i < 3; i++) {
                if (i != pos) trans.hide(fragments.get(i));
            }

            trans.show(fragments.get(pos)).commit();

            if (mode == Mode.CONTINUE) {
                if (pos == 0) binding.customDeckBottomButton.setText(R.string.continue_);
                else if (pos == 1) binding.customDeckBottomButton.setText(R.string.continue_);
                else binding.customDeckBottomButton.setText(R.string.save);
            } else if (mode == Mode.SAVE) {
                binding.customDeckBottomButton.setText(R.string.save);
            }

            return true;
        } else {
            return false;
        }
    }

    protected final boolean save() {
        Bundle bundle = new Bundle();

        for (int i = 0; i < 3; i++) {
            Fragment fragment = fragments.get(i);
            if (fragment instanceof SavableFragment) {
                if (!((SavableFragment) fragment).save(bundle))
                    return false;
            }
        }

        return true;
    }

    protected final void loading() {
        fragments.clear();

        binding.customDeckBottomButton.setEnabled(false);

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

        binding.customDeckBottomButton.setEnabled(true);
    }

    protected final void setBottomButtonMode(@NotNull Mode mode) {
        this.mode = mode;

        switch (mode) {
            case HIDDEN:
                binding.customDeckBottomButton.setVisibility(View.GONE);
                break;
            case CONTINUE:
                binding.customDeckBottomButton.setVisibility(View.VISIBLE);
                binding.customDeckBottomButton.setText(R.string.continue_);
                binding.customDeckBottomButton.setOnClickListener(v -> {
                    int selected = binding.customDeckNavigation.getSelected();
                    if (selected == 0) {
                        if (save()) switchFragment(1);
                    } else if (selected == 1) {
                        if (save()) switchFragment(2);
                    } else if (selected == 2) {
                        if (save()) finishAfterTransition();
                    }
                });
                break;
            case SAVE:
                binding.customDeckBottomButton.setVisibility(View.VISIBLE);
                binding.customDeckBottomButton.setText(R.string.save);
                binding.customDeckBottomButton.setOnClickListener(v -> {
                    if (save()) finishAfterTransition();
                });
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

    public interface SavableFragment {
        boolean save(@NotNull Bundle bundle);
    }
}
