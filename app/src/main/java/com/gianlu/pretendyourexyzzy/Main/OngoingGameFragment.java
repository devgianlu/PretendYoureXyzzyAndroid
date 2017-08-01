package com.gianlu.pretendyourexyzzy.Main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.NetIO.GameManager;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardSet;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameCards;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfo;
import com.gianlu.pretendyourexyzzy.NetIO.Models.User;
import com.gianlu.pretendyourexyzzy.NetIO.PYX;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.utils.URIBuilder;
import cz.msebera.android.httpclient.client.utils.URLEncodedUtils;
import cz.msebera.android.httpclient.message.BasicNameValuePair;

public class OngoingGameFragment extends Fragment implements PYX.IResult<GameInfo>, GameManager.IManager {
    private IFragment handler;
    private FrameLayout layout;
    private ProgressBar loading;
    private LinearLayout container;
    private GameManager manager;
    private User me;
    private int gameId;
    private PYX pyx;

    public static OngoingGameFragment getInstance(Game game, User me, OngoingGameFragment.IFragment handler) {
        OngoingGameFragment fragment = new OngoingGameFragment();
        fragment.handler = handler;
        fragment.setHasOptionsMenu(true);
        Bundle args = new Bundle();
        args.putSerializable("me", me);
        args.putSerializable("game", game);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        layout = (FrameLayout) inflater.inflate(R.layout.ongoing_game_fragment, parent, false);
        loading = layout.findViewById(R.id.ongoingGame_loading);
        container = layout.findViewById(R.id.ongoingGame_container);

        me = (User) getArguments().getSerializable("me");
        Game game = (Game) getArguments().getSerializable("game");
        if (game == null) {
            loading.setVisibility(View.GONE);
            container.setVisibility(View.GONE);
            MessageLayout.show(layout, R.string.failedLoading, R.drawable.ic_error_outline_black_48dp);
            return layout;
        }

        gameId = game.gid;
        pyx = PYX.get(getContext());
        pyx.getGameInfo(game.gid, this);

        return layout;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) updateActivityTitle();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityTitle();
    }

    private void updateActivityTitle() {
        Activity activity = getActivity();
        if (manager != null && activity != null && isVisible())
            activity.setTitle(manager.gameInfo.game.host + " - " + getString(R.string.app_name));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.ongoing_game, menu);
    }

    private void leaveGame() {
        PYX.get(getContext()).leaveGame(gameId, new PYX.ISuccess() {
            @Override
            public void onDone(PYX pyx) {
                if (handler != null) handler.onLeftGame();
            }

            @Override
            public void onException(Exception ex) {
                Toaster.show(getActivity(), Utils.Messages.FAILED_LEAVING, ex);
            }
        });
    }

    private boolean amHost() {
        return getGame() != null && Objects.equals(getGame().host, me.nickname);
    }

    private void loadCardCastSetsAndShowDialog() {
        pyx.listCardCastCardSets(gameId, new PYX.IResult<List<CardSet>>() {
            @Override
            public void onDone(PYX pyx, List<CardSet> result) {
                showCardCastDialog(result);
            }

            @Override
            public void onException(Exception ex) {
                Toaster.show(getActivity(), Utils.Messages.FAILED_LOADING, ex);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ongoingGame_leave:
                leaveGame();
                return true;
            case R.id.ongoingGame_options:
                if (amHost() && manager.gameInfo.game.status == Game.Status.LOBBY)
                    editGameOptions();
                else showGameOptions();
                return true;
            case R.id.ongoingGame_spectators:
                showSpectators();
                return true;
            case R.id.ongoingGame_share:
                shareGame();
                return true;
            case R.id.ongoingGame_cardcast:
                loadCardCastSetsAndShowDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showCardCastDialog(List<CardSet> sets) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.cardcast)
                .setNeutralButton(android.R.string.ok, null);

        if (amHost()) builder.setPositiveButton(R.string.add, null);

        if (sets.isEmpty())
            builder.setMessage(R.string.noCardSets);
        else
            builder.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, sets), null);


        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        askForCardCastCode(new ICardCast() {
                            @Override
                            public void onCardCastCode(String code) {
                                pyx.addCardCastCardSet(gameId, code, new PYX.ISuccess() {
                                    @Override
                                    public void onDone(PYX pyx) {
                                        Toaster.show(getActivity(), Utils.Messages.CARDCAST_ADDED);
                                        dialog.dismiss();

                                        loadCardCastSetsAndShowDialog();
                                    }

                                    @Override
                                    public void onException(Exception ex) {
                                        Toaster.show(getActivity(), Utils.Messages.FAILED_ADDING_CARDCAST, ex);
                                        dialog.dismiss();
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });

        CommonUtils.showDialog(getActivity(), dialog);
    }

    private void askForCardCastCode(final ICardCast listener) {
        final EditText code = new EditText(getContext());
        code.setAllCaps(true);
        code.setHint("XXXXX");
        code.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.addCardCast)
                .setView(code)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        listener.onCardCastCode(code.getText().toString());
                    }
                });

        CommonUtils.showDialog(getActivity(), builder);
    }

    private void shareGame() {
        if (getGame() == null) return;
        URI uri = pyx.server.uri;
        URIBuilder builder = new URIBuilder();
        builder.setScheme(uri.getScheme())
                .setHost(uri.getHost())
                .setPath("/zy/game.jsp");

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("game", String.valueOf(gameId)));
        if (getGame().hasPassword)
            params.add(new BasicNameValuePair("password", getGame().options.password));
        builder.setFragment(URLEncodedUtils.format(params, Charset.forName("UTF-8")));

        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, builder.build().toASCIIString());
            startActivity(Intent.createChooser(i, "Share game..."));
        } catch (URISyntaxException ex) {
            Toaster.show(getActivity(), Utils.Messages.FAILED_SHARING, ex);
        }
    }

    private void showSpectators() {
        if (getGame() == null) return;
        SuperTextView spectators = new SuperTextView(getContext(), R.string.spectatorsList, getGame().spectators.isEmpty() ? "none" : CommonUtils.join(getGame().spectators, ", "));
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
        spectators.setPadding(padding, padding, padding, padding);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.spectatorsLabel)
                .setView(spectators)
                .setPositiveButton(android.R.string.ok, null);

        CommonUtils.showDialog(getActivity(), builder);
    }

    @SuppressLint("InflateParams")
    private void editGameOptions() {
        if (getGame() == null) return;
        Game.Options options = getGame().options;
        final ScrollView layout = (ScrollView) LayoutInflater.from(getContext()).inflate(R.layout.edit_game_options_dialog, null, false);
        final TextInputLayout scoreLimit = layout.findViewById(R.id.editGameOptions_scoreLimit);
        CommonUtils.setText(scoreLimit, String.valueOf(options.scoreLimit));
        final TextInputLayout playerLimit = layout.findViewById(R.id.editGameOptions_playerLimit);
        CommonUtils.setText(playerLimit, String.valueOf(options.playersLimit));
        final TextInputLayout spectatorLimit = layout.findViewById(R.id.editGameOptions_spectatorLimit);
        CommonUtils.setText(spectatorLimit, String.valueOf(options.spectatorsLimit));
        final Spinner idleTimeMultiplier = layout.findViewById(R.id.editGameOptions_idleTimeMultiplier);
        idleTimeMultiplier.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, Game.Options.VALID_TIME_MULTIPLIERS));
        idleTimeMultiplier.setSelection(CommonUtils.indexOf(Game.Options.VALID_TIME_MULTIPLIERS, options.timeMultiplier));
        final TextInputLayout blankCards = layout.findViewById(R.id.editGameOptions_blankCards);
        CommonUtils.setText(blankCards, String.valueOf(options.blanksLimit));
        final TextInputLayout password = layout.findViewById(R.id.editGameOptions_password);
        CommonUtils.setText(password, options.password);
        final LinearLayout cardSets = layout.findViewById(R.id.editGameOptions_cardSets);
        cardSets.removeAllViews();
        for (CardSet set : pyx.firstLoad.cardSets) {
            CheckBox item = new CheckBox(getContext());
            item.setTag(set);
            item.setText(set.name);
            item.setChecked(getGame().options.cardSets.contains(set.id));
            cardSets.addView(item);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.editGameOptions)
                .setView(layout)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.apply, null);

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View button) {
                        scoreLimit.setErrorEnabled(false);
                        playerLimit.setErrorEnabled(false);
                        spectatorLimit.setErrorEnabled(false);
                        blankCards.setErrorEnabled(false);
                        password.setErrorEnabled(false);

                        Game.Options newOptions;
                        try {
                            newOptions = Game.Options.validateAndCreate(idleTimeMultiplier.getSelectedItem().toString(), CommonUtils.getText(spectatorLimit), CommonUtils.getText(playerLimit), CommonUtils.getText(scoreLimit), CommonUtils.getText(blankCards), cardSets, CommonUtils.getText(password));
                        } catch (Game.Options.InvalidFieldException ex) {
                            View view = layout.findViewById(ex.fieldId);
                            if (view != null && view instanceof TextInputLayout) {
                                if (ex.throwMessage == R.string.outOfRange)
                                    ((TextInputLayout) view).setError(getString(R.string.outOfRange, ex.min, ex.max));
                                else
                                    ((TextInputLayout) view).setError(getString(ex.throwMessage));
                            }
                            return;
                        }

                        dialog.dismiss();
                        pyx.changeGameOptions(gameId, newOptions, new PYX.ISuccess() {
                            @Override
                            public void onDone(PYX pyx) {
                                Toaster.show(getActivity(), Utils.Messages.OPTIONS_CHANGED);
                            }

                            @Override
                            public void onException(Exception ex) {
                                Toaster.show(getActivity(), Utils.Messages.FAILED_CHANGING_OPTIONS, ex);
                            }
                        });
                    }
                });
            }
        });

        CommonUtils.showDialog(getActivity(), dialog);
    }

    @SuppressLint("InflateParams")
    private void showGameOptions() {
        if (getGame() == null) return;
        Game.Options options = getGame().options;
        ScrollView layout = (ScrollView) LayoutInflater.from(getContext()).inflate(R.layout.game_options_dialog, null, false);
        SuperTextView scoreLimit = layout.findViewById(R.id.gameOptions_scoreLimit);
        scoreLimit.setHtml(R.string.scoreLimit, options.scoreLimit);
        SuperTextView playerLimit = layout.findViewById(R.id.gameOptions_playerLimit);
        playerLimit.setHtml(R.string.playerLimit, options.playersLimit);
        SuperTextView spectatorLimit = layout.findViewById(R.id.gameOptions_spectatorLimit);
        spectatorLimit.setHtml(R.string.spectatorLimit, options.spectatorsLimit);
        SuperTextView idleTimeMultiplier = layout.findViewById(R.id.gameOptions_idleTimeMultiplier);
        idleTimeMultiplier.setHtml(R.string.timeMultiplier, options.timeMultiplier);
        SuperTextView cardSets = layout.findViewById(R.id.gameOptions_cardSets);
        cardSets.setHtml(R.string.cardSets, options.cardSets.isEmpty() ? "none" : CommonUtils.join(pyx.firstLoad.createCardSetNamesList(options.cardSets), ", "));
        SuperTextView blankCards = layout.findViewById(R.id.gameOptions_blankCards);
        blankCards.setHtml(R.string.blankCards, options.blanksLimit);
        SuperTextView password = layout.findViewById(R.id.gameOptions_password);
        if (options.password == null || options.password.isEmpty())
            password.setVisibility(View.GONE);
        else password.setHtml(R.string.password, options.password);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.gameOptions)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, null);

        CommonUtils.showDialog(getActivity(), builder);
    }

    @Override
    public void onDone(PYX pyx, final GameInfo gameInfo) {
        if (manager == null) manager = new GameManager(container, gameInfo, me, this);
        pyx.pollingThread.addListener(manager);
        updateActivityTitle();

        pyx.getGameCards(gameInfo.game.gid, new PYX.IResult<GameCards>() {
            @Override
            public void onDone(PYX pyx, GameCards gameCards) {
                manager.setCards(gameCards);
                loading.setVisibility(View.GONE);
                container.setVisibility(View.VISIBLE);
                MessageLayout.hide(layout);
            }

            @Override
            public void onException(Exception ex) {
                OngoingGameFragment.this.onException(ex);
            }
        });
    }

    @Nullable
    private Game getGame() {
        return manager == null ? null : manager.gameInfo.game;
    }

    @Override
    public void onException(Exception ex) {
        Logging.logMe(getContext(), ex);
        loading.setVisibility(View.GONE);
        container.setVisibility(View.GONE);
        if (isAdded())
            MessageLayout.show(layout, getString(R.string.failedLoading_reason, ex.getMessage()), R.drawable.ic_error_outline_black_48dp);
    }

    @Override
    public void notifyWinner(String nickname) {
        if (isAdded())
            Toaster.show(getActivity(), getString(R.string.winnerIs, nickname), Toast.LENGTH_SHORT, null, null, null);
    }

    @Override
    public void notifyPlayerSkipped(@Nullable String nickname) {
        if (isAdded())
            Toaster.show(getActivity(), getString(R.string.playerSkipped, nickname == null ? "" : nickname), Toast.LENGTH_SHORT, null, null, null);
    }

    @Override
    public void notifyJudgeSkipped(@Nullable String nickname) {
        if (isAdded())
            Toaster.show(getActivity(), getString(R.string.judgeSkipped, nickname == null ? "" : nickname), Toast.LENGTH_SHORT, null, null, null);
    }

    @Override
    public void cannotStartGame(Exception ex) {
        if (isAdded()) Toaster.show(getActivity(), Utils.Messages.FAILED_START_GAME, ex);
    }

    @Override
    public void showDialog(AlertDialog.Builder builder) {
        if (isAdded()) CommonUtils.showDialog(getActivity(), builder);
    }

    @Override
    public void kicked() {
        if (handler != null) handler.onLeftGame();
    }

    @Override
    public void showToast(Toaster.Message message) {
        if (isAdded()) Toaster.show(getActivity(), message);
    }

    public interface IFragment {
        void onLeftGame();
    }

    private interface ICardCast {
        void onCardCastCode(String code);
    }
}
