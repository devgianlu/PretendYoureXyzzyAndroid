package com.gianlu.pretendyourexyzzy;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Main.GameChatFragment;
import com.gianlu.pretendyourexyzzy.Main.GamesFragment;
import com.gianlu.pretendyourexyzzy.Main.GlobalChatFragment;
import com.gianlu.pretendyourexyzzy.Main.NamesFragment;
import com.gianlu.pretendyourexyzzy.Main.OngoingGameFragment;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.User;
import com.gianlu.pretendyourexyzzy.NetIO.PYX;

import java.util.Objects;

// TODO: Show starred cards list
public class MainActivity extends AppCompatActivity implements GamesFragment.IFragment, OngoingGameFragment.IFragment {
    private final static String TAG_GLOBAL_CHAT = "globalChat";
    private final static String TAG_GAMES = "games";
    private final static String TAG_GAME_CHAT = "gameChat";
    private static final String TAG_PLAYERS = "players";
    private static final String TAG_ONGOING_GAME = "ongoingGame";
    private BottomNavigationView navigation;
    private NamesFragment namesFragment;
    private GlobalChatFragment globalChatFragment;
    private GamesFragment gamesFragment;
    private GameChatFragment gameChatFragment;
    private OngoingGameFragment ongoingGameFragment;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        user = (User) getIntent().getSerializableExtra("user");
        if (user == null) {
            Toaster.show(this, Utils.Messages.FAILED_LOADING, new NullPointerException("user is null!"));
            finish();
            return;
        }

        namesFragment = NamesFragment.getInstance();
        globalChatFragment = GlobalChatFragment.getInstance();
        gamesFragment = GamesFragment.getInstance(this);

        navigation = (BottomNavigationView) findViewById(R.id.main_navigation);
        Menu menu = navigation.getMenu();
        menu.removeItem(R.id.main_ongoingGame);
        menu.removeItem(R.id.main_gameChat);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.main_players:
                        setTitle(getString(R.string.playersLabel) + " - " + getString(R.string.app_name));
                        switchTo(TAG_PLAYERS);
                        break;
                    case R.id.main_globalChat:
                        setTitle(getString(R.string.globalChat) + " - " + getString(R.string.app_name));
                        switchTo(TAG_GLOBAL_CHAT);
                        break;
                    case R.id.main_games:
                        setTitle(getString(R.string.games) + " - " + getString(R.string.app_name));
                        switchTo(TAG_GAMES);
                        break;
                    case R.id.main_ongoingGame:
                        setTitle(getString(R.string.game) + " - " + getString(R.string.app_name));
                        switchTo(TAG_ONGOING_GAME);
                        break;
                    case R.id.main_gameChat:
                        setTitle(getString(R.string.gameChat) + " - " + getString(R.string.app_name));
                        switchTo(TAG_GAME_CHAT);
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
                    case R.id.main_globalChat:
                        if (globalChatFragment != null) globalChatFragment.scrollToTop();
                        break;
                    case R.id.main_games:
                        if (gamesFragment != null) gamesFragment.scrollToTop();
                        break;
                    case R.id.main_gameChat:
                        if (gameChatFragment != null) gameChatFragment.scrollToTop();
                        break;
                }
            }
        });

        navigation.setSelectedItemId(R.id.main_games);
        setKeepScreenOn(Prefs.getBoolean(this, Prefs.Keys.KEEP_SCREEN_ON, true));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.main_keepScreenOn).setChecked(Prefs.getBoolean(this, Prefs.Keys.KEEP_SCREEN_ON, true));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_logout:
                PYX.get(this).logout();
                startActivity(new Intent(this, LoadingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
                return true;
            case R.id.main_keepScreenOn:
                item.setChecked(!item.isChecked());
                Prefs.putBoolean(this, Prefs.Keys.KEEP_SCREEN_ON, item.isChecked());
                setKeepScreenOn(item.isChecked());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setKeepScreenOn(boolean on) {
        if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void switchTo(String tag) {
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
                case TAG_GLOBAL_CHAT:
                    transaction.add(R.id.main_container, globalChatFragment, TAG_GLOBAL_CHAT);
                    break;
                case TAG_GAMES:
                    transaction.add(R.id.main_container, gamesFragment, TAG_GAMES);
                    break;
                case TAG_ONGOING_GAME:
                    transaction.add(R.id.main_container, ongoingGameFragment, TAG_ONGOING_GAME);
                    break;
                case TAG_GAME_CHAT:
                    transaction.add(R.id.main_container, gameChatFragment, TAG_GAME_CHAT);
                    break;
            }
        }

        transaction.commit();
    }

    @Override
    public void onJoinedGame(Game game) { // TODO
        ongoingGameFragment = OngoingGameFragment.getInstance(game, user, this);
        gameChatFragment = GameChatFragment.getInstance(game);
        navigation.getMenu().clear();
        navigation.inflateMenu(R.menu.navigation);
        Menu menu = navigation.getMenu();
        menu.removeItem(R.id.main_games);
        navigation.setSelectedItemId(R.id.main_ongoingGame);
    }

    @Override
    public void onSpectatingGame(Game game) {
        ongoingGameFragment = OngoingGameFragment.getInstance(game, user, this);
        gameChatFragment = GameChatFragment.getInstance(game);
        navigation.getMenu().clear();
        navigation.inflateMenu(R.menu.navigation);
        Menu menu = navigation.getMenu();
        menu.removeItem(R.id.main_games);
        navigation.setSelectedItemId(R.id.main_ongoingGame);
    }

    @Override
    public void onLeftGame() {
        navigation.getMenu().clear();
        navigation.inflateMenu(R.menu.navigation);
        Menu menu = navigation.getMenu();
        menu.removeItem(R.id.main_ongoingGame);
        menu.removeItem(R.id.main_gameChat);
        navigation.setSelectedItemId(R.id.main_games);

        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        Fragment ongoingGame = manager.findFragmentByTag(TAG_ONGOING_GAME);
        if (ongoingGame != null) transaction.remove(ongoingGame);

        Fragment gameChat = manager.findFragmentByTag(TAG_GAME_CHAT);
        if (gameChat != null) transaction.remove(gameChat);

        transaction.commit();

        ongoingGameFragment = null;
        gameChatFragment = null;
    }
}
