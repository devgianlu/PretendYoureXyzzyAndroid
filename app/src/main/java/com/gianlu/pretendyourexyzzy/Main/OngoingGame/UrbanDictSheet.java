package com.gianlu.pretendyourexyzzy.Main.OngoingGame;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.BottomSheet.ModalBottomSheetHeaderView;
import com.gianlu.commonutils.BottomSheet.ThemedModalBottomSheet;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.Toaster;
import com.gianlu.commonutils.misc.MessageView;
import com.gianlu.pretendyourexyzzy.Adapters.DefinitionsAdapter;
import com.gianlu.pretendyourexyzzy.NetIO.UrbanDictionary.Definition;
import com.gianlu.pretendyourexyzzy.NetIO.UrbanDictionary.Definitions;
import com.gianlu.pretendyourexyzzy.NetIO.UrbanDictionary.UrbanDictApi;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.ThisApplication;
import com.gianlu.pretendyourexyzzy.Utils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class UrbanDictSheet extends ThemedModalBottomSheet<String, Definitions> implements DefinitionsAdapter.Listener {
    private RecyclerView list;
    private MessageView message;

    @NonNull
    public static UrbanDictSheet get() {
        return new UrbanDictSheet();
    }

    @Override
    protected void onCreateBody(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull String payload) {
        inflater.inflate(R.layout.sheet_urban_dict, parent, true);

        message = parent.findViewById(R.id.urbanDictSheet_message);
        list = parent.findViewById(R.id.urbanDictSheet_list);
        list.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        list.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        UrbanDictApi.get().define(payload, getActivity(), new UrbanDictApi.OnDefine() {
            @Override
            public void onResult(@NonNull Definitions result) {
                update(result);
                isLoading(false);

                ThisApplication.sendAnalytics(Utils.ACTION_OPEN_URBAN_DICT);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading).ex(ex));
                dismissAllowingStateLoss();
            }
        });
    }

    @Override
    protected void onRequestedUpdate(@NonNull Definitions payload) {
        if (payload.isEmpty()) {
            message.info(R.string.noDefinitions);
            list.setVisibility(View.GONE);
        } else {
            message.hide();
            list.setVisibility(View.VISIBLE);
            list.setAdapter(new DefinitionsAdapter(requireContext(), payload, this));
        }
    }

    @Override
    protected void onCustomizeToolbar(@NonNull Toolbar toolbar, @NonNull String payload) {
        toolbar.setTitle(payload);
        toolbar.setBackgroundResource(R.color.urbanPrimaryDark);
    }

    @Override
    protected boolean onCreateHeader(@NonNull LayoutInflater inflater, @NonNull ModalBottomSheetHeaderView parent, @NonNull String payload) {
        return false;
    }

    @Override
    protected boolean onCustomizeAction(@NonNull FloatingActionButton action, @NonNull String payload) {
        return false;
    }

    @Override
    protected int getCustomTheme(@NonNull String payload) {
        return R.style.UrbanDictTheme;
    }

    @Override
    public void onDefinitionSelected(@NonNull Definition definition) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(definition.permalink)));
    }
}
