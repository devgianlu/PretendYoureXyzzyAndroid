package com.gianlu.pretendyourexyzzy.main;

import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.adapters.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.Name;
import com.gianlu.pretendyourexyzzy.api.models.PollMessage;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewPlayersSettingsBinding;

import org.json.JSONException;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class NewPlayersFragment extends NewSettingsFragment.ChildFragment implements Pyx.OnEventListener {
    private static final String TAG = NewPlayersFragment.class.getSimpleName();
    private FragmentNewPlayersSettingsBinding binding;
    private RegisteredPyx pyx;
    private PlayersAdapter adapter;

    private void setPlayersStatus(boolean loading, boolean error) {
        if (loading) {
            binding.playersFragmentList.setAdapter(null);
            binding.playersFragmentListError.setVisibility(View.GONE);
            binding.playersFragmentListLoading.setVisibility(View.VISIBLE);
            binding.playersFragmentListLoading.showShimmer(true);

            binding.playersFragmentSwipeRefresh.setEnabled(false);
        } else if (error) {
            binding.playersFragmentList.setAdapter(null);
            binding.playersFragmentListError.setVisibility(View.VISIBLE);
            binding.playersFragmentListLoading.setVisibility(View.GONE);
            binding.playersFragmentListLoading.hideShimmer();

            binding.playersFragmentSwipeRefresh.setEnabled(true);
        } else {
            binding.playersFragmentListError.setVisibility(View.GONE);
            binding.playersFragmentListLoading.setVisibility(View.GONE);
            binding.playersFragmentListLoading.hideShimmer();

            binding.playersFragmentSwipeRefresh.setEnabled(true);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNewPlayersSettingsBinding.inflate(getLayoutInflater(), container, false);
        binding.playersFragmentBack.setOnClickListener(v -> goBack());

        binding.playersFragmentList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));

        binding.playersFragmentSwipeRefresh.setColorSchemeResources(R.color.appColor_500);
        binding.playersFragmentSwipeRefresh.setOnRefreshListener(() -> {
            if (pyx == null) return;

            setPlayersStatus(true, false);
            pyx.request(PyxRequests.getNamesList())
                    .addOnSuccessListener(this::playersLoaded)
                    .addOnFailureListener(this::failedLoadingPlayers);

            binding.playersFragmentSwipeRefresh.setRefreshing(false);
        });

        createPlayersPlaceholders();

        setPlayersStatus(true, false);

        return binding.getRoot();
    }

    private void createPlayersPlaceholders() {
        Paint paint = new Paint();
        paint.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.montserrat_regular));
        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics()));
        Paint.FontMetrics fm = paint.getFontMetrics();

        int height = (int) Math.ceil(fm.bottom - fm.top + fm.leading);
        int dp12 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());

        for (int i = 0; i < 40; i++) {
            int width = dp12 * ThreadLocalRandom.current().nextInt(8, 20);

            View view = new View(requireContext());
            view.setBackgroundResource(R.drawable.placeholder_name_item);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
            params.setMargins(0, 0, 0, dp12);
            binding.playersFragmentListLoadingChild.addView(view, params);
        }
    }

    private void playersLoaded(@NonNull List<Name> names) {
        adapter = new PlayersAdapter(names);
        binding.playersFragmentList.setAdapter(adapter);
        setPlayersStatus(false, false);
    }

    private void failedLoadingPlayers(@NonNull Exception ex) {
        Log.e(TAG, "Failed loading players names.", ex);
        setPlayersStatus(false, true);
    }

    @Override
    protected void onPyxReady(@NonNull RegisteredPyx pyx) {
        this.pyx = pyx;
        this.pyx.polling().addListener(this);

        pyx.request(PyxRequests.getNamesList())
                .addOnSuccessListener(this::playersLoaded)
                .addOnFailureListener(this::failedLoadingPlayers);

        binding.playersFragmentSwipeRefresh.setEnabled(true);
    }

    @Override
    protected void onPyxInvalid() {
        if (pyx != null) pyx.polling().removeListener(this);
        this.pyx = null;

        if (binding != null) {
            setPlayersStatus(true, false);
            binding.playersFragmentSwipeRefresh.setEnabled(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (pyx != null) pyx.polling().removeListener(this);
        this.pyx = null;
    }

    @Override
    public void onPollMessage(@NonNull PollMessage msg) throws JSONException {
        switch (msg.event) {
            case NEW_PLAYER:
                if (adapter != null)
                    adapter.itemChangedOrAdded(new Name(msg.obj.getString("n")));
                break;
            case PLAYER_LEAVE:
                if (adapter != null)
                    adapter.removeItem(msg.obj.getString("n"));
                break;
            case GAME_LIST_REFRESH:
                pyx.request(PyxRequests.getNamesList())
                        .addOnSuccessListener((names) -> {
                            if (adapter != null) adapter.itemsChanged(names);
                        })
                        .addOnFailureListener((ex) -> Log.e(TAG, "Failed refreshing names list."));
                break;
        }
    }

    public enum Sorting {
        AZ, ZA
    }

    private class PlayersAdapter extends OrderedRecyclerViewAdapter<PlayersAdapter.ViewHolder, Name, Sorting, Void> {

        PlayersAdapter(@NonNull List<Name> list) {
            super(list, Sorting.AZ);
        }

        @Override
        protected boolean matchQuery(@NonNull Name item, @Nullable String query) {
            return true;
        }

        @Override
        protected void onSetupViewHolder(@NonNull ViewHolder holder, int position, @NonNull Name name) {
            ((SuperTextView) holder.itemView).setHtml(name.sigil() == Name.Sigil.NORMAL_USER ? name.withSigil() : (SuperTextView.makeBold(name.sigil().symbol()) + name.noSigil()));

            // TODO: Handle player click
            // TODO: Handle Overloaded users
        }

        @Override
        protected void onUpdateViewHolder(@NonNull ViewHolder holder, int position, @NonNull Name payload) {
        }

        @Override
        protected void shouldUpdateItemCount(int count) {
        }

        void removeItem(@NonNull String nameStr) {
            for (int i = 0; i < originalObjs.size(); i++) {
                Name name = originalObjs.get(i);
                if (nameStr.equals(name.noSigil())) {
                    removeItem(name);
                    break;
                }
            }
        }

        @NonNull
        @Override
        public Comparator<Name> getComparatorFor(Sorting sorting) {
            switch (sorting) {
                default:
                case AZ:
                    return new Name.AzComparator();
                case ZA:
                    return new Name.ZaComparator();
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final SuperTextView text;

            ViewHolder(@NonNull ViewGroup parent) {
                super(NewPlayersFragment.this.getLayoutInflater().inflate(R.layout.item_name, parent, false));
                text = (SuperTextView) itemView;
            }
        }
    }
}
