package com.gianlu.pretendyourexyzzy.customdecks;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.pretendyourexyzzy.databinding.FragmentCustomDeckCardsBinding;

public abstract class AbsNewCardsFragment extends FragmentWithDialog {

    protected abstract boolean addEnabled();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentCustomDeckCardsBinding binding = FragmentCustomDeckCardsBinding.inflate(inflater, container, false);
        binding.customDeckCardsList.setLayoutManager(new GridLayoutManager(requireContext(), 3, RecyclerView.HORIZONTAL, false));

        if (addEnabled()) {
            binding.customDeckCardsAddContainer.setVisibility(View.VISIBLE);
            binding.customDeckCardsAdd.setOnClickListener(v -> {
                // TODO: Show add card fragment
            });
        } else {
            binding.customDeckCardsAddContainer.setVisibility(View.GONE);
        }

        // TODO: Load cards

        return binding.getRoot();
    }
}
