package com.gianlu.pretendyourexyzzy.main;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.misc.InfiniteRecyclerView;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.activities.CardcastDeckActivity;
import com.gianlu.pretendyourexyzzy.api.Cardcast;
import com.gianlu.pretendyourexyzzy.api.models.CardcastCard;
import com.gianlu.pretendyourexyzzy.api.models.CardcastDeck;
import com.gianlu.pretendyourexyzzy.api.models.CardcastDecks;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import me.zhanghai.android.materialratingbar.MaterialRatingBar;

public class CardcastFragment extends Fragment implements Cardcast.OnDecks, MenuItem.OnActionExpandListener, SearchView.OnQueryTextListener, SearchView.OnCloseListener {
    private final static int LIMIT = 12;
    private static final String TAG = CardcastFragment.class.getSimpleName();
    private RecyclerMessageView rmv;
    private Cardcast cardcast;
    private SearchView searchView;
    private Cardcast.Search search = new Cardcast.Search(null, null, Cardcast.Direction.DESCENDANT, Cardcast.Sort.RATING, true);

    @NonNull
    public static CardcastFragment getInstance() {
        CardcastFragment fragment = new CardcastFragment();
        fragment.setHasOptionsMenu(true);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (getContext() == null) return;

        inflater.inflate(R.menu.cardcast_fragment, menu);
        SearchManager searchManager = (android.app.SearchManager) getContext().getSystemService(Context.SEARCH_SERVICE);
        MenuItem item = menu.findItem(R.id.cardcastFragment_search);
        item.setOnActionExpandListener(this);
        searchView = (SearchView) item.getActionView();

        if (searchManager != null && getActivity() != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
            searchView.setIconifiedByDefault(false);
            searchView.setOnCloseListener(this);
            searchView.setOnQueryTextListener(this);
        }

        SubMenu sortingMenu = menu.findItem(R.id.cardcastFragment_sort).getSubMenu();
        inflater.inflate(R.menu.cardcast_decks_sort, sortingMenu);
        sortingMenu.setGroupCheckable(0, true, true);
    }

    private void showCategoriesDialog() {
        if (getContext() == null) return;

        final Cardcast.Category[] filters = Cardcast.Category.values();
        CharSequence[] stringFilters = new CharSequence[filters.length];

        for (int i = 0; i < filters.length; i++)
            stringFilters[i] = filters[i].getFormal(getContext());

        final boolean[] checkedFilters = new boolean[filters.length];
        if (search.categories != null) {
            for (Cardcast.Category category : search.categories)
                checkedFilters[CommonUtils.indexOf(filters, category)] = true;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(R.string.categories)
                .setMultiChoiceItems(stringFilters, checkedFilters, (dialog, which, isChecked) -> checkedFilters[which] = isChecked)
                .setNeutralButton(R.string.clearAll, (dialogInterface, i) -> {
                    search = new Cardcast.Search(search.query, null, search.direction, search.sort, search.nsfw);
                    refreshAdapter();
                })
                .setPositiveButton(R.string.apply, (dialog, which) -> {
                    List<Cardcast.Category> toApplyFilters = new ArrayList<>();
                    for (int i = 0; i < checkedFilters.length; i++)
                        if (checkedFilters[i]) toApplyFilters.add(filters[i]);

                    search = new Cardcast.Search(search.query, toApplyFilters.isEmpty() ? null : toApplyFilters, search.direction, search.sort, search.nsfw);
                    refreshAdapter();
                })
                .setNegativeButton(android.R.string.cancel, null);

        DialogUtils.showDialog(getActivity(), builder);
    }

    private void refreshAdapter() {
        rmv.startLoading();
        cardcast.getDecks(search, LIMIT, 0, null, this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cardcastFragment_showNsfw:
                item.setChecked(!item.isChecked());
                search = new Cardcast.Search(search.query, search.categories, search.direction, search.sort, item.isChecked());
                refreshAdapter();
                return true;
            case R.id.cardcastFragment_categories:
                showCategoriesDialog();
                return true;
            // Sorting
            case R.id.cardcastFragment_sort_rating:
                handleSort(item, Cardcast.Sort.RATING);
                return true;
            case R.id.cardcastFragment_sort_name:
                handleSort(item, Cardcast.Sort.NAME);
                return true;
            case R.id.cardcastFragment_sort_newest:
                handleSort(item, Cardcast.Sort.NEWEST);
                return true;
            case R.id.cardcastFragment_sort_size:
                handleSort(item, Cardcast.Sort.SIZE);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void handleSort(MenuItem item, Cardcast.Sort sort) {
        item.setChecked(true);
        search = new Cardcast.Search(search.query, search.categories, search.direction, sort, search.nsfw);
        refreshAdapter();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rmv = new RecyclerMessageView(requireContext());
        rmv.linearLayoutManager(RecyclerView.VERTICAL, false);

        cardcast = Cardcast.get();
        rmv.enableSwipeRefresh(() -> cardcast.getDecks(search, LIMIT, 0, null, this), R.color.colorAccent);

        cardcast.getDecks(search, LIMIT, 0, null, this);

        return rmv;
    }

    @Override
    public void onDone(@NonNull Cardcast.Search search, @NonNull CardcastDecks decks) {
        if (getContext() == null) return;

        if (decks.isEmpty())
            rmv.showInfo(R.string.searchNoDecks);
        else
            rmv.loadListData(new CardcastDecksAdapter(getContext(), cardcast, search, decks, LIMIT));
    }

    private static final String TAG = CardcastFragment.class.getSimpleName();

    @Override
    public void onException(@NonNull Exception ex) {
        Log.e(TAG, "Search failed.", ex);
        rmv.showError(R.string.failedLoading_reason, ex.getMessage());
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        onClose();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        search = new Cardcast.Search(query == null || query.isEmpty() ? null : query, search.categories, search.direction, search.sort, search.nsfw);
        refreshAdapter();
        return true;
    }

    @Override
    public boolean onClose() {
        searchView.setQuery(null, true);
        search = new Cardcast.Search(null, search.categories, search.direction, search.sort, search.nsfw);
        refreshAdapter();
        return false;
    }

    private class CardcastDecksAdapter extends InfiniteRecyclerView.InfiniteAdapter<CardcastDecksAdapter.ViewHolder, CardcastDeck> {
        private final LayoutInflater inflater;
        private final Cardcast cardcast;
        private final Cardcast.Search search;
        private final int limit;
        private final Random random = ThreadLocalRandom.current();

        CardcastDecksAdapter(Context context, Cardcast cardcast, Cardcast.Search search, CardcastDecks items, int limit) {
            super(context, new Config<CardcastDeck>().items(items).undeterminedPages().noSeparators());
            this.inflater = LayoutInflater.from(context);
            this.cardcast = cardcast;
            this.search = search;
            this.limit = limit;
        }

        @Nullable
        @Override
        protected Date getDateFromItem(CardcastDeck item) {
            return null;
        }

        @Override
        protected void userBindViewHolder(@NonNull ViewHolder holder, @NonNull ItemEnclosure<CardcastDeck> item, int position) {
            final CardcastDeck deck = item.getItem();

            holder.name.setText(deck.name);
            CommonUtils.setText(holder.author, R.string.byLowercase, deck.author.username);
            holder.nsfw.setVisibility(deck.hasNsfwCards ? View.VISIBLE : View.GONE);

            if (deck.sampleCalls != null && !deck.sampleCalls.isEmpty()
                    && deck.sampleResponses != null && !deck.sampleResponses.isEmpty()) {
                CardcastCard exampleBlackCard = deck.sampleCalls.get(random.nextInt(deck.sampleCalls.size()));
                CardcastCard exampleWhiteCard = deck.sampleResponses.get(random.nextInt(deck.sampleResponses.size()));
                holder.example.setHtml(Utils.composeCardcastDeckSentence(exampleBlackCard, exampleWhiteCard));
                holder.example.setVisibility(View.VISIBLE);
            } else {
                holder.example.setVisibility(View.GONE);
            }

            holder.rating.setRating(deck.rating);
            holder.rating.setEnabled(false);
            holder.blackCards.setText(String.valueOf(deck.calls));
            holder.whiteCards.setText(String.valueOf(deck.responses));

            holder.itemView.setOnClickListener(view -> CardcastDeckActivity.startActivity(getContext(), deck));

            CommonUtils.setRecyclerViewTopMargin(holder);
        }

        @NonNull
        @Override
        protected RecyclerView.ViewHolder createViewHolder(@NonNull ViewGroup parent) {
            return new ViewHolder(parent);
        }

        @Override
        protected void moreContent(int page, @NonNull ContentProvider<CardcastDeck> provider) {
            cardcast.getDecks(search, limit, limit * page, null, new Cardcast.OnDecks() {
                @Override
                public void onDone(@NonNull Cardcast.Search search, @NonNull CardcastDecks decks) {
                    provider.onMoreContent(decks);
                }

                @Override
                public void onException(@NonNull Exception ex) {
                    provider.onFailed(ex);
                }
            });
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final SuperTextView example;
            final TextView name;
            final TextView author;
            final View nsfw;
            final MaterialRatingBar rating;
            final TextView blackCards;
            final TextView whiteCards;

            ViewHolder(ViewGroup parent) {
                super(inflater.inflate(R.layout.item_cardcast_deck, parent, false));

                example = itemView.findViewById(R.id.cardcastDeckItem_example);
                name = itemView.findViewById(R.id.cardcastDeckItem_name);
                author = itemView.findViewById(R.id.cardcastDeckItem_author);
                nsfw = itemView.findViewById(R.id.cardcastDeckItem_nsfw);
                rating = itemView.findViewById(R.id.cardcastDeckItem_rating);
                blackCards = itemView.findViewById(R.id.cardcastDeckItem_blackCards);
                whiteCards = itemView.findViewById(R.id.cardcastDeckItem_whiteCards);
            }
        }
    }
}
