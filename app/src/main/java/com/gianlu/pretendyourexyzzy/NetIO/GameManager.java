package com.gianlu.pretendyourexyzzy.NetIO;

import android.support.annotation.NonNull;

import com.gianlu.pretendyourexyzzy.BuildConfig;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameCards;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfo;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;

import java.util.List;

public class GameManager implements PYX.IResult<List<PollMessage>> {
    public GameInfo gameInfo;
    public GameCards cards;

    public GameManager(@NonNull GameInfo gameInfo) {
        this.gameInfo = gameInfo;
    }

    public void updateInfo(@NonNull GameInfo gameInfo) {
        this.gameInfo = gameInfo;
    }

    public void updateCards(@NonNull GameCards cards) {
        this.cards = cards;
    }

    private void handlePollMessage(PollMessage message) {
        switch (message.event) {
            case GAME_LIST_REFRESH:
            case NOOP:
            case CHAT:
                // Not interested in these
                return;
            case GAME_PLAYER_JOIN:

                break;
        }
    }

    @Override
    public void onDone(PYX pyx, List<PollMessage> result) {
        for (PollMessage message : result) handlePollMessage(message);
    }

    @Override
    public void onException(Exception ex) {
        if (BuildConfig.DEBUG) ex.printStackTrace();
    }
}
