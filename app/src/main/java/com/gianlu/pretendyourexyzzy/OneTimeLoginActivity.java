package com.gianlu.pretendyourexyzzy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.api.FirstLoadedPyx;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxDiscoveryApi;
import com.gianlu.pretendyourexyzzy.api.PyxException;
import com.gianlu.pretendyourexyzzy.databinding.ActivityOneTimeLoginBinding;
import com.google.android.gms.tasks.Task;

import java.util.List;
import java.util.Objects;

public class OneTimeLoginActivity extends ActivityWithDialog {
    private static final String TAG = OneTimeLoginActivity.class.getSimpleName();
    private ActivityOneTimeLoginBinding binding;
    private Task<FirstLoadedPyx> firstLoadTask;
    private List<Pyx.Server> availableServers = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOneTimeLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firstLoadTask = PyxDiscoveryApi.get().firstLoad(this);

        binding.oneTimeLoginInputs.idCodeInput.setEndIconOnClickListener(v -> CommonUtils.setText(binding.oneTimeLoginInputs.idCodeInput, CommonUtils.randomString(100)));
        CommonUtils.clearErrorOnEdit(binding.oneTimeLoginInputs.usernameInput);
        CommonUtils.clearErrorOnEdit(binding.oneTimeLoginInputs.idCodeInput);

        binding.oneTimeLoginContinue.setOnClickListener(v -> {
            setLoading(true);
            firstLoadTask.addOnSuccessListener(this, this::register)
                    .addOnFailureListener(this, this::firstLoadFailed);
        });
    }

    @Nullable
    private String getIdCode() {
        String id = CommonUtils.getText(binding.oneTimeLoginInputs.idCodeInput).trim();
        return id.isEmpty() ? null : id;
    }

    private void setLoading(boolean loading) {
        binding.oneTimeLoginContinue.setEnabled(!loading);
        binding.oneTimeLoginInputs.usernameInput.setEnabled(!loading);
        binding.oneTimeLoginInputs.idCodeInput.setEnabled(!loading);
    }

    private void register(@NonNull FirstLoadedPyx pyx) {
        if (!pyx.isServerSecure() && !pyx.config().insecureIdAllowed())
            binding.oneTimeLoginInputs.idCodeInput.setEnabled(false);
        else
            binding.oneTimeLoginInputs.idCodeInput.setEnabled(true);

        String idCode = getIdCode();
        String username = CommonUtils.getText(binding.oneTimeLoginInputs.usernameInput);

        setLoading(true);
        pyx.register(username, idCode)
                .addOnSuccessListener(this, result -> {
                    Prefs.putString(PK.LAST_NICKNAME, result.user().nickname);
                    Prefs.putString(PK.LAST_ID_CODE, idCode);
                    Prefs.putBoolean(PK.ONE_TIME_LOGIN_SHOWN, true);

                    startActivity(new Intent(this, NewMainActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                })
                .addOnFailureListener(this, ex -> {
                    Log.e(TAG, "Failed registering on server.", ex);

                    setLoading(false);

                    if (ex instanceof PyxException) {
                        switch (((PyxException) ex).errorCode) {
                            case "rn":
                                binding.oneTimeLoginInputs.usernameInput.setError(getString(R.string.reservedNickname));
                                return;
                            case "in":
                                binding.oneTimeLoginInputs.usernameInput.setError(getString(R.string.invalidNickname));
                                return;
                            case "niu":
                                binding.oneTimeLoginInputs.usernameInput.setError(getString(R.string.alreadyUsedNickname));
                                return;
                            case "tmu":
                                if (changeServer(pyx.server))
                                    binding.oneTimeLoginInputs.usernameInput.setError(getString(R.string.tooManyUsers));
                                return;
                            case "iid":
                                binding.oneTimeLoginInputs.usernameInput.setError(getString(R.string.invalidIdCode));
                                return;
                        }
                    }

                    Toaster.with(this).message(R.string.failedLoading).show();
                });
    }

    /**
     * Changes server and registers on it.
     *
     * @param current The current server, which will not be reused
     * @return Whether there wasn't any server left on the list
     */
    private boolean changeServer(@NonNull Pyx.Server current) {
        if (availableServers == null) availableServers = Pyx.Server.loadAllServers();
        availableServers.remove(current);

        if (availableServers.isEmpty()) return true;

        Pyx.Server.setLastServer(Pyx.Server.pickBestServer(availableServers));

        setLoading(true);
        firstLoadTask = PyxDiscoveryApi.get().firstLoad(this);
        firstLoadTask.addOnSuccessListener(this, this::register)
                .addOnFailureListener(this, this::firstLoadFailed);
        return false;
    }

    private void firstLoadFailed(Exception ex) {
        setLoading(false);

        if (ex instanceof PyxException) {
            if (Objects.equals(((PyxException) ex).errorCode, "se"))
                return;
        }

        Log.e(TAG, "Failed loading server.", ex);

        Pyx.Server lastServer = Pyx.Server.lastServerNoThrow();
        if (lastServer != null && changeServer(lastServer))
            Toaster.with(this).message(R.string.failedLoading).show();
    }
}
