package com.gianlu.pretendyourexyzzy.Main.OngoingGame;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.pretendyourexyzzy.NetIO.PyxException;
import com.gianlu.pretendyourexyzzy.R;

import org.json.JSONException;

public final class Dialogs {

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
