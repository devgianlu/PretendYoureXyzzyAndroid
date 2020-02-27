package com.gianlu.pretendyourexyzzy.overloaded;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.gianlu.commonutils.bottomsheet.BaseModalBottomSheet;
import com.gianlu.commonutils.bottomsheet.ModalBottomSheetHeaderView;
import com.gianlu.pretendyourexyzzy.overloaded.api.OverloadedApi;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ChatBottomSheet extends BaseModalBottomSheet<OverloadedApi.Chat, ChatBottomSheet.Update> { // TODO

    @Override
    protected void onCreateHeader(@NonNull LayoutInflater inflater, @NonNull ModalBottomSheetHeaderView header, @NonNull OverloadedApi.Chat payload) {

    }

    @Override
    protected void onCreateBody(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull OverloadedApi.Chat payload) {

    }

    @Override
    protected boolean onCustomizeAction(@NonNull FloatingActionButton action, @NonNull OverloadedApi.Chat payload) {
        return false;
    }

    public static class Update {
    }
}
