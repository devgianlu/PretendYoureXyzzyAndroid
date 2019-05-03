package com.gianlu.pretendyourexyzzy.SpareActivities.CardcastDeck;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.gianlu.commonutils.CasualViews.MessageView;
import com.gianlu.commonutils.Logging;
import com.gianlu.pretendyourexyzzy.NetIO.Cardcast;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastDeckInfo;
import com.gianlu.pretendyourexyzzy.R;

import me.zhanghai.android.materialratingbar.MaterialRatingBar;

public class InfoFragment extends Fragment implements Cardcast.OnResult<CardcastDeckInfo> {
    private LinearLayout container;
    private ProgressBar loading;
    private MessageView message;

    @NonNull
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
        FrameLayout layout = (FrameLayout) inflater.inflate(R.layout.fragment_cardcast_deck_info, parent, false);
        container = layout.findViewById(R.id.cardcastDeckInfo_container);
        loading = layout.findViewById(R.id.cardcastDeckInfo_loading);
        message = layout.findViewById(R.id.cardcastDeckInfo_message);

        Bundle args = getArguments();
        String code;
        if (args == null || (code = args.getString("code", null)) == null) {
            loading.setVisibility(View.GONE);
            container.setVisibility(View.GONE);
            message.setError(R.string.failedLoading);
            return layout;
        }

        Cardcast.get().getDeckInfo(code, null, this);

        return layout;
    }

    @Override
    public void onDone(@NonNull CardcastDeckInfo result) {
        if (!isAdded()) return;

        loading.setVisibility(View.GONE);
        container.setVisibility(View.VISIBLE);
        message.hide();

        TextView code = container.findViewById(R.id.cardcastDeckInfo_code);
        code.setText(result.code);
        TextView description = container.findViewById(R.id.cardcastDeckInfo_description);
        description.setText(result.description);
        TextView author = container.findViewById(R.id.cardcastDeckInfo_author);
        author.setText(getString(R.string.byUppercase, result.author.username));
        MaterialRatingBar rating = container.findViewById(R.id.cardcastDeckInfo_rating);
        rating.setRating(result.rating);
        rating.setEnabled(false);
        TextView whiteCards = container.findViewById(R.id.cardcastDeckInfo_whiteCards);
        whiteCards.setText(String.valueOf(result.responses));
        TextView blackCards = container.findViewById(R.id.cardcastDeckInfo_blackCards);
        blackCards.setText(String.valueOf(result.calls));
    }

    @Override
    public void onException(@NonNull Exception ex) {
        Logging.log(ex);
        loading.setVisibility(View.GONE);
        container.setVisibility(View.GONE);
        message.setError(R.string.failedLoading_reason, ex.getMessage());
    }
}
