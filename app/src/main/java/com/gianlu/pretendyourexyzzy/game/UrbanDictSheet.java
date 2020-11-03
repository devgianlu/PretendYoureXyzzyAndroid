package com.gianlu.pretendyourexyzzy.game;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.bottomsheet.ModalBottomSheetHeaderView;
import com.gianlu.commonutils.bottomsheet.ThemedModalBottomSheet;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.misc.MessageView;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.ThisApplication;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.api.urbandictionary.Definition;
import com.gianlu.pretendyourexyzzy.api.urbandictionary.Definitions;
import com.gianlu.pretendyourexyzzy.api.urbandictionary.UrbanDictApi;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class UrbanDictSheet extends ThemedModalBottomSheet<String, Definitions> {
    private static final String TAG = UrbanDictSheet.class.getSimpleName();
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
                Log.e(TAG, "Failed getting definition.", ex);
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading));
                dismissAllowingStateLoss();
            }
        });
    }

    @Override
    protected void onReceivedUpdate(@NonNull Definitions payload) {
        if (payload.isEmpty()) {
            message.info(R.string.noDefinitions);
            list.setVisibility(View.GONE);
        } else {
            message.hide();
            list.setVisibility(View.VISIBLE);
            list.setAdapter(new DefinitionsAdapter(requireContext(), payload));
        }
    }

    @Override
    protected void onCreateHeader(@NonNull LayoutInflater inflater, @NonNull ModalBottomSheetHeaderView parent, @NonNull String payload) {
        parent.setTitle(payload);
        parent.setBackgroundColorRes(R.color.urbanPrimaryDark);
    }

    @Override
    protected boolean onCustomizeAction(@NonNull FloatingActionButton action, @NonNull String payload) {
        return false;
    }

    @Override
    protected int getCustomTheme(@NonNull String payload) {
        return R.style.UrbanDictTheme;
    }

    private class DefinitionsAdapter extends RecyclerView.Adapter<DefinitionsAdapter.ViewHolder> {
        private final LayoutInflater inflater;
        private final Definitions definitions;

        DefinitionsAdapter(@NonNull Context context, @NonNull Definitions definitions) {
            this.inflater = LayoutInflater.from(context);
            this.definitions = definitions;
            setHasStableIds(true);
        }

        @Override
        public long getItemId(int position) {
            return definitions.get(position).id;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final Definition def = definitions.get(position);
            holder.definition.setText(def.definition);
            holder.word.setText(def.word);
            if (def.example.isEmpty()) {
                holder.example.setVisibility(View.GONE);
            } else {
                holder.example.setVisibility(View.VISIBLE);
                holder.example.setText(def.example);
            }

            holder.itemView.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(def.permalink)));
                } catch (ActivityNotFoundException ex) {
                    DialogUtils.showToast(getContext(), Toaster.build().message(R.string.missingWebBrowser));
                }
            });
        }

        @Override
        public int getItemCount() {
            return definitions.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView definition;
            final TextView word;
            final TextView example;

            ViewHolder(ViewGroup parent) {
                super(inflater.inflate(R.layout.item_urban_def, parent, false));

                definition = itemView.findViewById(R.id.urbanDefItem_definition);
                word = itemView.findViewById(R.id.urbanDefItem_word);
                example = itemView.findViewById(R.id.urbanDefItem_example);
            }
        }
    }
}
