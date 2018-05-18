package com.gianlu.pretendyourexyzzy.Dialogs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.FirstLoad;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.PyxException;
import com.gianlu.pretendyourexyzzy.R;

import org.json.JSONException;

public final class Dialogs {

    @NonNull
    @SuppressLint("InflateParams")
    public static AlertDialog.Builder gameOptions(@NonNull Context context, @NonNull Game.Options options, @NonNull FirstLoad firstLoad) {
        ScrollView layout = (ScrollView) LayoutInflater.from(context).inflate(R.layout.dialog_game_options, null, false);

        SuperTextView scoreLimit = layout.findViewById(R.id.gameOptions_scoreLimit);
        scoreLimit.setHtml(R.string.scoreLimit, options.scoreLimit);

        SuperTextView playerLimit = layout.findViewById(R.id.gameOptions_playerLimit);
        playerLimit.setHtml(R.string.playerLimit, options.playersLimit);

        SuperTextView spectatorLimit = layout.findViewById(R.id.gameOptions_spectatorLimit);
        spectatorLimit.setHtml(R.string.spectatorLimit, options.spectatorsLimit);

        SuperTextView timerMultiplier = layout.findViewById(R.id.gameOptions_timerMultiplier);
        timerMultiplier.setHtml(R.string.timerMultiplier, options.timerMultiplier);

        SuperTextView cardSets = layout.findViewById(R.id.gameOptions_cardSets);
        cardSets.setHtml(R.string.cardSets, options.cardSets.isEmpty() ? "<i>none</i>" : CommonUtils.join(firstLoad.createCardSetNamesList(options.cardSets), ", "));

        SuperTextView blankCards = layout.findViewById(R.id.gameOptions_blankCards);
        blankCards.setHtml(R.string.blankCards, options.blanksLimit);

        SuperTextView password = layout.findViewById(R.id.gameOptions_password);
        if (options.password == null || options.password.isEmpty())
            password.setVisibility(View.GONE);
        else
            password.setHtml(R.string.password, options.password);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.gameOptions)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, null);

        return builder;
    }

    @NonNull
    @SuppressLint("InflateParams")
    public static AlertDialog.Builder notEnoughCards(@NonNull Context context, @NonNull PyxException ex) throws JSONException {
        LinearLayout layout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.dialog_cannot_start_game, null, false);

        int wcr = ex.obj.getInt("wcr");
        int bcr = ex.obj.getInt("bcr");
        int wcp = ex.obj.getInt("wcp");
        int bcp = ex.obj.getInt("bcp");

        ((TextView) layout.findViewById(R.id.cannotStartGame_wcr)).setText(String.valueOf(wcr));
        ((TextView) layout.findViewById(R.id.cannotStartGame_bcr)).setText(String.valueOf(bcr));
        ((TextView) layout.findViewById(R.id.cannotStartGame_wcp)).setText(String.valueOf(wcp));
        ((TextView) layout.findViewById(R.id.cannotStartGame_bcp)).setText(String.valueOf(bcp));
        ((ImageView) layout.findViewById(R.id.cannotStartGame_checkBc)).setImageResource(bcp >= bcr ? R.drawable.ic_done_black_48dp : R.drawable.ic_clear_black_48dp);
        ((ImageView) layout.findViewById(R.id.cannotStartGame_checkWc)).setImageResource(wcp >= wcr ? R.drawable.ic_done_black_48dp : R.drawable.ic_clear_black_48dp);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.cannotStartGame)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, null);

        return builder;
    }

    @NonNull
    public static AlertDialog.Builder askText(@NonNull Context context, @NonNull final OnText listener) {
        final EditText text = new EditText(context);

        return new AlertDialog.Builder(context)
                .setTitle(R.string.setBlankCardText)
                .setView(text)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onText(text.getText().toString());
                    }
                }).setNegativeButton(android.R.string.cancel, null);
    }

    @NonNull
    public static AlertDialog.Builder confirmation(@NonNull Context context, @NonNull final OnConfirmed listener) {
        return new AlertDialog.Builder(context)
                .setTitle(R.string.areYouSurePlayCard)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onConfirmed();
                    }
                }).setNegativeButton(android.R.string.no, null);
    }

    public interface OnConfirmed {
        void onConfirmed();
    }

    public interface OnText {
        void onText(@NonNull String text);
    }
}
