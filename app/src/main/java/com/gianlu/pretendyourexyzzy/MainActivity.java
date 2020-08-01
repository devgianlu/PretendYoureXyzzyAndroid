package com.gianlu.pretendyourexyzzy;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.drawer.BaseDrawerItem;
import com.gianlu.commonutils.drawer.DrawerManager;
import com.gianlu.commonutils.logs.LogsHelper;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxChatHelper;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.Game;
import com.gianlu.pretendyourexyzzy.api.models.GamePermalink;
import com.gianlu.pretendyourexyzzy.api.models.User;
import com.gianlu.pretendyourexyzzy.dialogs.EditGameOptionsDialog;
import com.gianlu.pretendyourexyzzy.dialogs.UserInfoDialog;
import com.gianlu.pretendyourexyzzy.main.CustomDecksFragment;
import com.gianlu.pretendyourexyzzy.main.DrawerItem;
import com.gianlu.pretendyourexyzzy.main.GamesFragment;
import com.gianlu.pretendyourexyzzy.main.NamesFragment;
import com.gianlu.pretendyourexyzzy.main.OnLeftGame;
import com.gianlu.pretendyourexyzzy.main.OngoingGameFragment;
import com.gianlu.pretendyourexyzzy.main.OverloadedFragment;
import com.gianlu.pretendyourexyzzy.main.PyxChatsFragment;
import com.gianlu.pretendyourexyzzy.metrics.MetricsActivity;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedSignInHelper;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;
import com.gianlu.pretendyourexyzzy.overloaded.SyncUtils;
import com.gianlu.pretendyourexyzzy.starred.StarredCardsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.PlayGamesAuthProvider;

import org.json.JSONException;

import java.util.Objects;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.OverloadedChatApi;

public class MainActivity extends ActivityWithDialog implements GamesFragment.OnParticipateGame, OnLeftGame, EditGameOptionsDialog.ApplyOptions, UserInfoDialog.OnViewGame, DrawerManager.MenuDrawerListener<DrawerItem>, DrawerManager.OnAction, OverloadedChatApi.UnreadCountListener, PyxChatHelper.UnreadCountListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private final Object fragmentsLock = new Object();
    private BottomNavigationManager navigation;
    private NamesFragment namesFragment;
    private GamesFragment gamesFragment;
    private CustomDecksFragment customDecksFragment;
    private OngoingGameFragment ongoingGameFragment;
    private PyxChatsFragment chatsFragment;
    private OverloadedFragment overloadedFragment;
    private RegisteredPyx pyx;
    private DrawerManager<User, DrawerItem> drawerManager;
    private volatile GamePermalink currentGame;
    private Item currentFragment = null;

    @Override
    protected void onResume() {
        super.onResume();
        if (drawerManager != null) drawerManager.syncTogglerState();

        if (overloadedFragment != null && !OverloadedUtils.isSignedIn()) {
            navigation.remove(Item.OVERLOADED);
            synchronized (fragmentsLock) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.remove(overloadedFragment);
                transaction.commitAllowingStateLoss();
                overloadedFragment = null;
            }
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (drawerManager != null) drawerManager.syncTogglerState();
    }

    @NonNull
    private <F extends Fragment> F getOrAdd(@NonNull FragmentTransaction transaction, @NonNull Item item, @NonNull CreateFragment<F> provider) {
        // noinspection unchecked
        F fragment = (F) getSupportFragmentManager().findFragmentByTag(item.tag);
        if (fragment != null) return fragment;

        fragment = provider.create();
        transaction.add(R.id.main_container, fragment, item.tag);
        return fragment;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("currentGame", currentGame);
        outState.putSerializable("currentFragment", currentFragment);
    }

    @Override
    public void onBackPressed() {
        if (ongoingGameFragment != null) {
            ongoingGameFragment.goBack();
            return;
        }

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.logout).setMessage(Html.fromHtml(getString(R.string.logout_confirmation)))
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, (d, which) -> logout());

        showDialog(dialog);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.main_keepScreenOn).setChecked(Prefs.getBoolean(PK.KEEP_SCREEN_ON));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_logout:
                logout();
                return true;
            case R.id.main_keepScreenOn:
                item.setChecked(!item.isChecked());
                Prefs.putBoolean(PK.KEEP_SCREEN_ON, item.isChecked());
                setKeepScreenOn(item.isChecked());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setKeepScreenOn(boolean on) {
        if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void addOrReplace(@NonNull FragmentTransaction transaction, @NonNull Fragment newFragment, @NonNull Item item) {
        FragmentManager manager = getSupportFragmentManager();
        Fragment fragment = manager.findFragmentByTag(item.tag);
        if (fragment != null) transaction.remove(fragment);

        transaction.add(R.id.main_container, newFragment, item.tag);
    }

    private void switchTo(@NonNull Item item) {
        currentFragment = item;

        if (isFinishing() || isDestroyed()) return;

        synchronized (fragmentsLock) {
            FragmentManager manager = getSupportFragmentManager();
            Fragment fragment = manager.findFragmentByTag(item.tag);
            FragmentTransaction transaction = manager.beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out);

            if (fragment != null) {
                for (Fragment hideFragment : manager.getFragments())
                    if (!Objects.equals(hideFragment.getTag(), fragment.getTag()))
                        transaction.hide(hideFragment);

                transaction.show(fragment);
            } else {
                for (Fragment hideFragment : manager.getFragments())
                    transaction.hide(hideFragment);

                switch (item) {
                    case PLAYERS:
                        transaction.add(R.id.main_container, namesFragment, item.tag);
                        break;
                    case GAMES:
                        transaction.add(R.id.main_container, gamesFragment, item.tag);
                        break;
                    case CUSTOM_DECKS:
                        transaction.add(R.id.main_container, customDecksFragment, item.tag);
                        break;
                    case ONGOING_GAME:
                        if (ongoingGameFragment != null)
                            transaction.add(R.id.main_container, ongoingGameFragment, item.tag);
                        break;
                    case PYX_CHAT:
                        if (chatsFragment != null)
                            transaction.add(R.id.main_container, chatsFragment, item.tag);
                        break;
                    case OVERLOADED:
                        if (overloadedFragment != null)
                            transaction.add(R.id.main_container, overloadedFragment, item.tag);
                        break;
                }
            }

            try {
                transaction.commit();

                if (fragment instanceof CustomDecksFragment)
                    ((CustomDecksFragment) fragment).tryShowingTutorial();
            } catch (IllegalStateException ex) {
                AnalyticsApplication.crashlyticsLog(ex.getMessage() + " at #switchTo(Item)");
            }
        }
    }

    @Override
    public void onParticipatingGame(@NonNull GamePermalink game) {
        currentGame = game;

        if (isFinishing() || isDestroyed()) return;
        inflateNavigation(Layout.ONGOING);

        synchronized (fragmentsLock) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            ongoingGameFragment = OngoingGameFragment.getInstance(game);
            addOrReplace(transaction, ongoingGameFragment, Item.ONGOING_GAME);

            toggleGameChat(transaction, game.gid);

            try {
                transaction.commitNow();
                navigation.setSelectedItem(Item.ONGOING_GAME);
            } catch (IllegalStateException ex) {
                AnalyticsApplication.crashlyticsLog(ex.getMessage() + " at #onParticipatingGame(GamePermalink)");
            }
        }
    }

    private void toggleGameChat(@NonNull FragmentTransaction transaction, @Nullable Integer gid) {
        if (chatsFragment == null && pyx.config().globalChatEnabled())
            chatsFragment = getOrAdd(transaction, Item.PYX_CHAT, () -> PyxChatsFragment.get(pyx));

        if (gid != null && pyx.config().gameChatEnabled()) {
            if (chatsFragment == null)
                chatsFragment = getOrAdd(transaction, Item.PYX_CHAT, () -> PyxChatsFragment.get(pyx));

            chatsFragment.toggleGameChat(gid);
        } else {
            if (chatsFragment != null) {
                if (pyx.config().globalChatEnabled()) chatsFragment.toggleGameChat(null);
                else transaction.remove(chatsFragment);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            Toaster.with(this).message(R.string.failedLoading).show();
            startActivity(new Intent(this, LoadingActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            return;
        }

        DrawerManager.Config<User, DrawerItem> drawerConfig = new DrawerManager.Config<User, DrawerItem>(this)
                .addMenuItem(new BaseDrawerItem<>(DrawerItem.HOME, R.drawable.baseline_home_24, getString(R.string.home)));

        if (pyx.hasMetrics())
            drawerConfig.addMenuItem(new BaseDrawerItem<>(DrawerItem.USER_METRICS, R.drawable.baseline_person_24, getString(R.string.metrics)));

        drawerConfig.addMenuItem(new BaseDrawerItem<>(DrawerItem.STARRED_CARDS, R.drawable.baseline_star_24, getString(R.string.starredCards)))
                .addMenuItemSeparator()
                .addMenuItem(new BaseDrawerItem<>(DrawerItem.PREFERENCES, R.drawable.baseline_settings_24, getString(R.string.preferences)))
                .addMenuItem(new BaseDrawerItem<>(DrawerItem.REPORT, R.drawable.baseline_report_problem_24, getString(R.string.report)))
                .singleProfile(pyx.user(), this);

        drawerManager = drawerConfig.build(this, findViewById(R.id.main_drawer), toolbar);
        drawerManager.setActiveItem(DrawerItem.HOME);

        navigation = new BottomNavigationManager(findViewById(R.id.main_navigation));
        navigation.setOnNavigationItemSelectedListener(item -> {
            switch (item) {
                case PLAYERS:
                    setTitle(getString(R.string.playersLabel) + " - " + getString(R.string.app_name));
                    break;
                case GAMES:
                    setTitle(getString(R.string.games) + " - " + getString(R.string.app_name));
                    break;
                case CUSTOM_DECKS:
                    setTitle(getString(R.string.customDecks) + " - " + getString(R.string.app_name));
                    break;
                case ONGOING_GAME:
                    setTitle(getString(R.string.gameLabel) + " - " + getString(R.string.app_name));
                    break;
                case PYX_CHAT:
                    setTitle(getString(R.string.chat) + " - " + getString(R.string.app_name));
                    break;
                case OVERLOADED:
                    setTitle(getString(R.string.overloaded) + " - " + getString(R.string.app_name));
                    break;
            }

            if (chatsFragment != null) {
                if (item == Item.PYX_CHAT) chatsFragment.onSelectedFragment();
                else chatsFragment.onDeselectedFragment();
            }

            switchTo(item);
            return true;
        });
        navigation.setOnNavigationItemReselectedListener(item -> {
            switch (item) {
                case PLAYERS:
                    if (namesFragment != null) namesFragment.scrollToTop();
                    break;
                case GAMES:
                    if (gamesFragment != null) gamesFragment.scrollToTop();
                    break;
                case PYX_CHAT:
                    if (chatsFragment != null) chatsFragment.scrollToTop();
                    break;
            }
        });

        setKeepScreenOn(Prefs.getBoolean(PK.KEEP_SCREEN_ON));

        GPGamesHelper.setPopupView(this, (View) navigation.view.getParent(), Gravity.TOP | Gravity.CENTER_HORIZONTAL);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        namesFragment = getOrAdd(transaction, Item.PLAYERS, NamesFragment::getInstance);
        gamesFragment = getOrAdd(transaction, Item.GAMES, GamesFragment::getInstance);
        customDecksFragment = getOrAdd(transaction, Item.CUSTOM_DECKS, CustomDecksFragment::getInstance);

        if (OverloadedUtils.isSignedIn())
            overloadedFragment = getOrAdd(transaction, Item.OVERLOADED, OverloadedFragment::getInstance);

        Item currentFragment = null;
        if (savedInstanceState != null) {
            currentGame = (GamePermalink) savedInstanceState.getSerializable("currentGame");
            currentFragment = (Item) savedInstanceState.getSerializable("currentFragment");
        }

        toggleGameChat(transaction, currentGame == null ? null : currentGame.gid);
        transaction.commitNow();

        ongoingGameFragment = (OngoingGameFragment) getSupportFragmentManager().findFragmentByTag(Item.ONGOING_GAME.tag);
        if (currentGame == null) {
            inflateNavigation(Layout.LOBBY);
            if (currentFragment == null) navigation.setSelectedItem(Item.GAMES);

            if (ongoingGameFragment != null) {
                transaction = getSupportFragmentManager().beginTransaction();
                if (ongoingGameFragment != null) transaction.remove(ongoingGameFragment);
                transaction.commitNow();
            }
        } else {
            if (ongoingGameFragment == null) {
                transaction = getSupportFragmentManager().beginTransaction();
                ongoingGameFragment = OngoingGameFragment.getInstance(currentGame);
                addOrReplace(transaction, ongoingGameFragment, Item.ONGOING_GAME);
                transaction.commitNow();
            }

            inflateNavigation(Layout.ONGOING);
            if (currentFragment == null) navigation.setSelectedItem(Item.ONGOING_GAME);
        }

        if (currentFragment != null) navigation.setSelectedItem(currentFragment);

        pyx.chat().addUnreadCountListener(this);

        GamePermalink perm = (GamePermalink) getIntent().getSerializableExtra("game");
        if (perm != null) {
            gamesFragment.launchGame(perm, getIntent().getStringExtra("password"), getIntent().getBooleanExtra("shouldRequest", true));
            getIntent().removeExtra("gid");
        }

        if (OverloadedUtils.isSignedIn()) {
            SyncUtils.syncStarredCards(this, null);
            SyncUtils.syncCustomDecks(this, null);
            SyncUtils.syncStarredCustomDecks(this, null);

            OverloadedSignInHelper.signInSilently(this, PlayGamesAuthProvider.PROVIDER_ID).addOnSuccessListener(account -> {
                String authCode = account.getServerAuthCode();
                if (authCode != null) OverloadedApi.get().linkGames(authCode);
            });
        }
    }

    @Override
    public void onLeftGame() {
        currentGame = null;
        ongoingGameFragment = null;
        AnalyticsApplication.sendAnalytics(Utils.ACTION_LEFT_GAME);

        inflateNavigation(Layout.LOBBY);
        navigation.setSelectedItem(Item.GAMES);

        synchronized (fragmentsLock) {
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();

            toggleGameChat(transaction, null);

            try {
                Fragment ongoingGame = manager.findFragmentByTag(Item.ONGOING_GAME.tag);
                if (ongoingGame != null) transaction.remove(ongoingGame);
                transaction.commit();
            } catch (IllegalStateException ex) {
                Log.d(TAG, "Failed fragments transaction on left game.", ex);
            }
        }
    }

    @Override
    public void viewGame(int gid, boolean locked) {
        if (gamesFragment != null) {
            navigation.setSelectedItem(Item.GAMES);
            gamesFragment.viewGame(gid, locked);
        }
    }

    @Override
    public boolean canViewGame() {
        return ongoingGameFragment == null && gamesFragment != null && gamesFragment.isAdded();
    }

    @Override
    public void changeGameOptions(int gid, @NonNull Game.Options options) {
        try {
            showProgress(R.string.loading);
            pyx.request(PyxRequests.changeGameOptions(gid, options), this, new Pyx.OnSuccess() {
                @Override
                public void onDone() {
                    dismissDialog();
                    Toaster.with(MainActivity.this).message(R.string.optionsChanged).show();
                }

                @Override
                public void onException(@NonNull Exception ex) {
                    dismissDialog();
                    Log.e(TAG, "Failed changing game options.", ex);
                    Toaster.with(MainActivity.this).message(R.string.failedChangingOptions).show();
                }
            });
        } catch (JSONException ex) {
            dismissDialog();
            Log.e(TAG, "Failed parsing game options.", ex);
            Toaster.with(this).message(R.string.failedChangingOptions).show();
        }
    }

    @Override
    public void drawerAction() {
        logout();
    }

    private void logout() {
        pyx.logout();
        startActivity(new Intent(this, LoadingActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    @Override
    public boolean onDrawerMenuItemSelected(@NonNull BaseDrawerItem<DrawerItem> item) {
        switch (item.id) {
            case HOME:
                return true;
            case STARRED_CARDS:
                StarredCardsActivity.startActivity(this);
                return true;
            case USER_METRICS:
                MetricsActivity.startActivity(this);
                return true;
            case PREFERENCES:
                startActivity(new Intent(this, PreferenceActivity.class));
                return true;
            case REPORT:
                LogsHelper.sendEmail(this, null);
                return true;
        }

        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        OverloadedApi.chat(this).addUnreadCountListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        OverloadedApi.chat(this).removeUnreadCountListener(this);
        if (pyx != null) pyx.chat().removeUnreadCountListener(this);
    }

    private void inflateNavigation(@NonNull Layout layout) {
        if (navigation == null) return;
        navigation.clear();

        for (int i = 0; i < layout.items.length; i++) {
            Item item = layout.items[i];
            if (item == Item.PYX_CHAT && !(pyx.config().gameChatEnabled() || pyx.config().globalChatEnabled()))
                continue;
            else if (item == Item.OVERLOADED && !OverloadedUtils.isSignedIn())
                continue;

            navigation.add(item);
        }

        mayUpdateUnreadCount();
    }

    @Override
    public void mayUpdateUnreadCount() {
        if (!OverloadedUtils.isSignedIn()) return;

        int count = OverloadedApi.chat(this).countTotalUnread();
        if (count == 0) navigation.removeBadge(Item.OVERLOADED);
        else navigation.setBadge(Item.OVERLOADED, count);
    }

    @Override
    public void onPyxUnread(int count) {
        if (count == 0) navigation.removeBadge(Item.PYX_CHAT);
        else navigation.setBadge(Item.PYX_CHAT, count);
    }

    private enum Item {
        CUSTOM_DECKS(R.string.customDecks, R.drawable.baseline_bookmarks_24), GAMES(R.string.games, R.drawable.baseline_games_24),
        PLAYERS(R.string.playersLabel, R.drawable.baseline_people_24), ONGOING_GAME(R.string.ongoingGame, R.drawable.baseline_casino_24),
        OVERLOADED(R.string.overloaded, R.drawable.baseline_videogame_asset_24), PYX_CHAT(R.string.chat, R.drawable.baseline_chat_bubble_outline_24);

        private final int text;
        private final int icon;
        private final int id;
        private final String tag;

        Item(@StringRes int text, @DrawableRes int icon) {
            this.text = text;
            this.icon = icon;
            this.id = ordinal();
            this.tag = name();
        }

        @NonNull
        static Item lookup(int id) {
            for (Item item : values())
                if (item.id == id)
                    return item;

            throw new IllegalArgumentException();
        }
    }

    private enum Layout {
        LOBBY(Item.PLAYERS, Item.PYX_CHAT, Item.GAMES, Item.CUSTOM_DECKS, Item.OVERLOADED),
        ONGOING(Item.PLAYERS, Item.PYX_CHAT, Item.CUSTOM_DECKS, Item.OVERLOADED, Item.ONGOING_GAME);

        static {
            for (Layout l : values())
                if (l.items.length > 5)
                    throw new IllegalStateException();
        }

        private final Item[] items;

        Layout(Item... items) {
            this.items = items;
        }
    }

    private interface CreateFragment<F extends Fragment> {
        @NonNull
        F create();
    }

    @UiThread
    private static class BottomNavigationManager {
        private final BottomNavigationView view;

        BottomNavigationManager(@NonNull BottomNavigationView view) {
            this.view = view;
        }

        void remove(@NonNull Item item) {
            view.getMenu().removeItem(item.id);
        }

        void add(@NonNull Item item) {
            view.getMenu().add(Menu.NONE, item.id, Menu.NONE, item.text).setIcon(item.icon);
        }

        void setBadge(@NonNull Item item, int number) {
            view.getOrCreateBadge(item.id).setNumber(number);
        }

        void removeBadge(@NonNull Item item) {
            view.removeBadge(item.id);
        }

        void clear() {
            view.getMenu().clear();
        }

        void setOnNavigationItemSelectedListener(@NonNull OnNavigationItemSelectedListener listener) {
            view.setOnNavigationItemSelectedListener(menuItem -> listener.onNavigationItemSelected(Item.lookup(menuItem.getItemId())));
        }

        void setOnNavigationItemReselectedListener(@NonNull OnNavigationItemReselectedListener listener) {
            view.setOnNavigationItemReselectedListener(menuItem -> listener.onNavigationItemReselected(Item.lookup(menuItem.getItemId())));
        }

        void setSelectedItem(@NonNull Item item) {
            view.setSelectedItemId(item.id);
        }

        interface OnNavigationItemSelectedListener {
            boolean onNavigationItemSelected(@NonNull Item item);
        }

        interface OnNavigationItemReselectedListener {
            void onNavigationItemReselected(@NonNull Item item);
        }
    }
}
