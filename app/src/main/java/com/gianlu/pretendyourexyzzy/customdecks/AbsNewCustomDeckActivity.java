package com.gianlu.pretendyourexyzzy.customdecks;

import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.pretendyourexyzzy.R;

import org.jetbrains.annotations.NotNull;

public abstract class AbsNewCustomDeckActivity extends ActivityWithDialog {
    private com.gianlu.pretendyourexyzzy.databinding.ActivityNewCustomDeckBinding binding;
    private Mode mode = Mode.HIDDEN;
    private PagerAdapter adapter;

    protected void onInflateMenu(@NotNull MenuInflater inflater, @NotNull Menu menu) {
    }

    protected boolean onMenuItemSelected(@NotNull MenuItem item) {
        return false;
    }

    protected final void setMenuIconVisible(boolean visible) {
        binding.customDeckMenu.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    protected final void setPageChangeAllowed(boolean allowed) {
        binding.customDeckPager.setUserInputEnabled(allowed);
    }

    @Override
    @CallSuper
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = com.gianlu.pretendyourexyzzy.databinding.ActivityNewCustomDeckBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.customDeckClose.setOnClickListener(v -> finishAfterTransition());
        binding.customDeckMenu.setOnClickListener(v -> showPopupMenu());

        binding.customDeckPager.setOffscreenPageLimit(3);
        binding.customDeckPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int pos) {
                if (adapter != null) {
                    Fragment prev = adapter.fragments[binding.customDeckNavigation.getSelected()];
                    if (prev instanceof AbsNewCardsFragment) ((AbsNewCardsFragment) prev).goBack();
                }

                binding.customDeckNavigation.setSelected(pos);

                if (mode == Mode.CONTINUE) {
                    if (pos == 0)
                        binding.customDeckBottomButton.setText(R.string.continue_);
                    else if (pos == 1)
                        binding.customDeckBottomButton.setText(R.string.continue_);
                    else
                        binding.customDeckBottomButton.setText(R.string.save);
                } else if (mode == Mode.SAVE) {
                    binding.customDeckBottomButton.setText(R.string.save);
                }
            }
        });

        binding.customDeckNavigation.setOnSelectionChangedListener(pos -> {
            if (!binding.customDeckPager.isUserInputEnabled())
                return false;

            binding.customDeckPager.setCurrentItem(pos, true);
            return true;
        });

        loading();
    }

    private void showPopupMenu() {
        PopupMenu menu = new PopupMenu(this, binding.customDeckMenu);
        onInflateMenu(menu.getMenuInflater(), menu.getMenu());
        menu.setOnMenuItemClickListener(this::onMenuItemSelected);
        menu.show();
    }

    @NotNull
    protected String getName() {
        Bundle bundle = new Bundle();
        if (save(bundle)) return bundle.getString("name", "");
        else return "";
    }

    @Nullable
    protected Integer getDeckId() {
        Bundle bundle = new Bundle();
        if (!save(bundle)) return null;
        int deckId = bundle.getInt("deckId", -1);
        return deckId == -1 ? null : deckId;
    }

    protected void onSaved(@NotNull Bundle bundle) {
    }

    private boolean save(@NotNull Bundle bundle) {
        SavableFragment.Callback callback = new SavableFragment.Callback() {
            @Override
            public void lockNavigation(boolean locked) {
                binding.customDeckBottomButton.setEnabled(!locked);
                binding.customDeckPager.setUserInputEnabled(!locked);
            }

            @NotNull
            @Override
            public CustomDecksDatabase getDb() {
                return CustomDecksDatabase.get(AbsNewCustomDeckActivity.this);
            }
        };

        for (int i = 0; i < adapter.fragments.length; i++) {
            Fragment fragment = adapter.fragments[i];
            if (fragment instanceof SavableFragment) {
                if (!((SavableFragment) fragment).save(bundle, callback))
                    return false;
            }
        }

        onSaved(bundle);
        return true;
    }

    protected final boolean save() {
        return save(new Bundle());
    }

    protected final void loading() {
        adapter = null;

        binding.customDeckBottomButton.setEnabled(false);

        binding.customDeckPager.setAdapter(null);
        binding.customDeckPager.setVisibility(View.GONE);
        binding.customDeckLoading.setVisibility(View.VISIBLE);
        ((Animatable) binding.customDeckLoading.getDrawable()).start();
    }

    protected final void loaded(@NotNull Fragment... frags) {
        if (frags.length != 3) throw new IllegalArgumentException();

        binding.customDeckLoading.setVisibility(View.GONE);
        binding.customDeckPager.setVisibility(View.VISIBLE);
        binding.customDeckPager.setAdapter(adapter = new PagerAdapter(this, frags));

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
                        if (save() && binding.customDeckPager.isUserInputEnabled())
                            binding.customDeckPager.setCurrentItem(1);
                    } else if (selected == 1) {
                        if (save() && binding.customDeckPager.isUserInputEnabled())
                            binding.customDeckPager.setCurrentItem(2);
                    } else if (selected == 2) {
                        if (save())
                            finishAfterTransition();
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
        if (adapter != null) {
            Fragment current = adapter.fragments[binding.customDeckPager.getCurrentItem()];
            if (current instanceof AbsNewCardsFragment && ((AbsNewCardsFragment) current).goBack())
                return;
        }

        finishAfterTransition();
    }

    public enum Mode {
        HIDDEN, CONTINUE, SAVE
    }

    public interface SavableFragment {
        /**
         * @return Generally, whether the save failed or the next fragment shouldn't be shown
         */
        boolean save(@NotNull Bundle bundle, @NotNull Callback callback);

        interface Callback {
            void lockNavigation(boolean locked);

            @NotNull
            CustomDecksDatabase getDb();
        }
    }

    private static class PagerAdapter extends FragmentStateAdapter {
        final Fragment[] fragments;

        PagerAdapter(@NonNull FragmentActivity activity, @NonNull Fragment[] fragments) {
            super(activity);
            this.fragments = fragments;

            if (fragments.length != 3)
                throw new IllegalArgumentException();
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return fragments[position];
        }

        @Override
        public int getItemCount() {
            return fragments.length;
        }
    }
}
