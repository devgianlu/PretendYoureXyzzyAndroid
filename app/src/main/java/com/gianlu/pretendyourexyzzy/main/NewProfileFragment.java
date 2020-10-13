package com.gianlu.pretendyourexyzzy.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.pretendyourexyzzy.NewMainActivity;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewProfileBinding;
import com.gianlu.pretendyourexyzzy.databinding.ItemStarredCardBinding;
import com.gianlu.pretendyourexyzzy.starred.StarredCardsDatabase;
import com.gianlu.pretendyourexyzzy.starred.StarredCardsDatabase.StarredCard;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NewProfileFragment extends FragmentWithDialog implements NewMainActivity.MainFragment {
    private FragmentNewProfileBinding binding;
    private RegisteredPyx pyx;

    @NonNull
    public static NewProfileFragment get() {
        return new NewProfileFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNewProfileBinding.inflate(inflater, container, false);
        binding.profileFragmentInputs.idCodeInput.setEndIconOnClickListener(v -> CommonUtils.setText(binding.profileFragmentInputs.idCodeInput, CommonUtils.randomString(100)));

        binding.profileFragmentStarredCardsList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        StarredCardsDatabase starredDb = StarredCardsDatabase.get(requireContext());
        List<StarredCard> starredCards = starredDb.getCards(false);
        if (starredCards.isEmpty()) {
            binding.profileFragmentStarredCardsEmpty.setVisibility(View.VISIBLE);
            binding.profileFragmentStarredCardsList.setVisibility(View.GONE);
        } else {
            binding.profileFragmentStarredCardsEmpty.setVisibility(View.GONE);
            binding.profileFragmentStarredCardsList.setVisibility(View.VISIBLE);
            binding.profileFragmentStarredCardsList.setAdapter(new StarredCardsAdapter(starredDb, starredCards));
        }

        // TODO: Load custom decks
        // TODO: Load friends
        // TODO: Load achievements

        return binding.getRoot();
    }

    @Override
    public void onPyxReady(@NotNull RegisteredPyx pyx) {
        this.pyx = pyx;

        CommonUtils.setText(binding.profileFragmentInputs.usernameInput, pyx.user().nickname);
        CommonUtils.setText(binding.profileFragmentInputs.idCodeInput, Prefs.getString(PK.LAST_ID_CODE, null));
    }

    @Override
    public void onPyxInvalid() {
        this.pyx = null;
    }

    @Override
    public boolean goBack() {
        return false;
    }

    @NotNull
    public String getUsername() {
        return CommonUtils.getText(binding.profileFragmentInputs.usernameInput);
    }

    @Nullable
    public String getIdCode() {
        String idCode = CommonUtils.getText(binding.profileFragmentInputs.idCodeInput);
        return idCode.trim().isEmpty() ? null : idCode.trim();
    }

    private class StarredCardsAdapter extends RecyclerView.Adapter<StarredCardsAdapter.ViewHolder> {
        private final StarredCardsDatabase db;
        private final List<StarredCard> list;

        StarredCardsAdapter(StarredCardsDatabase db, List<StarredCard> list) {
            this.db = db;
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StarredCard card = list.get(position);
            holder.binding.starredCardItemText.setHtml(card.textUnescaped());
            holder.binding.starredCardItemUnstar.setOnClickListener(v -> {
                db.remove(card);

                for (int i = 0; i < list.size(); i++) {
                    if (card.equals(list.get(i))) {
                        list.remove(i);
                        notifyItemRemoved(i);
                        return;
                    }
                }
            });
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final ItemStarredCardBinding binding;

            public ViewHolder(@NonNull ViewGroup parent) {
                super(getLayoutInflater().inflate(R.layout.item_starred_card, parent, false));
                binding = ItemStarredCardBinding.bind(itemView);
            }
        }
    }
}
