package com.gianlu.pretendyourexyzzy;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.gianlu.commonutils.SuperTextView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.WhoisResult;

public class UserInfoDialog extends DialogFragment {

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
        else return user;
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

        return layout;
    }
}
