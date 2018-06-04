package com.gianlu.pretendyourexyzzy.Dialogs;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

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
    private static final int WRITE_EXT_CODE = 67;
    private GameRoundSummary summary;
    private ImageView image;
    private CheckBox rotate;
    private ProgressBar loading;
    private GameRound round;
    private Button save;
    private Button share;

    @NonNull
    public static GameRoundDialog get(String id) {
        GameRoundDialog dialog = new GameRoundDialog();
        Bundle args = new Bundle();
        args.putString("id", id);
        dialog.setArguments(args);
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
                dismiss();
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
            dismiss();
            return layout;
        }

        Bundle args = getArguments();
        String id;
        if (args == null || (id = args.getString("id", null)) == null) {
            DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading));
            dismiss();
            return layout;
        }

        pyx.getGameRound(id, this);

        return layout;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == WRITE_EXT_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                saveToExternalStorage();
            else
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.declinedWritePermission));
            return;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void saveToExternalStorage() {
        if (getActivity() == null || getContext() == null) return;

        File dest = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            File image = save(dest);
            if (image == null) {
                Toaster.with(getContext()).message(R.string.failedSavingImage).show();
            } else {
                Toaster.with(getContext()).message(R.string.imageSavedTo, image.getAbsolutePath()).extra(image).show();
                dismiss();
            }
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXT_CODE);
        }
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
        dismiss();
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
        dismiss();
    }
}
