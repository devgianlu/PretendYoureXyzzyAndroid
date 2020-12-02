package com.gianlu.pretendyourexyzzy.metrics;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.misc.BreadcrumbsView;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.GamePermalink;
import com.gianlu.pretendyourexyzzy.api.models.metrics.SessionHistory;
import com.gianlu.pretendyourexyzzy.api.models.metrics.UserHistory;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewMetricsBinding;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

final class MetricsDelegate implements BreadcrumbsView.Listener {
    private static final int TYPE_USER = 0;
    private static final int TYPE_SESSION = 1;
    private static final int TYPE_GAME = 2;
    private static final String TAG_USER = UserHistoryFragment.class.getName();
    private static final String TAG_SESSION = SessionHistoryFragment.class.getName();
    private static final String TAG_GAME = GameHistoryFragment.class.getName();
    private static final String TAG = MetricsDelegate.class.getSimpleName();
    private final FragmentManager fragmentManager;
    private final com.gianlu.pretendyourexyzzy.databinding.FragmentNewMetricsBinding binding;
    private final GamePermalink game;
    private final Listener listener;
    private RegisteredPyx pyx;

    MetricsDelegate(@NotNull FragmentManager fragmentManager, @NotNull FragmentNewMetricsBinding binding, @Nullable GamePermalink game, @NotNull Listener listener) {
        this.fragmentManager = fragmentManager;
        this.binding = binding;
        this.game = game;
        this.listener = listener;

        binding.metricsFragmentBreadcrumbs.setListener(this);
    }

    void setPyx(@NotNull RegisteredPyx pyx) {
        this.pyx = pyx;

        if (game == null) loadUserHistory();
        else loadGame(game);
    }

    void unsetPyx() {
        this.pyx = null;
    }

    void destroy() {
        unsetPyx();
        binding.metricsFragmentBreadcrumbs.clearListener();
    }

    boolean navigateBack() {
        return binding.metricsFragmentBreadcrumbs.navigateBack();
    }

    void loadUserHistory() {
        binding.metricsFragmentBreadcrumbs.clear();
        binding.metricsFragmentBreadcrumbs.addItem(new BreadcrumbsView.Item(pyx.user().nickname, TYPE_USER));

        fragmentManager
                .beginTransaction()
                .replace(binding.metricsFragmentContainer.getId(), UserHistoryFragment.get(), TAG_USER)
                .commit();
    }

    void loadSession(@NonNull UserHistory.Session session) {
        binding.metricsFragmentBreadcrumbs.addItem(new BreadcrumbsView.Item(session.name(), TYPE_SESSION, session));

        fragmentManager
                .beginTransaction()
                .replace(binding.metricsFragmentContainer.getId(), SessionHistoryFragment.get(session.id), TAG_SESSION)
                .commit();
    }

    private void loadGame(@NonNull GamePermalink game) {
        String gameId = game.extractGameMetricsId();
        if (gameId == null) {
            listener.showToast(Toaster.build().message(R.string.failedLoading));
            listener.leave();
            return;
        }

        listener.showProgress(R.string.loading);
        pyx.getGameHistory(gameId)
                .addOnSuccessListener(result -> {
                    listener.dismissDialog();
                    binding.metricsFragmentBreadcrumbs.addItem(new BreadcrumbsView.Item(binding.getRoot().getContext().getString(R.string.ongoingGame), TYPE_GAME, null));
                    fragmentManager
                            .beginTransaction()
                            .replace(binding.metricsFragmentContainer.getId(), GameHistoryFragment.get(result), TAG_GAME)
                            .commit();
                })
                .addOnFailureListener(ex -> {
                    listener.dismissDialog();
                    Log.e(TAG, "Failed loading history.", ex);
                    listener.showToast(Toaster.build().message(R.string.failedLoading));
                    listener.leave();
                });
    }

    public void loadGame(@NonNull SessionHistory.Game game) {
        binding.metricsFragmentBreadcrumbs.addItem(new BreadcrumbsView.Item(CommonUtils.getFullDateFormatter().format(new Date(game.timestamp)), TYPE_GAME, game));

        fragmentManager
                .beginTransaction()
                .replace(binding.metricsFragmentContainer.getId(), GameHistoryFragment.get(game.id), TAG_GAME)
                .commit();
    }

    @Override
    public void onSegmentSelected(@NonNull BreadcrumbsView.Item item) {
        binding.metricsFragmentBreadcrumbs.removeFrom(item);

        switch (item.type) {
            case TYPE_USER:
                loadUserHistory();
                break;
            case TYPE_SESSION:
                if (item.data != null) loadSession((UserHistory.Session) item.data);
                break;
            case TYPE_GAME:
                if (item.data != null) loadGame((SessionHistory.Game) item.data);
                break;
        }
    }

    public interface Listener extends DialogUtils.ShowStuffInterface {
        void leave();
    }
}
