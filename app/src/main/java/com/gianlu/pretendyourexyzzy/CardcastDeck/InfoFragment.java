package com.gianlu.pretendyourexyzzy.CardcastDeck;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.pretendyourexyzzy.NetIO.Cardcast;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastDeckInfo;
import com.gianlu.pretendyourexyzzy.R;

import me.zhanghai.android.materialratingbar.MaterialRatingBar;

public class InfoFragment extends Fragment implements Cardcast.IResult<CardcastDeckInfo> {
    private FrameLayout layout;
    private LinearLayout container;
    private ProgressBar loading;

    public static InfoFragment getInstance(Context context, String code) {
        InfoFragment fragment = new InfoFragment();
        Bundle args = new Bundle();
        args.putString("code", code);
        args.putString("title", context.getString(R.string.info));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        layout = (FrameLayout) inflater.inflate(R.layout.fragment_cardcast_deck_info, parent, false);
        container = layout.findViewById(R.id.cardcastDeckInfo_container);
        loading = layout.findViewById(R.id.cardcastDeckInfo_loading);

        String code = getArguments().getString("code", null);
        if (code == null) {
            loading.setVisibility(View.GONE);
            container.setVisibility(View.GONE);
            MessageLayout.show(layout, R.string.failedLoading, R.drawable.ic_error_outline_black_48dp);
            return layout;
        }

        Cardcast.get(getContext()).getDeckInfo(code, this);

        return layout;
    }

    @Override
    public void onDone(CardcastDeckInfo result) {
        loading.setVisibility(View.GONE);
        container.setVisibility(View.VISIBLE);
        MessageLayout.hide(layout);

        TextView code = container.findViewById(R.id.cardcastDeckInfo_code);
        code.setText(result.code);
        TextView description = container.findViewById(R.id.cardcastDeckInfo_description);
        description.setText(result.description);
        TextView author = container.findViewById(R.id.cardcastDeckInfo_author);
        if (isAdded()) author.setText(getString(R.string.byUppercase, result.author.username));
        MaterialRatingBar rating = container.findViewById(R.id.cardcastDeckInfo_rating);
        rating.setRating(result.rating);
        rating.setEnabled(false);
        TextView whiteCards = container.findViewById(R.id.cardcastDeckInfo_whiteCards);
        whiteCards.setText(String.valueOf(result.responses));
        TextView blackCards = container.findViewById(R.id.cardcastDeckInfo_blackCards);
        blackCards.setText(String.valueOf(result.calls));
    }

    @Override
    public void onException(Exception ex) {
        Logging.logMe(ex);
        loading.setVisibility(View.GONE);
        container.setVisibility(View.GONE);
        if (isAdded())
            MessageLayout.show(layout, getString(R.string.failedLoading_reason, ex.getMessage()), R.drawable.ic_error_outline_black_48dp);
    }
}
