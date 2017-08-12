package com.gianlu.pretendyourexyzzy.Main;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.SearchView;

import com.gianlu.cardcastapi.Cardcast;
import com.gianlu.cardcastapi.Models.Deck;
import com.gianlu.cardcastapi.Models.Decks;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.InfiniteRecyclerView;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.pretendyourexyzzy.Adapters.CardcastDecksAdapter;
import com.gianlu.pretendyourexyzzy.CardcastHelper;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

import java.util.ArrayList;
import java.util.List;

public class CardcastFragment extends Fragment implements CardcastHelper.IDecks, CardcastDecksAdapter.IAdapter, MenuItem.OnActionExpandListener, SearchView.OnQueryTextListener, SearchView.OnCloseListener {
    private final static int LIMIT = 12;
    private FrameLayout layout;
    private InfiniteRecyclerView list;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar loading;
    private CardcastHelper cardcast;
    private SearchView searchView;
    private CardcastHelper.Search search = new CardcastHelper.Search(null, null, Cardcast.Direction.DESCENDANT, Cardcast.Sort.RATING, true);

    public static CardcastFragment getInstance() {
        CardcastFragment fragment = new CardcastFragment();
        fragment.setHasOptionsMenu(true);
        return fragment;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.cardcast_fragment, menu);
        SearchManager searchManager = (android.app.SearchManager) getContext().getSystemService(Context.SEARCH_SERVICE);
        MenuItem item = menu.findItem(R.id.cardcastFragment_search);
        item.setOnActionExpandListener(this);
        searchView = (SearchView) item.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setOnCloseListener(this);
        searchView.setOnQueryTextListener(this);

        SubMenu sortingMenu = menu.findItem(R.id.cardcastFragment_sort).getSubMenu();
        inflater.inflate(R.menu.cardcast_decks_sort, sortingMenu);
        sortingMenu.setGroupCheckable(0, true, true);
    }

    private void showCategoriesDialog() {
        final Cardcast.Category[] filters = Cardcast.Category.values();
        CharSequence[] stringFilters = new CharSequence[filters.length];

        for (int i = 0; i < filters.length; i++)
            stringFilters[i] = Utils.getCategoryFormal(getContext(), filters[i]);

        final boolean[] checkedFilters = new boolean[filters.length];
        if (search.categories != null) {
            for (Cardcast.Category category : search.categories)
                checkedFilters[CommonUtils.indexOf(filters, category)] = true;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.categories)
                .setMultiChoiceItems(stringFilters, checkedFilters, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        checkedFilters[which] = isChecked;
                    }
                })
                .setNeutralButton(R.string.clearAll, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        search = new CardcastHelper.Search(search.query, null, search.direction, search.sort, search.nsfw);
                        refreshAdapter();
                    }
                })
                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        List<Cardcast.Category> toApplyFilters = new ArrayList<>();
                        for (int i = 0; i < checkedFilters.length; i++)
                            if (checkedFilters[i]) toApplyFilters.add(filters[i]);

                        search = new CardcastHelper.Search(search.query, toApplyFilters.isEmpty() ? null : toApplyFilters, search.direction, search.sort, search.nsfw);
                        refreshAdapter();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);

        CommonUtils.showDialog(getActivity(), builder);
    }

    private void refreshAdapter() {
        loading.setVisibility(View.VISIBLE);
        swipeRefresh.setVisibility(View.GONE);
        MessageLayout.hide(layout);

        cardcast.getDecks(search, LIMIT, 0, this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cardcastFragment_showNsfw:
                item.setChecked(!item.isChecked());

                search = new CardcastHelper.Search(search.query, search.categories, search.direction, search.sort, item.isChecked());
                refreshAdapter();
                return true;
            case R.id.cardcastFragment_categories:
                showCategoriesDialog();
                return true;
            // Sorting
            case R.id.cardcastFragment_sort_rating:
                handleSort(Cardcast.Sort.RATING);
                return true;
            case R.id.cardcastFragment_sort_name:
                handleSort(Cardcast.Sort.NAME);
                return true;
            case R.id.cardcastFragment_sort_newest:
                handleSort(Cardcast.Sort.NEWEST);
                return true;
            case R.id.cardcastFragment_sort_size:
                handleSort(Cardcast.Sort.SIZE);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void handleSort(Cardcast.Sort sort) {
        search = new CardcastHelper.Search(search.query, search.categories, search.direction, sort, search.nsfw);
        refreshAdapter();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = (FrameLayout) inflater.inflate(R.layout.recycler_view_layout, container, false);
        layout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary_background));
        loading = layout.findViewById(R.id.recyclerViewLayout_loading);
        swipeRefresh = layout.findViewById(R.id.recyclerViewLayout_swipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.colorAccent);
        list = layout.findViewById(R.id.recyclerViewLayout_list);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        cardcast = new CardcastHelper(getContext(), Cardcast.get());
        cardcast.getDecks(search, LIMIT, 0, this);

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                cardcast.getDecks(search, LIMIT, 0, CardcastFragment.this);
            }
        });

        return layout;
    }

    @Override
    public void onDone(CardcastHelper.Search search, Decks decks) {
        swipeRefresh.setRefreshing(false);
        loading.setVisibility(View.GONE);
        swipeRefresh.setVisibility(View.VISIBLE);
        MessageLayout.hide(layout);

        list.setAdapter(new CardcastDecksAdapter(getContext(), cardcast, search, decks, LIMIT, this));
    }

    @Override
    public void onException(Exception ex) {
        Logging.logMe(getContext(), ex);
        swipeRefresh.setRefreshing(false);
        loading.setVisibility(View.GONE);
        swipeRefresh.setVisibility(View.GONE);
        if (isAdded())
            MessageLayout.show(layout, getString(R.string.failedLoading_reason, ex.getMessage()), R.drawable.ic_error_outline_black_48dp);
    }

    @Override
    public void onDeckSelected(Deck deck) { // TODO

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
        search = new CardcastHelper.Search(query == null || query.isEmpty() ? null : query,
                search.categories, search.direction, search.sort, search.nsfw);

        refreshAdapter();
        return true;
    }

    @Override
    public boolean onClose() {
        searchView.setQuery(null, true);
        return false;
    }
}
