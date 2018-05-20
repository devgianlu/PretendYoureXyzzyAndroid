package com.gianlu.pretendyourexyzzy.Dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.R;

public class CardImageZoomDialog extends DialogFragment {

    @NonNull
    public static CardImageZoomDialog get(@NonNull BaseCard card) {
        CardImageZoomDialog dialog = new CardImageZoomDialog();
        Bundle args = new Bundle();
        args.putString("url", card.getImageUrl());
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Window window = dialog.getWindow();
                if (window != null)
                    window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.dialog_image_zoom, container, false);

        final String url;
        Bundle args = getArguments();
        if (args == null || (url = args.getString("url", null)) == null) {
            dismiss();
            return layout;
        }

        ImageView image = layout.findViewById(R.id.imageZoomDialog_image);
        Glide.with(this).load(url).into(image);

        ImageButton open = layout.findViewById(R.id.imageZoomDialog_open);
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        });

        return layout;
    }
}
