package com.gianlu.pretendyourexyzzy.Dialogs;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.gianlu.commonutils.AskPermission;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.CardViews.GameRoundSummary;
import com.gianlu.pretendyourexyzzy.NetIO.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics.GameRound;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class GameRoundDialog extends DialogFragment implements Pyx.OnResult<GameRound> {
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
        rotate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                generate();
            }
        });
        save = layout.findViewById(R.id.gameRoundDialog_save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveToExternalStorage();
            }
        });
        share = layout.findViewById(R.id.gameRoundDialog_share);
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getContext() != null)
                    share(getContext());
            }
        });

        Button cancel = layout.findViewById(R.id.gameRoundDialog_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissAllowingStateLoss();
            }
        });

        image.setVisibility(View.GONE);
        loading.setVisibility(View.VISIBLE);
        rotate.setEnabled(false);
        share.setEnabled(false);
        save.setEnabled(false);

        Pyx pyx;
        try {
            pyx = Pyx.get();
        } catch (LevelMismatchException ex) {
            DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading).ex(ex));
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

        pyx.getGameRound(id, this);

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
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.deniedWritePermission).error(true));
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
            Logging.log(ex);
            return null;
        }

        return image;
    }

    private void share(@NonNull Context context) {
        File image = save(new File(context.getFilesDir(), "gameRoundImages"));
        if (image == null) {
            Toaster.with(context).message(R.string.failedSavingImage).error(true).show();
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
        if (getContext() == null) return;

        summary = new GameRoundSummary(getContext(), round, rotate.isChecked());
        image.setImageBitmap(summary.getBitmap());
    }

    @Override
    public void onDone(@NonNull GameRound result) {
        round = result;
        generate();

        loading.setVisibility(View.GONE);
        image.setVisibility(View.VISIBLE);
        rotate.setEnabled(true);
        share.setEnabled(true);
        save.setEnabled(true);
    }

    @Override
    public void onException(@NonNull Exception ex) {
        DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading).ex(ex));
        dismissAllowingStateLoss();
    }
}
