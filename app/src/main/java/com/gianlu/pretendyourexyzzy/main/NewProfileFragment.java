package com.gianlu.pretendyourexyzzy.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.pretendyourexyzzy.NewMainActivity;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewProfileBinding;

import org.jetbrains.annotations.NotNull;

public class NewProfileFragment extends FragmentWithDialog implements NewMainActivity.MainFragment {
    private FragmentNewProfileBinding binding;
    private RegisteredPyx pyx;

    @NonNull
    public static NewProfileFragment get() {
        return new NewProfileFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNewProfileBinding.inflate(inflater, container, false);
        binding.profileFragmentInputs.idCodeInput.setEndIconOnClickListener(v -> CommonUtils.setText(binding.profileFragmentInputs.idCodeInput, CommonUtils.randomString(100)));
        return binding.getRoot();
    }

    @Override
    public void onPyxReady(@NotNull RegisteredPyx pyx) {
        this.pyx = pyx;

        CommonUtils.setText(binding.profileFragmentInputs.usernameInput, pyx.user().nickname);
        CommonUtils.setText(binding.profileFragmentInputs.idCodeInput, Prefs.getString(PK.LAST_ID_CODE, null));
    }

    @Override
    public void onPyxInvalid() {
        this.pyx = null;
    }

    @Override
    public boolean goBack() {
        return false;
    }

    @NotNull
    public String getUsername() {
        return CommonUtils.getText(binding.profileFragmentInputs.usernameInput);
    }

    @Nullable
    public String getIdCode() {
        String idCode = CommonUtils.getText(binding.profileFragmentInputs.idCodeInput);
        return idCode.trim().isEmpty() ? null : idCode.trim();
    }
}
