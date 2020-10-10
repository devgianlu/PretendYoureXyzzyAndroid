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
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewProfileBinding;

import org.jetbrains.annotations.NotNull;

public class NewProfileFragment extends FragmentWithDialog implements NewMainActivity.MainFragment {
    private FragmentNewProfileBinding binding;

    @NonNull
    public static NewProfileFragment get() {
        return new NewProfileFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNewProfileBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onPyxReady(@NotNull RegisteredPyx pyx) {

    }

    @Override
    public void onPyxInvalid() {

    }
}
