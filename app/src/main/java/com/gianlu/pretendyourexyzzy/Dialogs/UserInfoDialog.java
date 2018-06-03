package com.gianlu.pretendyourexyzzy.Dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.WhoisResult;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.NetIO.PyxRequests;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.R;

import java.util.Date;

public class UserInfoDialog extends DialogFragment {
    private OnViewGame listener;

    public static void loadAndShow(@NonNull RegisteredPyx pyx, @NonNull final FragmentActivity activity, @NonNull String name) {
        DialogUtils.showDialog(activity, DialogUtils.progressDialog(activity, R.string.loading));
        pyx.request(PyxRequests.whois(name), new Pyx.OnResult<WhoisResult>() {
            @Override
            public void onDone(@NonNull WhoisResult result) {
                DialogUtils.dismissDialog(activity);
                DialogUtils.showDialog(activity, UserInfoDialog.get(result));
            }

            @Override
            public void onException(@NonNull Exception ex) {
                DialogUtils.dismissDialog(activity);
                Toaster.with(activity).message(R.string.failedLoading).ex(ex).show();
            }
        });
    }

    @NonNull
    public static UserInfoDialog get(@NonNull WhoisResult user) {
        UserInfoDialog dialog = new UserInfoDialog();
        Bundle args = new Bundle();
        args.putSerializable("user", user);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireActivity(), R.style.TitledDialog);
        WhoisResult user = getUser();
        if (user != null) dialog.setTitle(user.nickname);
        return dialog;
    }

    @Nullable
    private WhoisResult getUser() {
        WhoisResult user;
        Bundle args = getArguments();
        if (args == null || (user = (WhoisResult) args.getSerializable("user")) == null)
            return null;
        else
            return user;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnViewGame)
            listener = (OnViewGame) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.dialog_user_info, container, false);

        WhoisResult user = getUser();
        if (user == null) return null;

        SuperTextView sigil = layout.findViewById(R.id.userInfoDialog_sigil);
        sigil.setHtml(R.string.sigil, user.sigil.getFormal(getContext()));

        SuperTextView idCode = layout.findViewById(R.id.userInfoDialog_idCode);
        if (user.idCode == null || user.idCode.isEmpty()) {
            idCode.setVisibility(View.GONE);
        } else {
            idCode.setVisibility(View.VISIBLE);
            idCode.setHtml(R.string.idCode, user.idCode);
        }

        SuperTextView connAt = layout.findViewById(R.id.userInfoDialog_connAt);
        connAt.setHtml(R.string.connectedAt, CommonUtils.getFullDateFormatter().format(new Date(user.connectedAt)));

        SuperTextView idleFrom = layout.findViewById(R.id.userInfoDialog_idle);
        idleFrom.setHtml(R.string.idleFor, CommonUtils.timeFormatter(user.idle / 1000));

        SuperTextView ipAddr = layout.findViewById(R.id.userInfoDialog_ipAddr);
        if (user.ipAddress() == null) {
            ipAddr.setVisibility(View.GONE);
        } else {
            ipAddr.setVisibility(View.VISIBLE);
            ipAddr.setHtml(R.string.ipAddress, user.ipAddress());
        }

        SuperTextView client = layout.findViewById(R.id.userInfoDialog_client);
        if (user.clientName() == null) {
            client.setVisibility(View.GONE);
        } else {
            client.setVisibility(View.VISIBLE);
            client.setHtml(R.string.clientName, user.clientName());
        }

        Button viewGame = layout.findViewById(R.id.userInfoDialog_viewGame);
        SuperTextView game = layout.findViewById(R.id.userInfoDialog_game);
        final Game gameInfo = user.game();
        if (gameInfo == null) {
            game.setVisibility(View.GONE);
            viewGame.setVisibility(View.GONE);
        } else {
            game.setVisibility(View.VISIBLE);

            Object[] args = new Object[3];
            args[0] = gameInfo.host;
            args[1] = gameInfo.players.size();
            args[2] = gameInfo.options.playersLimit;

            if (gameInfo.hasPassword(false)) {
                if (gameInfo.status.isStarted())
                    game.setHtml(R.string.gameStartedLockedDetails, args);
                else
                    game.setHtml(R.string.gameLobbyLockedDetails, args);
            } else {
                if (gameInfo.status.isStarted())
                    game.setHtml(R.string.gameStartedOpenDetails, args);
                else
                    game.setHtml(R.string.gameLobbyOpenDetails, args);
            }

            if (listener != null && listener.canViewGame()) {
                viewGame.setVisibility(View.VISIBLE);
                viewGame.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) {
                            listener.viewGame(gameInfo.gid, gameInfo.hasPassword(false));
                            dismiss();
                        }
                    }
                });
            } else {
                viewGame.setVisibility(View.GONE);
            }
        }

        return layout;
    }

    public interface OnViewGame {
        void viewGame(int gid, boolean locked);

        boolean canViewGame();
    }
}
