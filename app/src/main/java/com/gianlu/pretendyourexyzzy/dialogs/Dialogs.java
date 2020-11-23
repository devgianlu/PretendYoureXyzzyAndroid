package com.gianlu.pretendyourexyzzy.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxException;
import com.gianlu.pretendyourexyzzy.databinding.DialogAddServerBinding;
import com.gianlu.pretendyourexyzzy.databinding.DialogCannotStartGameBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONException;

import java.util.Objects;

import okhttp3.HttpUrl;

public final class Dialogs {

    @NonNull
    public static AlertDialog addServer(@NonNull Context context, @Nullable Pyx.Server server) {
        DialogAddServerBinding binding = DialogAddServerBinding.inflate(LayoutInflater.from(context), null, false);

        CommonUtils.clearErrorOnEdit(binding.addServerName);
        CommonUtils.clearErrorOnEdit(binding.addServerUrl);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.addServer)
                .setView(binding.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(server == null ? R.string.add : R.string.apply, null);

        if (server != null) {
            CommonUtils.setText(binding.addServerName, server.name);
            CommonUtils.setText(binding.addServerUrl, server.url.toString());
            builder.setNeutralButton(R.string.remove, (dialog, which) -> Pyx.Server.removeUserServer(server));
        }

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(di -> dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nameStr = CommonUtils.getText(binding.addServerName);
            if (nameStr.isEmpty() || (server != null && !Objects.equals(server.name, nameStr) && Pyx.Server.hasServer(nameStr))) {
                binding.addServerName.setError(context.getString(R.string.invalidServerName));
                return;
            }

            String urlStr = CommonUtils.getText(binding.addServerUrl);
            HttpUrl url = Pyx.Server.parseUrl(urlStr);
            if (url == null) {
                binding.addServerUrl.setError(context.getString(R.string.invalidServerUrl));
                return;
            }

            try {
                Pyx.Server.addUserServer(new Pyx.Server(url, null, nameStr, Pyx.Server.Params.defaultValues(), true));
            } catch (JSONException ex) {
                Toaster.with(context).message(R.string.failedAddingServer).show();
            }

            di.dismiss();
        }));

        return dialog;
    }

    @NonNull
    public static MaterialAlertDialogBuilder notEnoughCards(@NonNull Context context, @NonNull PyxException ex) throws JSONException {
        DialogCannotStartGameBinding binding = DialogCannotStartGameBinding.inflate(LayoutInflater.from(context), null, false);

        int wcr = ex.obj.getInt("wcr");
        int bcr = ex.obj.getInt("bcr");
        int wcp = ex.obj.getInt("wcp");
        int bcp = ex.obj.getInt("bcp");

        binding.cannotStartGameWcr.setText(String.valueOf(wcr));
        binding.cannotStartGameBcr.setText(String.valueOf(bcr));
        binding.cannotStartGameWcp.setText(String.valueOf(wcp));
        binding.cannotStartGameBcp.setText(String.valueOf(bcp));
        binding.cannotStartGameCheckBc.setImageResource(bcp >= bcr ? R.drawable.baseline_done_24 : R.drawable.baseline_clear_24);
        binding.cannotStartGameCheckWc.setImageResource(wcp >= wcr ? R.drawable.baseline_done_24 : R.drawable.baseline_clear_24);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.cannotStartGame)
                .setView(binding.getRoot())
                .setPositiveButton(android.R.string.ok, null);

        return builder;
    }

    @NonNull
    public static MaterialAlertDialogBuilder askText(@NonNull Context context, @NonNull final OnText listener) {
        EditText text = new EditText(context);
        return new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.setBlankCardText)
                .setView(text)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> listener.onText(text.getText().toString())).setNegativeButton(android.R.string.cancel, null);
    }

    @NonNull
    public static MaterialAlertDialogBuilder confirmation(@NonNull Context context, @StringRes int res, @NonNull final OnConfirmed listener) {
        return new MaterialAlertDialogBuilder(context).setTitle(res)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> listener.onConfirmed())
                .setNegativeButton(android.R.string.no, null);
    }

    public interface OnConfirmed {
        void onConfirmed();
    }

    public interface OnText {
        void onText(@NonNull String text);
    }
}
