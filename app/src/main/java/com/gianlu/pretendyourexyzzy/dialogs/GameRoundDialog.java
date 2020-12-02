package com.gianlu.pretendyourexyzzy.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;

import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.ThisApplication;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.models.metrics.GameRound;
import com.gianlu.pretendyourexyzzy.cards.GameRoundSummary;
import com.gianlu.pretendyourexyzzy.databinding.DialogGameRoundBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class GameRoundDialog extends DialogFragment {
    private static final String TAG = GameRoundDialog.class.getSimpleName();
    private GameRoundSummary summary;
    private GameRound round;
    private DialogGameRoundBinding binding;

    @NonNull
    public static GameRoundDialog get(@NonNull String id) {
        GameRoundDialog dialog = new GameRoundDialog();
        Bundle args = new Bundle();
        args.putString("id", id);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogGameRoundBinding.inflate(inflater, container, false);
        binding.gameRoundDialogRotate.setOnCheckedChangeListener((buttonView, isChecked) -> generate());
        binding.gameRoundDialogCancel.setOnClickListener(v -> dismissAllowingStateLoss());
        binding.gameRoundDialogShare.setOnClickListener(v -> share(requireContext()));

        binding.gameRoundDialogImage.setVisibility(View.GONE);
        binding.gameRoundDialogLoading.setVisibility(View.VISIBLE);
        binding.gameRoundDialogRotate.setEnabled(false);
        binding.gameRoundDialogShare.setEnabled(false);

        Pyx pyx;
        try {
            pyx = Pyx.get();
        } catch (LevelMismatchException ex) {
            DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading));
            dismissAllowingStateLoss();
            return null;
        }

        Bundle args = getArguments();
        String id;
        if (args == null || (id = args.getString("id", null)) == null) {
            DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading));
            dismissAllowingStateLoss();
            return null;
        }

        pyx.getGameRound(id)
                .addOnSuccessListener(requireActivity(), result -> {
                    round = result;
                    generate();

                    binding.gameRoundDialogImage.setVisibility(View.VISIBLE);
                    binding.gameRoundDialogLoading.setVisibility(View.GONE);
                    binding.gameRoundDialogRotate.setEnabled(true);
                    binding.gameRoundDialogShare.setEnabled(true);
                })
                .addOnFailureListener(requireActivity(), ex -> {
                    Log.e(TAG, "Failed loading round.", ex);
                    DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading));
                    dismissAllowingStateLoss();
                });

        return binding.getRoot();
    }

    @Nullable
    private File save(@NonNull File directory) {
        if (summary == null) return null;

        if (!directory.exists() && !directory.mkdir()) return null;

        File image = new File(directory, summary.getName() + ".png");
        Bitmap bitmap = summary.getBitmap();

        try (FileOutputStream out = new FileOutputStream(image)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException ex) {
            Log.e(TAG, "Failed saving image.", ex);
            return null;
        }

        ThisApplication.sendAnalytics(Utils.ACTION_SAVE_SHARE_ROUND);

        return image;
    }

    private void share(@NonNull Context context) {
        File image = save(new File(context.getFilesDir(), "gameRoundImages"));
        if (image == null) {
            Toaster.with(context).message(R.string.failedSavingImage).show();
            return;
        }

        Uri uri = FileProvider.getUriForFile(context, "com.gianlu.pretendyourexyzzy", image);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setDataAndType(uri, "image/png");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, "Send to..."));
        dismissAllowingStateLoss();
    }

    private void generate() {
        if (getContext() == null || round == null) return;

        summary = new GameRoundSummary(getContext(), round, binding.gameRoundDialogRotate.isChecked());
        binding.gameRoundDialogImage.setImageBitmap(summary.getBitmap());
    }
}
