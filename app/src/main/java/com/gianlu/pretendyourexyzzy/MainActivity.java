package com.gianlu.pretendyourexyzzy;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Dialogs.EditGameOptionsDialog;
import com.gianlu.pretendyourexyzzy.Dialogs.UserInfoDialog;
import com.gianlu.pretendyourexyzzy.Main.CardcastFragment;
import com.gianlu.pretendyourexyzzy.Main.ChatFragment;
import com.gianlu.pretendyourexyzzy.Main.GamesFragment;
import com.gianlu.pretendyourexyzzy.Main.NamesFragment;
import com.gianlu.pretendyourexyzzy.Main.OnLeftGame;
import com.gianlu.pretendyourexyzzy.Main.OngoingGameFragment;
import com.gianlu.pretendyourexyzzy.Main.OngoingGameHelper;
import com.gianlu.pretendyourexyzzy.NetIO.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.NetIO.PyxRequests;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.SpareActivities.StarredCardsActivity;
import com.gianlu.pretendyourexyzzy.SpareActivities.StarredDecksActivity;

import org.json.JSONException;

import java.util.Objects;

public class MainActivity extends ActivityWithDialog implements GamesFragment.OnParticipateGame, OnLeftGame, EditGameOptionsDialog.ApplyOptions, OngoingGameHelper.Listener, UserInfoDialog.OnViewGame {
    private final static String TAG_GAMES = "games";
    private final static String TAG_GAME_CHAT = "gameChat";
    private static final String TAG_PLAYERS = "players";
    private static final String TAG_ONGOING_GAME = "ongoingGame";
    private static final String TAG_CARDCAST = "cardcast";
    private static final String TAG_GLOBAL_CHAT = "globalChat";
    private BottomNavigationView navigation;
    private NamesFragment namesFragment;
    private GamesFragment gamesFragment;
    private CardcastFragment cardcastFragment;
    private ChatFragment gameChatFragment;
    private OngoingGameFragment ongoingGameFragment;
    private ChatFragment globalChatFragment;
    private int currentGid = -1;
    private RegisteredPyx pyx;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (ongoingGameFragment != null && currentGid != -1) {
            Fragment.SavedState state = getSupportFragmentManager().saveFragmentInstanceState(ongoingGameFragment);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.remove(ongoingGameFragment);
            ongoingGameFragment = OngoingGameFragment.getInstance(currentGid, state);
            transaction.add(R.id.main_container, ongoingGameFragment, TAG_ONGOING_GAME);

            navigation.getMenu().clear();
            navigation.inflateMenu(R.menu.navigation_ongoing_game);

            transaction.commitNowAllowingStateLoss();
            navigation.setSelectedItemId(R.id.main_ongoingGame);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OngoingGameHelper.setup(this);

        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            Toaster.show(this, Utils.Messages.FAILED_LOADING, ex);
            startActivity(new Intent(this, LoadingActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            return;
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        namesFragment = NamesFragment.getInstance();
        transaction.add(R.id.main_container, namesFragment, TAG_PLAYERS);
        gamesFragment = GamesFragment.getInstance(this);
        transaction.add(R.id.main_container, gamesFragment, TAG_GAMES);
        cardcastFragment = CardcastFragment.getInstance();
        transaction.add(R.id.main_container, cardcastFragment, TAG_CARDCAST);

        if (pyx.isGlobalChatEnabled()) {
            globalChatFragment = ChatFragment.getGlobalInstance();
            transaction.add(R.id.main_container, globalChatFragment, TAG_GLOBAL_CHAT);
        }

        transaction.commitNow();

        navigation = findViewById(R.id.main_navigation);
        if (!pyx.isGlobalChatEnabled()) navigation.getMenu().removeItem(R.id.main_globalChat);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.main_players:
                        setTitle(getString(R.string.playersLabel) + " - " + getString(R.string.app_name));
                        switchTo(TAG_PLAYERS);
                        break;
                    case R.id.main_games:
                        setTitle(getString(R.string.games) + " - " + getString(R.string.app_name));
                        switchTo(TAG_GAMES);
                        break;
                    case R.id.main_cardcast:
                        setTitle(getString(R.string.cardcast) + " - " + getString(R.string.app_name));
                        switchTo(TAG_CARDCAST);
                        break;
                    case R.id.main_ongoingGame:
                        setTitle(getString(R.string.gameLabel) + " - " + getString(R.string.app_name));
                        switchTo(TAG_ONGOING_GAME);
                        break;
                    case R.id.main_gameChat:
                        setTitle(getString(R.string.gameChat) + " - " + getString(R.string.app_name));
                        switchTo(TAG_GAME_CHAT);
                        break;
                    case R.id.main_globalChat:
                        setTitle(getString(R.string.globalChat) + " - " + getString(R.string.app_name));
                        switchTo(TAG_GLOBAL_CHAT);
                        break;
                }

                return true;
            }
        });
        navigation.setOnNavigationItemReselectedListener(new BottomNavigationView.OnNavigationItemReselectedListener() {
            @Override
            public void onNavigationItemReselected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.main_players:
                        if (namesFragment != null) namesFragment.scrollToTop();
                        break;
                    case R.id.main_games:
                        if (gamesFragment != null) gamesFragment.scrollToTop();
                        break;
                    case R.id.main_gameChat:
                        if (gameChatFragment != null) gameChatFragment.scrollToTop();
                        break;
                    case R.id.main_globalChat:
                        if (globalChatFragment != null) globalChatFragment.scrollToTop();
                        break;
                }
            }
        });

        navigation.setSelectedItemId(R.id.main_games);
        setKeepScreenOn(Prefs.getBoolean(this, PKeys.KEEP_SCREEN_ON, true));

        int gid = getIntent().getIntExtra("gid", -1);
        if (gid != -1) {
            gamesFragment.launchGame(gid, getIntent().getStringExtra("password"), getIntent().getBooleanExtra("shouldRequest", true));
            getIntent().removeExtra("gid");
        }
    }

    @Override
    public void onBackPressed() {
        if (ongoingGameFragment != null)
            ongoingGameFragment.onBackPressed();
        else
            super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.main_keepScreenOn).setChecked(Prefs.getBoolean(this, PKeys.KEEP_SCREEN_ON, true));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_logout:
                try {
                    RegisteredPyx.get().logout();
                } catch (LevelMismatchException ex) {
                    Logging.log(ex);
                }

                startActivity(new Intent(this, LoadingActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
                return true;
            case R.id.main_keepScreenOn:
                item.setChecked(!item.isChecked());
                Prefs.putBoolean(this, PKeys.KEEP_SCREEN_ON, item.isChecked());
                setKeepScreenOn(item.isChecked());
                return true;
            case R.id.main_starredCards:
                StarredCardsActivity.startActivity(this);
                return true;
            case R.id.main_starredDecks:
                StarredDecksActivity.startActivity(this);
                return true;
            case R.id.main_preferences:
                startActivity(new Intent(this, PreferencesActivity.class));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean canModifyCardcastDecks() {
        return ongoingGameFragment != null && ongoingGameFragment.canModifyCardcastDecks();
    }

    @Override
    public void addCardcastDeck(String code) {
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

    private void switchTo(@NonNull String tag) {
        FragmentManager manager = getSupportFragmentManager();
        Fragment fragment = manager.findFragmentByTag(tag);
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

            switch (tag) {
                case TAG_PLAYERS:
                    transaction.add(R.id.main_container, namesFragment, TAG_PLAYERS);
                    break;
                case TAG_GAMES:
                    transaction.add(R.id.main_container, gamesFragment, TAG_GAMES);
                    break;
                case TAG_CARDCAST:
                    transaction.add(R.id.main_container, cardcastFragment, TAG_CARDCAST);
                    break;
                case TAG_ONGOING_GAME:
                    if (ongoingGameFragment != null)
                        transaction.add(R.id.main_container, ongoingGameFragment, TAG_ONGOING_GAME);
                    break;
                case TAG_GAME_CHAT:
                    if (gameChatFragment != null)
                        transaction.add(R.id.main_container, gameChatFragment, TAG_GAME_CHAT);
                    break;
                case TAG_GLOBAL_CHAT:
                    if (globalChatFragment != null)
                        transaction.add(R.id.main_container, globalChatFragment, TAG_GLOBAL_CHAT);
                    break;
            }
        }

        transaction.commitAllowingStateLoss();
    }

    @Override
    public void onParticipatingGame(@NonNull Integer gid) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        ongoingGameFragment = OngoingGameFragment.getInstance(gid, null);
        transaction.add(R.id.main_container, ongoingGameFragment, TAG_ONGOING_GAME);
        gameChatFragment = ChatFragment.getGameInstance(gid);
        transaction.add(R.id.main_container, gameChatFragment, TAG_GAME_CHAT).commitNowAllowingStateLoss();
        navigation.getMenu().clear();
        navigation.inflateMenu(R.menu.navigation_ongoing_game);
        if (!pyx.isGlobalChatEnabled()) navigation.getMenu().removeItem(R.id.main_globalChat);
        navigation.setSelectedItemId(R.id.main_ongoingGame);

        currentGid = gid;
    }

    @Override
    public void onLeftGame() {
        currentGid = -1;

        navigation.getMenu().clear();
        navigation.inflateMenu(R.menu.navigation_lobby);
        if (!pyx.isGlobalChatEnabled()) navigation.getMenu().removeItem(R.id.main_globalChat);
        navigation.setSelectedItemId(R.id.main_games);

        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        Fragment ongoingGame = manager.findFragmentByTag(TAG_ONGOING_GAME);
        if (ongoingGame != null) transaction.remove(ongoingGame);

        Fragment gameChat = manager.findFragmentByTag(TAG_GAME_CHAT);
        if (gameChat != null) transaction.remove(gameChat);

        transaction.commitAllowingStateLoss();

        ongoingGameFragment = null;
        gameChatFragment = null;

        AnalyticsApplication.sendAnalytics(this, Utils.ACTION_LEFT_GAME);
    }

    @Override
    public void viewGame(int gid, boolean locked) {
        if (gamesFragment != null) {
            navigation.setSelectedItemId(R.id.main_games);
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
                    Toaster.show(MainActivity.this, Utils.Messages.OPTIONS_CHANGED);
                }

                @Override
                public void onException(@NonNull Exception ex) {
                    dismissDialog();
                    Toaster.show(MainActivity.this, Utils.Messages.FAILED_CHANGING_OPTIONS, ex);
                }
            });
        } catch (JSONException ex) {
            dismissDialog();
            Toaster.show(this, Utils.Messages.FAILED_CHANGING_OPTIONS, ex);
        }
    }
}
