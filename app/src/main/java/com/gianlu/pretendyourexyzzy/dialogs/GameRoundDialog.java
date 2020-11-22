package com.gianlu.pretendyourexyzzy.dialogs;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;

import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.permissions.AskPermission;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.ThisApplication;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.models.metrics.GameRound;
import com.gianlu.pretendyourexyzzy.cards.GameRoundSummary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class GameRoundDialog extends DialogFragment {
    private static final String TAG = GameRoundDialog.class.getSimpleName();
    private GameRoundSummary summary;
    private ImageView image;
    private CheckBox rotate;
    private ProgressBar loading;
    private GameRound round;
    private Button save;
    private Button share;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.dialog_game_round, container, false);
        image = layout.findViewById(R.id.gameRoundDialog_image);
        loading = layout.findViewById(R.id.gameRoundDialog_loading);
        rotate = layout.findViewById(R.id.gameRoundDialog_rotate);
        rotate.setOnCheckedChangeListener((buttonView, isChecked) -> generate());
        save = layout.findViewById(R.id.gameRoundDialog_save);
        save.setOnClickListener(v -> saveToExternalStorage());
        share = layout.findViewById(R.id.gameRoundDialog_share);
        share.setOnClickListener(v -> {
            if (getContext() != null)
                share(getContext());
        });

        Button cancel = layout.findViewById(R.id.gameRoundDialog_cancel);
        cancel.setOnClickListener(v -> dismissAllowingStateLoss());

        image.setVisibility(View.GONE);
        loading.setVisibility(View.VISIBLE);
        rotate.setEnabled(false);
        share.setEnabled(false);
        save.setEnabled(false);

        Pyx pyx;
        try {
            pyx = Pyx.get();
        } catch (LevelMismatchException ex) {
            DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading));
            dismissAllowingStateLoss();
            return layout;
        }

        Bundle args = getArguments();
        String id;
        if (args == null || (id = args.getString("id", null)) == null) {
            DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading));
            dismissAllowingStateLoss();
            return layout;
        }

        pyx.getGameRound(id)
                .addOnSuccessListener(requireActivity(), result -> {
                    round = result;
                    generate();

                    loading.setVisibility(View.GONE);
                    image.setVisibility(View.VISIBLE);
                    rotate.setEnabled(true);
                    share.setEnabled(true);
                    save.setEnabled(true);
                })
                .addOnFailureListener(requireActivity(), ex -> {
                    Log.e(TAG, "Failed loading round.", ex);
                    DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading));
                    dismissAllowingStateLoss();
                });

        return layout;
    }

    private void saveToExternalStorage() {
        if (getActivity() == null) return;

        AskPermission.ask(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE, new AskPermission.Listener() {
            @Override
            public void permissionGranted(@NonNull String permission) {
                File image = save(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
                if (image == null) {
                    DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedSavingImage));
                } else {
                    DialogUtils.showToast(getContext(), Toaster.build().message(R.string.imageSavedTo, image.getAbsolutePath()).extra(image));
                    dismissAllowingStateLoss();
                }
            }

            @Override
            public void permissionDenied(@NonNull String permission) {
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.deniedWritePermission));
            }

            @Override
            public void askRationale(@NonNull AlertDialog.Builder builder) {
                builder.setTitle(R.string.askWritePermission)
                        .setMessage(R.string.askWritePermission_roundImage);
            }
        });
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

        summary = new GameRoundSummary(getContext(), round, rotate.isChecked());
        image.setImageBitmap(summary.getBitmap());
    }
}
