package com.gianlu.pretendyourexyzzy;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.pretendyourexyzzy.api.FirstLoadedPyx;
import com.gianlu.pretendyourexyzzy.api.PyxDiscoveryApi;
import com.gianlu.pretendyourexyzzy.databinding.ActivityOneTimeLoginBinding;
import com.google.android.gms.tasks.Task;

public class OneTimeLoginActivity extends ActivityWithDialog {
    private ActivityOneTimeLoginBinding binding;
    private Task<FirstLoadedPyx> firstLoadTask;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOneTimeLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firstLoadTask = PyxDiscoveryApi.get().firstLoad(this);

        binding.oneTimeLoginContinue.setOnClickListener(v -> {
            if (firstLoadTask.isComplete()) {
                if (firstLoadTask.isSuccessful()) doContinue(firstLoadTask.getResult());
                else continueFailed(firstLoadTask.getException());
                return;
            }

            binding.oneTimeLoginContinue.setEnabled(false);
            firstLoadTask.addOnSuccessListener(OneTimeLoginActivity.this, OneTimeLoginActivity.this::doContinue)
                    .addOnFailureListener(OneTimeLoginActivity.this, OneTimeLoginActivity.this::continueFailed);
        });
    }

    private void doContinue(@NonNull FirstLoadedPyx pyx) {
        // TODO
    }

    private void continueFailed(Exception ex) {
        // TODO
    }
}
