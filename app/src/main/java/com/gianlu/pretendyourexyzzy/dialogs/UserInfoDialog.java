package com.gianlu.pretendyourexyzzy.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.Game;
import com.gianlu.pretendyourexyzzy.api.models.WhoisResult;

import java.util.Date;

import static android.view.View.GONE;

public class UserInfoDialog extends DialogFragment {
    private static final String TAG = UserInfoDialog.class.getSimpleName();
    private OnViewGame listener;

    public static void loadAndShow(@NonNull RegisteredPyx pyx, @NonNull FragmentActivity activity, @NonNull String name) {
        DialogUtils.showDialog(activity, DialogUtils.progressDialog(activity, R.string.loading));
        pyx.request(PyxRequests.whois(name), activity, new Pyx.OnResult<WhoisResult>() {
            @Override
            public void onDone(@NonNull WhoisResult result) {
                DialogUtils.dismissDialog(activity);
                DialogUtils.showDialog(activity, UserInfoDialog.get(result), null);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                DialogUtils.dismissDialog(activity);
                Log.e(TAG, "Failed whois.", ex);
                Toaster.with(activity).message(R.string.failedLoading).show();
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
    public void onAttach(@NonNull Context context) {
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

        TextView name = layout.findViewById(R.id.userInfoDialog_name);
        name.setText(user.nickname);

        SuperTextView sigil = layout.findViewById(R.id.userInfoDialog_sigil);
        sigil.setHtml(R.string.sigil, user.sigil.getFormal(requireContext()));

        SuperTextView idCode = layout.findViewById(R.id.userInfoDialog_idCode);
        if (user.idCode.isEmpty()) {
            idCode.setVisibility(GONE);
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
            ipAddr.setVisibility(GONE);
        } else {
            ipAddr.setVisibility(View.VISIBLE);
            ipAddr.setHtml(R.string.ipAddress, user.ipAddress());
        }

        SuperTextView client = layout.findViewById(R.id.userInfoDialog_client);
        if (user.clientName() == null) {
            client.setVisibility(GONE);
        } else {
            client.setVisibility(View.VISIBLE);
            client.setHtml(R.string.clientName, user.clientName());
        }

        Button viewGame = layout.findViewById(R.id.userInfoDialog_viewGame);
        SuperTextView game = layout.findViewById(R.id.userInfoDialog_game);
        Game gameInfo = user.game();
        if (gameInfo == null) {
            game.setVisibility(GONE);
            viewGame.setVisibility(GONE);
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
                CommonUtils.setPaddingDip((View) viewGame.getParent(), null, 0, null, null);
                viewGame.setVisibility(View.VISIBLE);
                viewGame.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.viewGame(gameInfo.gid, gameInfo.hasPassword(false));
                        dismissAllowingStateLoss();
                    }
                });
            } else {
                viewGame.setVisibility(GONE);
            }
        }

        return layout;
    }

    public interface OnViewGame {
        void viewGame(int gid, boolean locked);

        boolean canViewGame();
    }
}
