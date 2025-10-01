package com.tonkar.volleyballreferee.engine.game;

import com.tonkar.volleyballreferee.engine.team.TeamType;
import com.tonkar.volleyballreferee.engine.api.model.SanctionDto;

import com.tonkar.volleyballreferee.engine.game.sanction.ISanction;
import com.tonkar.volleyballreferee.engine.game.score.IScore;
import com.tonkar.volleyballreferee.engine.game.timeout.ITimeout;
import com.tonkar.volleyballreferee.engine.service.IStoredGame;
import com.tonkar.volleyballreferee.engine.team.ITeam;

import java.util.List;

public interface IGame extends IGeneral, IScore, ITeam, ITimeout, ISanction {

    boolean areNotificationsEnabled();

    void restoreGame(IStoredGame storedGame);

    List<GameEvent> getLatestGameEvents();

    void undoGameEvent(GameEvent gameEvent);


    void markLastSanctionAsImproperRequest(TeamType teamType);
    SanctionDto getLastSanction(TeamType teamType);
}
