package com.gianlu.pretendyourexyzzy.dialogs;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.BaseCardUrlLoader;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;

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
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setOnShowListener(dialogInterface -> {
            Window window = dialog.getWindow();
            if (window != null)
                window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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
            dismissAllowingStateLoss();
            return layout;
        }

        final TextView error = layout.findViewById(R.id.imageZoomDialog_error);
        error.setVisibility(View.GONE);
        final ImageView image = layout.findViewById(R.id.imageZoomDialog_image);
        image.setVisibility(View.VISIBLE);

        Glide.with(this).load(BaseCardUrlLoader.extractUrl(url)).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                error.setVisibility(View.VISIBLE);
                image.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                error.setVisibility(View.GONE);
                image.setVisibility(View.VISIBLE);
                return false;
            }
        }).into(image);

        ImageButton open = layout.findViewById(R.id.imageZoomDialog_open);
        open.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))));

        return layout;
    }
}
