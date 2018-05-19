package com.gianlu.pretendyourexyzzy.Dialogs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.NetIO.Models.FirstLoad;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.NetIO.PyxException;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

import org.json.JSONException;

import java.util.Objects;

import okhttp3.HttpUrl;

public final class Dialogs {

    @SuppressLint("InflateParams")
    public static AlertDialog addServer(@NonNull final Context context, @Nullable final Pyx.Server server, @NonNull final OnAddServer listener) {
        LinearLayout layout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.dialog_add_server, null, false);

        final TextInputLayout nameField = layout.findViewById(R.id.addServer_name);
        CommonUtils.getEditText(nameField).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                nameField.setErrorEnabled(false);
            }
        });
        final TextInputLayout urlField = layout.findViewById(R.id.addServer_url);
        CommonUtils.getEditText(urlField).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                urlField.setErrorEnabled(false);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.addServer)
                .setView(layout)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(server == null ? R.string.add : R.string.apply, null);

        if (server != null) {
            CommonUtils.setText(nameField, server.name);
            CommonUtils.setText(urlField, server.url.toString());
            builder.setNeutralButton(R.string.remove, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Pyx.Server.removeServer(context, server);
                    listener.removeItem(server);
                }
            });
        }

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialogInterface) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String nameStr = CommonUtils.getText(nameField);
                        if (nameStr.isEmpty() || (server != null && !Objects.equals(server.name, nameStr) && Pyx.Server.hasServer(context, nameStr))) {
                            nameField.setError(context.getString(R.string.invalidServerName));
                            return;
                        }

                        String urlStr = CommonUtils.getText(urlField);
                        HttpUrl url = Pyx.Server.parseUrl(urlStr);
                        if (url == null) {
                            urlField.setError(context.getString(R.string.invalidServerUrl));
                            return;
                        }

                        Pyx.Server server = new Pyx.Server(url, nameStr);
                        try {
                            Pyx.Server.addServer(context, server);
                            listener.loadServers();
                        } catch (JSONException ex) {
                            Toaster.show(context, Utils.Messages.FAILED_ADDING_SERVER, ex);
                        }

                        dialogInterface.dismiss();
                    }
                });
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                listener.startTests();
            }
        });
        return dialog;
    }

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

    public interface OnAddServer {
        void loadServers();

        void removeItem(Pyx.Server server);

        void startTests();
    }

    public interface OnConfirmed {
        void onConfirmed();
    }

    public interface OnText {
        void onText(@NonNull String text);
    }
}
