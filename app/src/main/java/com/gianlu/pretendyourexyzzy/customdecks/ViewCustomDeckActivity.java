package com.gianlu.pretendyourexyzzy.customdecks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.viewpager.widget.ViewPager;

import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.adapters.PagerAdapter;
import com.gianlu.pretendyourexyzzy.api.crcast.CrCastApi;
import com.gianlu.pretendyourexyzzy.api.crcast.CrCastDeck;
import com.gianlu.pretendyourexyzzy.api.models.Deck;
import com.gianlu.pretendyourexyzzy.customdecks.view.BlackCardsFragment;
import com.gianlu.pretendyourexyzzy.customdecks.view.GeneralInfoFragment;
import com.gianlu.pretendyourexyzzy.customdecks.view.WhiteCardsFragment;
import com.google.android.material.tabs.TabLayout;

import org.jetbrains.annotations.NotNull;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.OverloadedApi.OverloadedServerException;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;
import xyz.gianlu.pyxoverloaded.callback.GeneralCallback;
import xyz.gianlu.pyxoverloaded.model.UserProfile.CustomDeckWithCards;

public class ViewCustomDeckActivity extends ActivityWithDialog implements AbsCardsFragment.Listener {
    private static final String TAG = ViewCustomDeckActivity.class.getSimpleName();
    private CustomDecksDatabase db;
    private ViewPager pager;
    private TabLayout tabs;
    private GeneralInfoFragment generalInfoFragment;
    private String shareCode;
    private String owner;
    private CustomDeckWithCards customDeck = null;
    private ProgressBar loading;

    public static void startActivitySearch(@NonNull Context context, @NonNull Deck deck) {
        Intent intent = new Intent(context, ViewCustomDeckActivity.class);
        intent.putExtra("search", true);
        intent.putExtra("crCast", false);
        intent.putExtra("deckName", deck.name);
        intent.putExtra("watermark", deck.watermark);
        intent.putExtra("desc", deck.description);
        intent.putExtra("blackCards", deck.blackCards);
        intent.putExtra("whiteCards", deck.whiteCards);
        context.startActivity(intent);
    }

    public static void startActivityCrCast(@NonNull Context context, @NonNull String deckName, @NonNull String deckCode) {
        Intent intent = new Intent(context, ViewCustomDeckActivity.class);
        intent.putExtra("crCast", true);
        intent.putExtra("deckCode", deckCode);
        intent.putExtra("deckName", deckName);
        context.startActivity(intent);
    }

    public static void startActivity(@NotNull Context context, @NotNull String owner, @NotNull String deckName, @NonNull String shareCode) {
        Intent intent = new Intent(context, ViewCustomDeckActivity.class);
        intent.putExtra("search", false);
        intent.putExtra("crCast", false);
        intent.putExtra("owner", owner);
        intent.putExtra("shareCode", shareCode);
        intent.putExtra("deckName", deckName);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_view_custom_deck);

        if (getIntent().hasExtra("owner"))
            setTitle(getIntent().getStringExtra("deckName") + " - " + getIntent().getStringExtra("owner"));
        else
            setTitle(getIntent().getStringExtra("deckName"));

        setSupportActionBar(findViewById(R.id.editViewCustomDeck_toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        loading = findViewById(R.id.editViewCustomDeck_loading);
        loading.setVisibility(View.VISIBLE);

        pager = findViewById(R.id.editViewCustomDeck_pager);
        pager.setOffscreenPageLimit(3);

        tabs = findViewById(R.id.editViewCustomDeck_tabs);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        db = CustomDecksDatabase.get(this);

        if (getIntent().getBooleanExtra("crCast", false)) {
            String deckCode = getIntent().getStringExtra("deckCode");
            if (deckCode == null) {
                onBackPressed();
                return;
            }

            CrCastApi.get().getDeck(deckCode, db, this, new CrCastApi.DeckCallback() {
                @Override
                public void onDeck(@NonNull CrCastDeck deck) {
                    deckLoaded(deck);
                }

                @Override
                public void onException(@NonNull Exception ex) {
                    Log.e(TAG, "Failed loading CrCast deck.", ex);
                    Toaster.with(ViewCustomDeckActivity.this).message(R.string.failedLoading).show();
                    onBackPressed();
                }
            });
        } else if (getIntent().getBooleanExtra("search", false)) {
            String deckName = getIntent().getStringExtra("deckName");
            String watermark = getIntent().getStringExtra("watermark");
            String desc = getIntent().getStringExtra("desc");
            int blackCards = getIntent().getIntExtra("blackCards", -1);
            int whiteCards = getIntent().getIntExtra("whiteCards", -1);

            if (deckName == null || watermark == null || desc == null || blackCards == -1 || whiteCards == -1) {
                onBackPressed();
                return;
            }

            OverloadedSyncApi.get().searchPublicCustomDeck(deckName, watermark, desc, blackCards, whiteCards, this, new GeneralCallback<CustomDeckWithCards>() {
                @Override
                public void onResult(@NonNull CustomDeckWithCards result) {
                    deckLoaded(result);
                }

                @Override
                public void onFailed(@NonNull Exception ex) {
                    Log.e(TAG, "Failed searching custom deck.", ex);

                    if (ex instanceof OverloadedServerException && (((OverloadedServerException) ex).reason.equals(OverloadedServerException.REASON_NO_SUCH_DECK)
                            || ((OverloadedServerException) ex).reason.equals(OverloadedServerException.REASON_NO_SUCH_USER))) {
                        Toaster.with(ViewCustomDeckActivity.this).message(R.string.cannotFindCustomDeck).show();
                    } else {
                        Toaster.with(ViewCustomDeckActivity.this).message(R.string.failedLoading).show();
                    }

                    onBackPressed();
                }
            });
        } else {
            shareCode = getIntent().getStringExtra("shareCode");
            owner = getIntent().getStringExtra("owner");
            String deckName = getIntent().getStringExtra("deckName");
            if (owner == null || deckName == null || shareCode == null) {
                onBackPressed();
                return;
            }

            OverloadedSyncApi.get().getPublicCustomDeck(owner, deckName, this, new GeneralCallback<CustomDeckWithCards>() {
                @Override
                public void onResult(@NonNull CustomDeckWithCards result) {
                    deckLoaded(result);
                    db.updateStarredDeck(result.shareCode, result.name, result.watermark, result.count);
                }

                @Override
                public void onFailed(@NonNull Exception ex) {
                    Log.e(TAG, "Failed loading custom deck cards.", ex);

                    if (ex instanceof OverloadedServerException && (((OverloadedServerException) ex).reason.equals(OverloadedServerException.REASON_NO_SUCH_DECK)
                            || ((OverloadedServerException) ex).reason.equals(OverloadedServerException.REASON_NO_SUCH_USER))) {
                        db.removeStarredDeck(owner, shareCode);
                        Toaster.with(ViewCustomDeckActivity.this).message(R.string.deckDoesNotExist).show();
                    } else {
                        Toaster.with(ViewCustomDeckActivity.this).message(R.string.failedLoading).show();
                    }

                    onBackPressed();
                }
            });
        }
    }

    @NonNull
    @Override
    public String getWatermark() {
        return generalInfoFragment != null ? generalInfoFragment.getWatermark() : "";
    }

    @Override
    public void refreshTabs() {
        if (pager != null && pager.getAdapter() != null)
            pager.getAdapter().notifyDataSetChanged();
    }

    private void deckLoaded(@NonNull CrCastDeck result) {
        customDeck = null;
        shareCode = null;
        owner = null;

        CrCastDeck.Cards cards = result.cards();
        if (cards == null)
            return;

        pager.setAdapter(new PagerAdapter(getSupportFragmentManager(),
                generalInfoFragment = GeneralInfoFragment.get(this, result),
                BlackCardsFragment.getWithBaseCards(this, false, cards.blacks),
                WhiteCardsFragment.getWithBaseCards(this, false, cards.whites)));
        tabs.setupWithViewPager(pager);

        loading.setVisibility(View.GONE);
        supportInvalidateOptionsMenu();
    }

    private void deckLoaded(@NonNull CustomDeckWithCards result) {
        customDeck = result;
        shareCode = result.shareCode;
        owner = result.owner;

        BlackCardsFragment blackCardsFragment = BlackCardsFragment.getWithOverloadedCards(this, result.collaborator, result.blackCards());
        WhiteCardsFragment whiteCardsFragment = WhiteCardsFragment.getWithOverloadedCards(this, result.collaborator, result.whiteCards());

        if (result.collaborator) {
            CollaboratorHandler handler = new CollaboratorHandler(result.shareCode, result.watermark);
            blackCardsFragment.setHandler(handler);
            whiteCardsFragment.setHandler(handler);
        }

        pager.setAdapter(new PagerAdapter(getSupportFragmentManager(),
                generalInfoFragment = GeneralInfoFragment.get(this, result),
                blackCardsFragment, whiteCardsFragment));
        tabs.setupWithViewPager(pager);

        loading.setVisibility(View.GONE);
        supportInvalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.view_custom_deck, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (customDeck == null || shareCode == null || owner == null || owner.equals(OverloadedApi.get().username())) {
            menu.removeItem(R.id.viewCustomDeck_addStar);
            menu.removeItem(R.id.viewCustomDeck_removeStar);
        } else if (db.isStarred(shareCode)) {
            menu.removeItem(R.id.viewCustomDeck_addStar);
        } else {
            menu.removeItem(R.id.viewCustomDeck_removeStar);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.viewCustomDeck_addStar:
                if (customDeck == null || owner == null)
                    return false;

                db.addStarredDeck(customDeck.shareCode, customDeck.name, customDeck.watermark, owner, customDeck.count);
                supportInvalidateOptionsMenu();
                return true;
            case R.id.viewCustomDeck_removeStar:
                if (owner == null || shareCode == null)
                    return false;

                db.removeStarredDeck(owner, shareCode);
                supportInvalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
