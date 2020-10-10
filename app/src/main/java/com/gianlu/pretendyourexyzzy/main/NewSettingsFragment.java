package com.gianlu.pretendyourexyzzy.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.pretendyourexyzzy.NewMainActivity;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewSettingsBinding;

import org.jetbrains.annotations.NotNull;

public class NewSettingsFragment extends FragmentWithDialog implements NewMainActivity.MainFragment {
    private FragmentNewSettingsBinding binding;

    @NonNull
    public static NewSettingsFragment get() {
        return new NewSettingsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNewSettingsBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onPyxReady(@NotNull RegisteredPyx pyx) {

    }

    @Override
    public void onPyxInvalid() {

    }
}
