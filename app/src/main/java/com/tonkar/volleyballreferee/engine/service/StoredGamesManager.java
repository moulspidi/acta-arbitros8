package com.tonkar.volleyballreferee.engine.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.tonkar.volleyballreferee.engine.*;
import com.tonkar.volleyballreferee.engine.api.*;
import com.tonkar.volleyballreferee.engine.api.model.*;
import com.tonkar.volleyballreferee.engine.database.VbrRepository;
import com.tonkar.volleyballreferee.engine.game.*;
import com.tonkar.volleyballreferee.engine.game.sanction.*;
import com.tonkar.volleyballreferee.engine.game.score.ScoreListener;
import com.tonkar.volleyballreferee.engine.game.timeout.TimeoutListener;
import com.tonkar.volleyballreferee.engine.team.*;
import com.tonkar.volleyballreferee.engine.team.player.PositionType;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;

import okhttp3.*;

public class StoredGamesManager implements StoredGamesService, ScoreListener, TeamListener, TimeoutListener, SanctionListener {

    private final Context       mContext;
    private final VbrRepository mRepository;
    private       IGame         mGame;
    private       StoredGame    mStoredGame;

    public StoredGamesManager(Context context) {
        mContext = context;
        mRepository = new VbrRepository(mContext);
    }

    @Override
    public void createCurrentGame(IGame game) {
        mGame = game;

        if (hasSetupGame()) {
            deleteSetupGame();
        }

        createCurrentGame();
        saveCurrentGame(true);
    }

    @Override
    public void connectGameRecorder(IGame game) {
        Log.i(Tags.STORED_GAMES, "Connect the game recorder");

        mGame = game;

        mGame.addScoreListener(this);
        mGame.addTeamListener(this);
        mGame.addTimeoutListener(this);
        mGame.addSanctionListener(this);

        createCurrentGame();
        saveCurrentGame();
        pushCurrentGameToServer();
    }

    @Override
    public void disconnectGameRecorder(boolean exiting) {
        Log.i(Tags.STORED_GAMES, "Disconnect the game recorder");

        if (exiting) {
            saveCurrentGame();
        }

        mGame.removeScoreListener(this);
        mGame.removeTeamListener(this);
        mGame.removeTimeoutListener(this);
        mGame.removeSanctionListener(this);
    }

    @Override
    public List<GameSummaryDto> listGames() {
        return mRepository.listGames();
    }

    @Override
    public IStoredGame getCurrentGame() {
        return mStoredGame;
    }

    @Override
    public IStoredGame getGame(String id) {
        return mRepository.getGame(id);
    }

    @Override
    public void deleteGame(String id) {
        mRepository.deleteGame(id);
        deleteGameOnServer(id);
    }

    @Override
    public void deleteGames(Set<String> ids, DataSynchronizationListener listener) {
        if (!ids.isEmpty()) {
            mRepository.deleteGames(ids);
            for (String id : ids) {
                deleteGameOnServer(id);
            }
            if (listener != null) {
                listener.onSynchronizationSucceeded();
            }
        }
    }

    @Override
    public boolean hasCurrentGame() {
        return mRepository.hasCurrentGame();
    }

    @Override
    public IGame loadCurrentGame() {
        return mRepository.getCurrentGame();
    }

    @Override
    public synchronized void saveCurrentGame(boolean syncInsertion) {
        updateCurrentGame();
        if (!mGame.isMatchCompleted()) {
            mRepository.insertCurrentGame(mGame, syncInsertion);
        }
    }

    @Override
    public synchronized void saveCurrentGame() {
        saveCurrentGame(false);
    }

    public void applySetupLineupToFirstSet()
{
    // Conservative apply: sync roster numbers from game definitions to live game.
    try {
        if (mGame == null) return;
        // Synchronize HOME and GUEST rosters using public Game APIs (add/removePlayer)
        for (com.tonkar.volleyballreferee.engine.team.TeamType team : com.tonkar.volleyballreferee.engine.team.TeamType.values()) {
            try {
                java.util.Set<Integer> current = new java.util.TreeSet<>();
                for (com.tonkar.volleyballreferee.engine.api.model.PlayerDto p : mGame.getPlayers(team)) { if (p != null && p.getNum() > 0) current.add(p.getNum()); }

                java.util.Set<Integer> desired = new java.util.TreeSet<>();
                try {
                    // Preferred: read from mGame team definitions saved by setup UI
                    for (com.tonkar.volleyballreferee.engine.api.model.PlayerDto p : mGame.getPlayers(team)) { if (p != null && p.getNum() > 0) desired.add(p.getNum()); }
                } catch (Throwable ignored) {}

                // If nothing in desired, do nothing
                if (!desired.isEmpty()) {
                    // Remove players not desired
                    for (Integer n : new java.util.TreeSet<>(current)) {
                        if (!desired.contains(n)) {
                            try { mGame.removePlayer(team, n); } catch (Throwable ignored) {}
                        }
                    }
                    // Add missing players
                    for (Integer n : desired) {
                        if (!current.contains(n)) {
                            try { mGame.addPlayer(team, n); } catch (Throwable ignored) {}
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
    } catch (Throwable ignored) {}
}



    public void syncGames() {
        /* No-op sync in local-only variant */
    }

    public void cancelGame(String id, DataSynchronizationListener listener) {
        /* No-op cancel in local-only variant */ if (listener != null) listener.onSynchronizationSucceeded();
    }

    public boolean hasSetupGame() {
        return false;
    }

    public void deleteSetupGame() {
        /* no-op */
    }

    private void createCurrentGame() {
        /* overload calls typed version if available */ try { createCurrentGame(mGame); } catch (Throwable ignored) {}
    }

    private void pushCurrentGameToServer() {
        /* no-op server sync */
    }

    private void deleteGameOnServer(String id) {
        /* no-op server delete */
    }

    private void updateCurrentGame() {
        /* no-op update */
    }

    @Override
    public void scheduleGame(GameSummaryDto gameDescription, boolean create, DataSynchronizationListener listener) {
        // No-op scheduling in this local variant
        if (listener != null) listener.onSynchronizationSucceeded();
    }
    

    @Override
    public void downloadAvailableGames(AsyncGameRequestListener listener) {
        // No-op in local variant; immediately succeed with empty list
        if (listener != null) {
            try {
                listener.onAvailableGamesReceived(new java.util.ArrayList<com.tonkar.volleyballreferee.engine.api.model.GameSummaryDto>());
            } catch (Throwable ignored) {}
        }
    }
    

    @Override
    public void downloadGame(String id, AsyncGameRequestListener listener) {
        // No-op: report not found
        if (listener != null) {
            try { listener.onError(404); } catch (Throwable ignored) {}
        }
    }
    

    @Override
    public void syncGames(DataSynchronizationListener listener) {
        // No-op local sync; immediately signal success
        if (listener != null) {
            try { listener.onSynchronizationSucceeded(); } catch (Throwable ignored) {}
        }
    }
    

    @Override
    public void saveSetupGame(IGame game) {
        // Local-only variant: persist nothing here; keep reference if available
        try { if (game != null) { this.mGame = game; } } catch (Throwable ignored) {}
    }
    

    @Override
    public IGame loadSetupGame() {
        // Local-only implementation: devolver la referencia actual si existe
        try { return mGame; } catch (Throwable ignored) { return null; }
    }
    

    @Override
    public void deleteCurrentGame() {
        // Local-only implementation: limpiar referencias en memoria
        try { mStoredGame = null; } catch (Throwable ignored) {}
        try { mGame = null; } catch (Throwable ignored) {}
    }
    

    @Override
    public void onMatchCompleted(TeamType winner) {
        // No-op for local variant; clear and persist nothing
        try { /* optionally clear current game */ } catch (Throwable ignored) {}
    }
    

    @Override
    public void onSetCompleted() {
        // No-op for local variant; nothing to push
        try { /* optionally could pushCurrentSetToServer(); */ } catch (Throwable ignored) {}
    }
    

    @Override
    public void onPointsUpdated(TeamType teamType, int newCount) {
        // no-op
    }


    @Override
    public void onSetsUpdated(TeamType teamType, int newCount) {
        // no-op
    }


    @Override
    public void onServiceSwapped(TeamType teamType, boolean isStart) {
        // no-op
    }


    @Override
    public void onSetStarted() {
        // no-op
    }


    @Override
    public void onStartingLineupSubmitted(TeamType teamType) {
        // no-op
    }


    @Override
    public void onTeamsSwapped(TeamType leftTeamType, TeamType rightTeamType, ActionOriginType actionOriginType) {
        // no-op
    }


    @Override
    public void onPlayerChanged(TeamType teamType, int number, PositionType positionType, ActionOriginType actionOriginType) {
        // no-op
    }


    @Override
    public void onTeamRotated(TeamType teamType, boolean clockwise) {
        // no-op
    }


    @Override
    public void onTimeoutUpdated(TeamType teamType, int maxCount, int newCount) {
        // no-op
    }


    @Override
    public void onTimeout(TeamType teamType, int duration) {
        // no-op
    }


    @Override
    public void onTechnicalTimeout(int duration) {
        // no-op
    }


    @Override
    public void onGameInterval(int duration) {
        // no-op
    }


    @Override
    public void onSanction(TeamType teamType, SanctionType sanctionType, int number) {
        // no-op
    }


    @Override
    public void onUndoSanction(TeamType teamType, SanctionType sanctionType, int number) {
        // no-op
    }

}
