package com.gianlu.pretendyourexyzzy;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.Drawer.BaseDrawerItem;
import com.gianlu.commonutils.Drawer.DrawerManager;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Dialogs.EditGameOptionsDialog;
import com.gianlu.pretendyourexyzzy.Dialogs.UserInfoDialog;
import com.gianlu.pretendyourexyzzy.Main.CardcastFragment;
import com.gianlu.pretendyourexyzzy.Main.ChatFragment;
import com.gianlu.pretendyourexyzzy.Main.DrawerItem;
import com.gianlu.pretendyourexyzzy.Main.GamesFragment;
import com.gianlu.pretendyourexyzzy.Main.NamesFragment;
import com.gianlu.pretendyourexyzzy.Main.OnLeftGame;
import com.gianlu.pretendyourexyzzy.Main.OngoingGameFragment;
import com.gianlu.pretendyourexyzzy.Main.OngoingGameHelper;
import com.gianlu.pretendyourexyzzy.Metrics.MetricsActivity;
import com.gianlu.pretendyourexyzzy.NetIO.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GamePermalink;
import com.gianlu.pretendyourexyzzy.NetIO.Models.User;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.NetIO.PyxRequests;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.Starred.StarredCardsActivity;
import com.gianlu.pretendyourexyzzy.Starred.StarredDecksActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;

import java.util.Objects;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends ActivityWithDialog implements GamesFragment.OnParticipateGame, OnLeftGame, EditGameOptionsDialog.ApplyOptions, OngoingGameHelper.Listener, UserInfoDialog.OnViewGame, DrawerManager.MenuDrawerListener<DrawerItem>, DrawerManager.OnAction {
    private BottomNavigationManager navigation;
    private NamesFragment namesFragment;
    private GamesFragment gamesFragment;
    private CardcastFragment cardcastFragment;
    private ChatFragment gameChatFragment;
    private OngoingGameFragment ongoingGameFragment;
    private ChatFragment globalChatFragment;
    private RegisteredPyx pyx;
    private DrawerManager<User, DrawerItem> drawerManager;
    private volatile GamePermalink currentGame;
    private Item currentFragment = null;

    @Override
    protected void onResume() {
        super.onResume();
        if (drawerManager != null) drawerManager.syncTogglerState();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (drawerManager != null) drawerManager.syncTogglerState();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        OngoingGameHelper.setup(this);

        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            Toaster.with(this).message(R.string.failedLoading).ex(ex).show();
            startActivity(new Intent(this, LoadingActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            return;
        }

        DrawerManager.Config<User, DrawerItem> drawerConfig = new DrawerManager.Config<User, DrawerItem>(this)
                .addMenuItem(new BaseDrawerItem<>(DrawerItem.HOME, R.drawable.baseline_home_24, getString(R.string.home)));

        if (pyx.hasMetrics())
            drawerConfig.addMenuItem(new BaseDrawerItem<>(DrawerItem.USER_METRICS, R.drawable.baseline_person_24, getString(R.string.metrics)));

        drawerConfig.addMenuItem(new BaseDrawerItem<>(DrawerItem.STARRED_CARDS, R.drawable.baseline_star_24, getString(R.string.starredCards)))
                .addMenuItem(new BaseDrawerItem<>(DrawerItem.STARRED_CARDCAST_DECKS, R.drawable.baseline_bookmarks_24, getString(R.string.starredCardcastDecks)))
                .addMenuItemSeparator()
                .addMenuItem(new BaseDrawerItem<>(DrawerItem.PREFERENCES, R.drawable.baseline_settings_24, getString(R.string.preferences)))
                .addMenuItem(new BaseDrawerItem<>(DrawerItem.REPORT, R.drawable.baseline_report_problem_24, getString(R.string.report)))
                .singleProfile(pyx.user(), this);

        drawerManager = drawerConfig
                .build(this, findViewById(R.id.main_drawer), toolbar);

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
                case CARDCAST:
                    setTitle(getString(R.string.cardcast) + " - " + getString(R.string.app_name));
                    break;
                case ONGOING_GAME:
                    setTitle(getString(R.string.gameLabel) + " - " + getString(R.string.app_name));
                    break;
                case GAME_CHAT:
                    setTitle(getString(R.string.gameChat) + " - " + getString(R.string.app_name));
                    break;
                case GLOBAL_CHAT:
                    setTitle(getString(R.string.globalChat) + " - " + getString(R.string.app_name));
                    break;
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
                case GAME_CHAT:
                    if (gameChatFragment != null) gameChatFragment.scrollToTop();
                    break;
                case GLOBAL_CHAT:
                    if (globalChatFragment != null) globalChatFragment.scrollToTop();
                    break;
            }
        });

        setKeepScreenOn(Prefs.getBoolean(PK.KEEP_SCREEN_ON));

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        namesFragment = (NamesFragment) getOrAdd(transaction, Item.PLAYERS, NamesFragment::getInstance);
        gamesFragment = (GamesFragment) getOrAdd(transaction, Item.GAMES, GamesFragment::getInstance);
        cardcastFragment = (CardcastFragment) getOrAdd(transaction, Item.CARDCAST, CardcastFragment::getInstance);

        if (pyx.config().globalChatEnabled())
            globalChatFragment = (ChatFragment) getOrAdd(transaction, Item.GLOBAL_CHAT, ChatFragment::getGlobalInstance);

        transaction.commitNow();

        Item currentFragment = null;
        if (savedInstanceState != null) {
            currentGame = (GamePermalink) savedInstanceState.getSerializable("currentGame");
            currentFragment = (Item) savedInstanceState.getSerializable("currentFragment");
        }

        ongoingGameFragment = (OngoingGameFragment) getSupportFragmentManager().findFragmentByTag(Item.ONGOING_GAME.tag);
        gameChatFragment = (ChatFragment) getSupportFragmentManager().findFragmentByTag(Item.GAME_CHAT.tag);

        if (currentGame == null) {
            inflateNavigation(Layout.LOBBY);
            if (currentFragment == null) navigation.setSelectedItem(Item.GAMES);

            if (gameChatFragment != null || ongoingGameFragment != null) {
                transaction = getSupportFragmentManager().beginTransaction();
                if (gameChatFragment != null) transaction.remove(gameChatFragment);
                if (ongoingGameFragment != null) transaction.remove(ongoingGameFragment);
                transaction.commitNow();
            }
        } else {
            if (ongoingGameFragment == null) {
                transaction = getSupportFragmentManager().beginTransaction();
                ongoingGameFragment = OngoingGameFragment.getInstance(currentGame);
                addOrReplace(transaction, ongoingGameFragment, Item.ONGOING_GAME);

                if (pyx.config().gameChatEnabled()) {
                    gameChatFragment = ChatFragment.getGameInstance(currentGame.gid);
                    addOrReplace(transaction, gameChatFragment, Item.GAME_CHAT);
                }

                transaction.commitNow();
            }

            inflateNavigation(Layout.ONGOING);
            if (currentFragment == null) navigation.setSelectedItem(Item.ONGOING_GAME);
        }

        if (currentFragment != null) navigation.setSelectedItem(currentFragment);

        GamePermalink perm = (GamePermalink) getIntent().getSerializableExtra("game");
        if (perm != null) {
            gamesFragment.launchGame(perm, getIntent().getStringExtra("password"), getIntent().getBooleanExtra("shouldRequest", true));
            getIntent().removeExtra("gid");
        }
    }

    @NonNull
    private Fragment getOrAdd(@NonNull FragmentTransaction transaction, @NonNull Item item, @NonNull CreateFragment provider) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(item.tag);
        if (fragment != null) return fragment;

        fragment = provider.create();
        transaction.add(R.id.main_container, fragment, item.tag);
        return fragment;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("currentGame", currentGame);
        outState.putSerializable("currentFragment", currentFragment);
    }

    @Override
    public void onBackPressed() {
        if (ongoingGameFragment != null) ongoingGameFragment.goBack();
        else super.onBackPressed();
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
                drawerAction();
                return true;
            case R.id.main_keepScreenOn:
                item.setChecked(!item.isChecked());
                Prefs.putBoolean(PK.KEEP_SCREEN_ON, item.isChecked());
                setKeepScreenOn(item.isChecked());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean canModifyCardcastDecks() {
        return ongoingGameFragment != null && ongoingGameFragment.canModifyCardcastDecks();
    }

    @Override
    public void addCardcastDeck(@NonNull String code) {
        if (!canModifyCardcastDecks()) return;
        ongoingGameFragment.addCardcastDeck(code);
    }

    @Override
    public void addCardcastStarredDecks() {
        if (!canModifyCardcastDecks()) return;
        ongoingGameFragment.addCardcastStarredDecks();
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
                case CARDCAST:
                    transaction.add(R.id.main_container, cardcastFragment, item.tag);
                    break;
                case ONGOING_GAME:
                    if (ongoingGameFragment != null)
                        transaction.add(R.id.main_container, ongoingGameFragment, item.tag);
                    break;
                case GAME_CHAT:
                    if (gameChatFragment != null)
                        transaction.add(R.id.main_container, gameChatFragment, item.tag);
                    break;
                case GLOBAL_CHAT:
                    if (globalChatFragment != null)
                        transaction.add(R.id.main_container, globalChatFragment, item.tag);
                    break;
            }
        }

        try {
            transaction.commit();
        } catch (IllegalStateException ex) {
            AnalyticsApplication.crashlyticsLog(ex.getMessage() + " at #switchTo(Item)");
        }
    }

    @Override
    public void onParticipatingGame(@NonNull GamePermalink game) {
        currentGame = game;

        if (isFinishing() || isDestroyed()) return;
        inflateNavigation(Layout.ONGOING);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        ongoingGameFragment = OngoingGameFragment.getInstance(game);
        addOrReplace(transaction, ongoingGameFragment, Item.ONGOING_GAME);

        if (pyx.config().gameChatEnabled()) {
            gameChatFragment = ChatFragment.getGameInstance(game.gid);
            addOrReplace(transaction, gameChatFragment, Item.GAME_CHAT);
        }

        try {
            transaction.commitNow();
            navigation.setSelectedItem(Item.ONGOING_GAME);
        } catch (IllegalStateException ex) {
            AnalyticsApplication.crashlyticsLog(ex.getMessage() + " at #onParticipatingGame(GamePermalink)");
        }
    }

    @Override
    public void onLeftGame() {
        currentGame = null;
        ongoingGameFragment = null;
        gameChatFragment = null;
        AnalyticsApplication.sendAnalytics(Utils.ACTION_LEFT_GAME);

        inflateNavigation(Layout.LOBBY);
        navigation.setSelectedItem(Item.GAMES);

        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        Fragment ongoingGame = manager.findFragmentByTag(Item.ONGOING_GAME.tag);
        if (ongoingGame != null) transaction.remove(ongoingGame);

        Fragment gameChat = manager.findFragmentByTag(Item.GAME_CHAT.tag);
        if (gameChat != null) transaction.remove(gameChat);

        try {
            transaction.commit();
        } catch (IllegalStateException ex) {
            AnalyticsApplication.crashlyticsLog(ex.getMessage() + " at #onLeftGame()");
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
            showDialog(DialogUtils.progressDialog(this, R.string.loading));
            pyx.request(PyxRequests.changeGameOptions(gid, options), new Pyx.OnSuccess() {
                @Override
                public void onDone() {
                    dismissDialog();
                    Toaster.with(MainActivity.this).message(R.string.optionsChanged).show();
                }

                @Override
                public void onException(@NonNull Exception ex) {
                    dismissDialog();
                    Toaster.with(MainActivity.this).message(R.string.failedChangingOptions).ex(ex).show();
                }
            });
        } catch (JSONException ex) {
            dismissDialog();
            Toaster.with(this).message(R.string.failedChangingOptions).ex(ex).show();
        }
    }

    @Override
    public void drawerAction() {
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
            case STARRED_CARDCAST_DECKS:
                StarredDecksActivity.startActivity(this);
                return true;
            case USER_METRICS:
                MetricsActivity.startActivity(this);
                return true;
            case PREFERENCES:
                startActivity(new Intent(this, PreferenceActivity.class));
                return true;
            case REPORT:
                Logging.sendEmail(this, null);
                return true;
        }

        return false;
    }

    private void inflateNavigation(@NonNull Layout layout) {
        navigation.clear();

        for (int i = 0; i < layout.items.length; i++) {
            Item item = layout.items[i];
            if (item == Item.GAME_CHAT && !pyx.config().gameChatEnabled())
                continue;
            else if (item == Item.GLOBAL_CHAT && !pyx.config().globalChatEnabled())
                continue;

            navigation.add(item);
        }
    }

    private enum Item {
        GLOBAL_CHAT(R.string.globalChat, R.drawable.baseline_chat_24), GAME_CHAT(R.string.gameChat, R.drawable.baseline_chat_bubble_outline_24),
        CARDCAST(R.string.cardcast, R.drawable.baseline_cast_24), GAMES(R.string.games, R.drawable.baseline_games_24),
        PLAYERS(R.string.playersLabel, R.drawable.baseline_people_24), ONGOING_GAME(R.string.ongoingGame, R.drawable.baseline_casino_24);

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
        LOBBY(Item.PLAYERS, Item.GLOBAL_CHAT, Item.GAMES, Item.CARDCAST),
        ONGOING(Item.PLAYERS, Item.GLOBAL_CHAT, Item.CARDCAST, Item.ONGOING_GAME, Item.GAME_CHAT);

        private final Item[] items;

        Layout(Item... items) {
            this.items = items;
        }
    }

    private interface CreateFragment {
        @NonNull
        Fragment create();
    }

    @UiThread
    private static class BottomNavigationManager {
        private final BottomNavigationView view;

        BottomNavigationManager(@NonNull BottomNavigationView view) {
            this.view = view;
        }

        void add(@NonNull Item item) {
            view.getMenu().add(Menu.NONE, item.id, Menu.NONE, item.text).setIcon(item.icon);
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
