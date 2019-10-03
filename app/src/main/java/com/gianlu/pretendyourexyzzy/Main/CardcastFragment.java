package com.gianlu.pretendyourexyzzy.Main;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.pretendyourexyzzy.Adapters.CardcastDecksAdapter;
import com.gianlu.pretendyourexyzzy.NetIO.Cardcast;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastDeck;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastDecks;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.SpareActivities.CardcastDeckActivity;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CardcastFragment extends Fragment implements Cardcast.OnDecks, CardcastDecksAdapter.Listener, MenuItem.OnActionExpandListener, SearchView.OnQueryTextListener, SearchView.OnCloseListener {
    private final static int LIMIT = 12;
    private RecyclerMessageView layout;
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
    public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater) {
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

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
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
        layout.startLoading();
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
        layout = new RecyclerMessageView(requireContext());
        layout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary_background));
        layout.linearLayoutManager(RecyclerView.VERTICAL, false);

        cardcast = Cardcast.get();
        layout.enableSwipeRefresh(() -> cardcast.getDecks(search, LIMIT, 0, null, this), R.color.colorAccent);

        cardcast.getDecks(search, LIMIT, 0, null, this);

        return layout;
    }

    @Override
    public void onDone(@NonNull Cardcast.Search search, @NonNull CardcastDecks decks) {
        if (isDetached() || !isAdded()) return;

        if (decks.isEmpty())
            layout.showInfo(R.string.searchNoDecks);
        else
            layout.loadListData(new CardcastDecksAdapter(getContext(), cardcast, search, decks, LIMIT, this));
    }

    @Override
    public void onException(@NonNull Exception ex) {
        Logging.log(ex);
        layout.showError(R.string.failedLoading_reason, ex.getMessage());
    }

    @Override
    public void onDeckSelected(@NonNull CardcastDeck deck) {
        CardcastDeckActivity.startActivity(getContext(), deck);
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
}
