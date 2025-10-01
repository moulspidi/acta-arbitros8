package com.tonkar.volleyballreferee.engine.scoresheet;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.Base64;

import androidx.annotation.DrawableRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import com.tonkar.volleyballreferee.R;
import com.tonkar.volleyballreferee.engine.api.model.*;
import com.tonkar.volleyballreferee.engine.game.GameType;
import com.tonkar.volleyballreferee.engine.game.sanction.SanctionType;
import com.tonkar.volleyballreferee.engine.service.IStoredGame;
import com.tonkar.volleyballreferee.engine.team.TeamType;
import com.tonkar.volleyballreferee.engine.team.player.PositionType;
import com.tonkar.volleyballreferee.ui.util.UiUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

import java.io.ByteArrayOutputStream;
import java.text.*;
import java.util.*;

public class ScoreSheetBuilder {

    private final Context     mContext;
    private final IStoredGame mStoredGame;
    private final String      mFilename;
    private       Document    mDocument;
    private       Element     mBody;
    private final DateFormat  mDateFormatter;
    private final DateFormat  mTimeFormatter;

    private String mLogo;
    private String mRemarks;
    private String mReferee1Signature;
    private String mReferee2Signature;
    private String mScorerSignature;
    private String mHomeCaptainSignature;
    private String mHomeCoachSignature;
    private String mGuestCaptainSignature;
    private String mGuestCoachSignature;
    private String mReferee1Name;
    private String mReferee2Name;
    private String mScorerName;
    private String mHomeCaptainName;
    private String mHomeCoachName;
    private String mGuestCaptainName;
    private String mGuestCoachName;
    // --- Licencias (nuevos campos) ---
    private String mReferee1License;
    private String mReferee2License;
    private String mScorerLicense;

    // --- Licencias extra: Assistant Coaches & Staff ---
    private String mHomeCoachLicence;
    private String mGuestCoachLicence;
    private String mHomeStaffLicence;
    private String mGuestStaffLicence;

    public record ScoreSheet(String filename, String content) {}

    public ScoreSheet createScoreSheet() {
        mDocument = Jsoup.parse(htmlSkeleton(mFilename), "UTF-8");
        mBody = mDocument.body();

        String html = switch (mStoredGame.getKind()) {
            case INDOOR -> createStoredIndoorGame();
            case BEACH -> createStoredBeachGame();
            case INDOOR_4X4 -> createStoredIndoor4x4Game();
            case SNOW -> createStoredSnowGame();
        };

        return new ScoreSheet(mFilename, html);
    }

    public ScoreSheetBuilder(Context context, IStoredGame storedGame) {
        mContext = context;
        mStoredGame = storedGame;

        DateFormat formatter = new SimpleDateFormat("dd_MM_yyyy", Locale.getDefault());
        formatter.setTimeZone(TimeZone.getDefault());
        String date = formatter.format(new Date(storedGame.getScheduledAt()));

        String homeTeam = storedGame.getTeamName(TeamType.HOME);
        String guestTeam = storedGame.getTeamName(TeamType.GUEST);

        String filename = String.format(Locale.getDefault(), "%s__%s__%s.html", homeTeam, guestTeam, date);
        mFilename = filename.replaceAll("[\\s|\\?\\*<:>\\+\\[\\]/\\']", "_");

        mDateFormatter = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault());
        mDateFormatter.setTimeZone(TimeZone.getDefault());

        mTimeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
        mTimeFormatter.setTimeZone(TimeZone.getDefault());

        mRemarks = "";
    }

    public String getFilename() {
        return mFilename;
    }

    private String createStoredIndoorGame() {
        mBody.appendChild(createStoredGameHeader());
        mBody.appendChild(createStoredTeams());

        for (int setIndex = 0; setIndex < mStoredGame.getNumberOfSets(); setIndex++) {
            Element cardDiv = new Element("div");
            cardDiv.addClass("div-card").addClass("spacing-before");
            if (setIndex % 2 == 1 || (setIndex == 0 && (mStoredGame.getPlayers(TeamType.HOME).size() > 14 || mStoredGame
                    .getPlayers(TeamType.GUEST)
                    .size() > 14)) || mStoredGame.getSubstitutions(TeamType.HOME, setIndex).size() > 6 || mStoredGame
                    .getSubstitutions(TeamType.GUEST, setIndex)
                    .size() > 6 || (mStoredGame.getPoints(TeamType.HOME, setIndex) + mStoredGame.getPoints(TeamType.GUEST,
                                                                                                           setIndex) > 64)) {
                cardDiv.addClass("new-page-for-printers");
            }
            cardDiv.attr("id", String.format(Locale.getDefault(), "div-set-%d", (1 + setIndex)));
            cardDiv.appendChild(createStoredSetHeader(setIndex));

            Element line2Div = new Element("div");
            line2Div.addClass("div-flex-row");
            line2Div
                    .appendChild(createStoredStartingLineup(setIndex))
                    .appendChild(createSpacingDiv())
                    .appendChild(createSpacingDiv())
                    .appendChild(createStoredSubstitutions(setIndex));
            if (mStoredGame.getRules().isTeamTimeouts()) {
                line2Div.appendChild(createSpacingDiv()).appendChild(createSpacingDiv()).appendChild(createStoredTimeouts(setIndex));
            }
            cardDiv.appendChild(line2Div);

            cardDiv.appendChild(createStoredLadder(setIndex));

            mBody.appendChild(cardDiv);
        }

        mBody.appendChild(createStoredGameHeader().addClass("new-page-for-printers"));
        mBody.appendChild(createRemarks());
        mBody.appendChild(createSignatures());
        mBody.appendChild(createLicencesCard());
        mBody.appendChild(createFooter());

        return mDocument.toString();
    }

    private Element createStoredGameHeader() {
        Element cardDiv = new Element("div");
        cardDiv.addClass("div-card");

        if (mLogo != null) {
            Element logoDiv = new Element("div");
            logoDiv.addClass("div-grid-game-header-logo");
            logoDiv.appendChild(createLogoBox(mLogo));

            cardDiv.appendChild(logoDiv);
        }

        Element gameInfoDiv = new Element("div");
        gameInfoDiv.addClass("div-grid-game-header-info");

        SelectedLeagueDto selectedLeague = mStoredGame.getLeague();
        String league = selectedLeague == null ? "" : selectedLeague.getName() + " / " + selectedLeague.getDivision();

        gameInfoDiv.appendChild(createCellSpan(league, true, false));
        gameInfoDiv.appendChild(createCellSpan(mDateFormatter.format(new Date(mStoredGame.getStartTime())), true, false));

        String startEndTimes = String.format("%s \u2192 %s", mTimeFormatter.format(new Date(mStoredGame.getStartTime())),
                                             mTimeFormatter.format(new Date(mStoredGame.getEndTime())));
        gameInfoDiv.appendChild(createCellSpan(startEndTimes, true, false));

        int duration = (int) Math.ceil((mStoredGame.getEndTime() - mStoredGame.getStartTime()) / 60000.0);
        gameInfoDiv.appendChild(
                createCellSpan(String.format(Locale.getDefault(), mContext.getString(R.string.set_duration), duration), true, false));

        cardDiv.appendChild(gameInfoDiv);
        

        Element homeSetsInfoDiv = new Element("div");
        homeSetsInfoDiv.addClass("div-grid-sets-info");

        Element homeTeamNameSpan = createCellSpan(mStoredGame.getTeamName(TeamType.HOME), true, false);
        homeTeamNameSpan.addClass("vbr-home-team");
        homeSetsInfoDiv.appendChild(homeTeamNameSpan);

        homeSetsInfoDiv.appendChild(createCellSpan(String.valueOf(mStoredGame.getSets(TeamType.HOME)), true, false));

        int homePointsTotal = 0;

        for (int setIndex = 0; setIndex < mStoredGame.getNumberOfSets(); setIndex++) {
            int points = mStoredGame.getPoints(TeamType.HOME, setIndex);
            homePointsTotal += points;
            homeSetsInfoDiv.appendChild(createSetCellAnchor(String.valueOf(points), setIndex));
        }

        homeSetsInfoDiv.appendChild(createCellSpan(String.valueOf(homePointsTotal), true, false));

        cardDiv.appendChild(homeSetsInfoDiv);

        Element guestSetsInfoDiv = new Element("div");
        guestSetsInfoDiv.addClass("div-grid-sets-info");

        Element guestTeamNameSpan = createCellSpan(mStoredGame.getTeamName(TeamType.GUEST), true, false);
        guestTeamNameSpan.addClass("vbr-guest-team");
        guestSetsInfoDiv.appendChild(guestTeamNameSpan);

        guestSetsInfoDiv.appendChild(createCellSpan(String.valueOf(mStoredGame.getSets(TeamType.GUEST)), true, false));

        int guestPointsTotal = 0;

        for (int setIndex = 0; setIndex < mStoredGame.getNumberOfSets(); setIndex++) {
            int points = mStoredGame.getPoints(TeamType.GUEST, setIndex);
            guestPointsTotal += points;
            guestSetsInfoDiv.appendChild(createSetCellAnchor(String.valueOf(points), setIndex));
        }

        guestSetsInfoDiv.appendChild(createCellSpan(String.valueOf(guestPointsTotal), true, false));

        cardDiv.appendChild(guestSetsInfoDiv);

        return cardDiv;
    }

    private Element createStoredTeams() {
        Element cardDiv = new Element("div");
        cardDiv.addClass("div-card").addClass("spacing-before");

        cardDiv.appendChild(createTitleDiv(mContext.getString(R.string.players)));

        Element teamsDiv = new Element("div");
        teamsDiv.addClass("div-grid-h-g");
        teamsDiv.appendChild(createTeamDiv(TeamType.HOME)).appendChild(createSpacingDiv()).appendChild(createTeamDiv(TeamType.GUEST));
        cardDiv.appendChild(teamsDiv);

        return cardDiv;
    }

    private Element createTeamDiv(TeamType teamType) {
        Element teamDiv = new Element("div");
        teamDiv.addClass("div-grid-team");

        for (PlayerDto player : mStoredGame.getPlayers(teamType)) {
            teamDiv.appendChild(createPlayerSpan(teamType, player.getNum(), mStoredGame.isLibero(teamType, player.getNum())));
            teamDiv.appendChild(createCellSpan(player.getName(), false, false));
        }

        return teamDiv;
    }

    private Element createTitleDiv(String title) {
        Element titleDiv = new Element("div");
        titleDiv.addClass("div-title");
        titleDiv.appendChild(createCellSpan(title, false, false));
        return titleDiv;
    }

    private Element createSetCellAnchor(String text, int setIndex) {
        Element anchor = new Element("a");
        anchor.addClass("bordered-cell").addClass("set-anchor");
        anchor.attr("href", String.format(Locale.getDefault(), "#div-set-%d", (1 + setIndex)));
        anchor.appendText(text);
        return anchor;
    }

    private Element createCellSpan(String text, boolean withBorder, boolean isBadge) {
        Element span = new Element("span");
        span.addClass(isBadge ? "badge" : (withBorder ? "bordered-cell" : "cell"));
        span.appendText(text);
        return span;
    }

    private Element createPlayerSpan(TeamType teamType, int player, boolean isLibero) {
        String playerStr = String.valueOf(player);

        if (player < 0 || player == SanctionDto.TEAM) {
            playerStr = "-";
        } else if (player == SanctionDto.COACH) {
            playerStr = mContext.getString(R.string.coach_abbreviation);
        }

        Element playerSpan = createCellSpan(playerStr, false, true);

        if (isLibero) {
            playerSpan.addClass(TeamType.HOME.equals(teamType) ? "vbr-home-libero" : "vbr-guest-libero");
        } else {
            playerSpan.addClass(TeamType.HOME.equals(teamType) ? "vbr-home-team" : "vbr-guest-team");
        }

        if (mStoredGame.getCaptain(teamType) == player) {
            playerSpan.addClass("vbr-captain");
        }

        return playerSpan;
    }

    private Element createEmptyPlayerSpan(TeamType teamType) {
        Element playerSpan = createCellSpan("-", false, true);
        playerSpan.addClass(TeamType.HOME.equals(teamType) ? "vbr-home-team" : "vbr-guest-team");
        return playerSpan;
    }

    private Element createStoredSetHeader(int setIndex) {
        Element setHeaderDiv = new Element("div");
        setHeaderDiv.addClass("div-flex-row");

        Element setInfoDiv = new Element("div");
        setInfoDiv.addClass("div-grid-set-header-info");

        Element indexSpan = createCellSpan(String.format(Locale.getDefault(), mContext.getString(R.string.set_number), (setIndex + 1)),
                                           true, false);
        indexSpan
                .addClass("set-index-cell")
                .addClass((mStoredGame.getPoints(TeamType.HOME, setIndex) > mStoredGame.getPoints(TeamType.GUEST,
                                                                                                  setIndex)) ? "vbr-home-team" : "vbr-guest-team");
        setInfoDiv.appendChild(indexSpan);

        setInfoDiv.appendChild(createCellSpan(String.valueOf(mStoredGame.getPoints(TeamType.HOME, setIndex)), true, false));
        setInfoDiv.appendChild(createCellSpan(String.valueOf(mStoredGame.getPoints(TeamType.GUEST, setIndex)), true, false));

        setHeaderDiv.appendChild(setInfoDiv);

        setHeaderDiv.appendChild(createSpacingDiv());
        setHeaderDiv.appendChild(createSpacingDiv());

        Element setTimeDiv = new Element("div");
        setTimeDiv.addClass("div-grid-set-header-time");

        String startEndTimes = String.format("%s \u2192 %s", mTimeFormatter.format(new Date(mStoredGame.getSetStartTime(setIndex))),
                                             mTimeFormatter.format(new Date(mStoredGame.getSetEndTime(setIndex))));
        int duration = (int) Math.ceil(mStoredGame.getSetDuration(setIndex) / 60000.0);

        setTimeDiv.appendChild(createCellSpan(startEndTimes, true, false));
        setTimeDiv.appendChild(
                createCellSpan(String.format(Locale.getDefault(), mContext.getString(R.string.set_duration), duration), true, false));

        setHeaderDiv.appendChild(setTimeDiv);

        setHeaderDiv.appendChild(createSpacingDiv());
        setHeaderDiv.appendChild(createSpacingDiv());

        if (mStoredGame.getRules().isSanctions()) {
            setHeaderDiv.appendChild(createStoredSanctions(setIndex));
        }

        return setHeaderDiv;
    }

    private Element createStoredStartingLineup(int setIndex) {
        Element wrapperDiv = new Element("div");

        wrapperDiv.appendChild(createTitleDiv(mContext.getString(R.string.confirm_lineup_title)).addClass("spacing-before"));

        Element lineupsDiv = new Element("div");
        lineupsDiv.addClass("div-grid-h-g");
        lineupsDiv
                .appendChild(createLineupDiv(TeamType.HOME, setIndex))
                .appendChild(createEmptyDiv())
                .appendChild(createLineupDiv(TeamType.GUEST, setIndex));
        wrapperDiv.appendChild(lineupsDiv);

        return wrapperDiv;
    }

    private Element createLineupDiv(TeamType teamType, int setIndex) {
        Element lineupDiv = new Element("div");
        lineupDiv.addClass("div-grid-lineup").addClass("border");

        if (mStoredGame.isStartingLineupConfirmed(teamType, setIndex)) {
            lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_4_title), false, false));
            lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_3_title), false, false));
            lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_2_title), false, false));

            lineupDiv.appendChild(
                    createPlayerSpan(teamType, mStoredGame.getPlayerAtPositionInStartingLineup(teamType, PositionType.POSITION_4, setIndex),
                                     false));
            lineupDiv.appendChild(
                    createPlayerSpan(teamType, mStoredGame.getPlayerAtPositionInStartingLineup(teamType, PositionType.POSITION_3, setIndex),
                                     false));
            lineupDiv.appendChild(
                    createPlayerSpan(teamType, mStoredGame.getPlayerAtPositionInStartingLineup(teamType, PositionType.POSITION_2, setIndex),
                                     false));

            lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_5_title), false, false));
            lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_6_title), false, false));
            lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_1_title), false, false));

            lineupDiv.appendChild(
                    createPlayerSpan(teamType, mStoredGame.getPlayerAtPositionInStartingLineup(teamType, PositionType.POSITION_5, setIndex),
                                     false));
            lineupDiv.appendChild(
                    createPlayerSpan(teamType, mStoredGame.getPlayerAtPositionInStartingLineup(teamType, PositionType.POSITION_6, setIndex),
                                     false));
            lineupDiv.appendChild(
                    createPlayerSpan(teamType, mStoredGame.getPlayerAtPositionInStartingLineup(teamType, PositionType.POSITION_1, setIndex),
                                     false));
        } else {
            lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_4_title), false, false));
            lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_3_title), false, false));
            lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_2_title), false, false));

            lineupDiv.appendChild(createEmptyPlayerSpan(teamType));
            lineupDiv.appendChild(createEmptyPlayerSpan(teamType));
            lineupDiv.appendChild(createEmptyPlayerSpan(teamType));

            lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_5_title), false, false));
            lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_6_title), false, false));
            lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_1_title), false, false));

            lineupDiv.appendChild(createEmptyPlayerSpan(teamType));
            lineupDiv.appendChild(createEmptyPlayerSpan(teamType));
            lineupDiv.appendChild(createEmptyPlayerSpan(teamType));
        }

        return lineupDiv;
    }

    private Element createStoredSubstitutions(int setIndex) {
        Element wrapperDiv = new Element("div");

        wrapperDiv.appendChild(createTitleDiv(mContext.getString(R.string.substitutions_tab)).addClass("spacing-before"));

        Element substitutionsDiv = new Element("div");
        substitutionsDiv.addClass("div-grid-h-g");

        substitutionsDiv
                .appendChild(createSubstitutionsDiv(TeamType.HOME, setIndex))
                .appendChild(createEmptyDiv())
                .appendChild(createSubstitutionsDiv(TeamType.GUEST, setIndex));

        wrapperDiv.appendChild(substitutionsDiv);

        return wrapperDiv;
    }

    private Element createSubstitutionsDiv(TeamType teamType, int setIndex) {
        Element substitutionsDiv = new Element("div");
        substitutionsDiv.addClass("div-flex-column");

        for (SubstitutionDto substitution : mStoredGame.getSubstitutions(teamType, setIndex)) {
            substitutionsDiv.appendChild(createSubstitutionDiv(teamType, substitution));
        }

        return substitutionsDiv;
    }

    private Element createSubstitutionDiv(TeamType teamType, SubstitutionDto substitution) {
        Element substitutionDiv = new Element("div");
        substitutionDiv.addClass("div-grid-substitution");

        String score = String.format(Locale.getDefault(), "%d-%d",
                                     TeamType.HOME.equals(teamType) ? substitution.getHomePoints() : substitution.getGuestPoints(),
                                     TeamType.HOME.equals(teamType) ? substitution.getGuestPoints() : substitution.getHomePoints());

        substitutionDiv.appendChild(createPlayerSpan(teamType, substitution.getPlayerIn(), false));
        substitutionDiv.appendChild(new Element("div").addClass("substitution-image"));
        substitutionDiv.appendChild(createPlayerSpan(teamType, substitution.getPlayerOut(), false));
        substitutionDiv.appendChild(createCellSpan(score, false, false));

        return substitutionDiv;
    }

    private Element createStoredTimeouts(int setIndex) {
        Element wrapperDiv = new Element("div");

        wrapperDiv.appendChild(createTitleDiv(mContext.getString(R.string.timeouts_tab)).addClass("spacing-before"));

        Element timeoutsDiv = new Element("div");
        timeoutsDiv.addClass("div-grid-h-g");
        timeoutsDiv
                .appendChild(createTimeoutsDiv(TeamType.HOME, setIndex))
                .appendChild(createEmptyDiv())
                .appendChild(createTimeoutsDiv(TeamType.GUEST, setIndex));

        wrapperDiv.appendChild(timeoutsDiv);

        return wrapperDiv;
    }

    private Element createTimeoutsDiv(TeamType teamType, int setIndex) {
        Element timeoutsDiv = new Element("div");
        timeoutsDiv.addClass("div-flex-column");

        for (TimeoutDto timeout : mStoredGame.getCalledTimeouts(teamType, setIndex)) {
            timeoutsDiv.appendChild(createTimeoutDiv(teamType, timeout));
        }

        return timeoutsDiv;
    }

    private Element createTimeoutDiv(TeamType teamType, TimeoutDto timeout) {
        Element timeoutDiv = new Element("div");
        timeoutDiv.addClass("div-grid-timeout");

        String score = String.format(Locale.getDefault(), "%d-%d",
                                     TeamType.HOME.equals(teamType) ? timeout.getHomePoints() : timeout.getGuestPoints(),
                                     TeamType.HOME.equals(teamType) ? timeout.getGuestPoints() : timeout.getHomePoints());

        timeoutDiv.appendChild(createPlayerSpan(teamType, -1, false).addClass(getTimeoutImageClass(mStoredGame.getTeamColor(teamType))));
        timeoutDiv.appendChild(createCellSpan(score, false, false));

        return timeoutDiv;
    }

    private String getTimeoutImageClass(int backgroundColor) {
        String imageClass;

        double a = 1 - (0.299 * Color.red(backgroundColor) + 0.587 * Color.green(backgroundColor) + 0.114 * Color.blue(
                backgroundColor)) / 255;

        if (a < 0.5) {
            imageClass = "timeout-gray-image";
        } else {
            imageClass = "timeout-white-image";
        }

        return imageClass;
    }

    private Element createStoredSanctions(int setIndex) {
        Element wrapperDiv = new Element("div");

        wrapperDiv.appendChild(createTitleDiv(mContext.getString(R.string.sanctions_tab)));

        Element sanctionsDiv = new Element("div");
        sanctionsDiv.addClass("div-grid-h-g");
        sanctionsDiv
                .appendChild(createSanctionsDiv(TeamType.HOME, setIndex))
                .appendChild(createEmptyDiv())
                .appendChild(createSanctionsDiv(TeamType.GUEST, setIndex));

        wrapperDiv.appendChild(sanctionsDiv);

        return wrapperDiv;
    }

    private Element createSanctionsDiv(TeamType teamType, int setIndex) {
        Element sanctionsDiv = new Element("div");
        sanctionsDiv.addClass("div-flex-column");

        for (SanctionDto sanction : mStoredGame.getAllSanctions(teamType, setIndex)) {
            sanctionsDiv.appendChild(createSanctionDiv(teamType, sanction));
        }

        return sanctionsDiv;
    }

    private Element createSanctionDiv(TeamType teamType, SanctionDto sanction) {
        org.jsoup.nodes.Element sanctionDiv = new org.jsoup.nodes.Element("div");
        sanctionDiv.addClass("div-grid-sanction");

        int player = sanction.getNum();

        // Icono / clase de la sanción y jugador implicado
        sanctionDiv.appendChild(new org.jsoup.nodes.Element("div").addClass(getSanctionImageClass(sanction.getCard())));
        sanctionDiv.appendChild(createPlayerSpan(teamType, player, mStoredGame.isLibero(teamType, player)));

        // Reconstrucción segura del marcador
        String score = String.format(
                java.util.Locale.getDefault(),
                "%d-%d",
                (com.tonkar.volleyballreferee.engine.team.TeamType.HOME.equals(teamType) ? sanction.getHomePoints() : sanction.getGuestPoints()),
                (com.tonkar.volleyballreferee.engine.team.TeamType.HOME.equals(teamType) ? sanction.getGuestPoints() : sanction.getHomePoints())
        );

        // Badge textual para la familia de demora
        String delayLabel = null;
        try {
            if (sanction.getCard().isDelaySanctionType()) {
                if (sanction.isImproperRequest()) {
                    delayLabel = "IR";
                } else if (sanction.getCard() == com.tonkar.volleyballreferee.engine.game.sanction.SanctionType.DELAY_WARNING) {
                    delayLabel = "Delay Warning";
                } else if (sanction.getCard() == com.tonkar.volleyballreferee.engine.game.sanction.SanctionType.DELAY_PENALTY) {
                    delayLabel = "Delay Penalty";
                }
            }
        } catch (Throwable ignored) { }

        if (delayLabel != null && (score == null || score.isEmpty())) {
            score = delayLabel;
        } else if (delayLabel != null) {
            score = delayLabel + " · " + score;
        }

        sanctionDiv.appendChild(createCellSpan(score, false, false));
        return sanctionDiv;
}
} catch (Throwable ignored) {}
        
        // Build delay label for clarity
        try {
            if (sanction.getCard().isDelaySanctionType()) {
                if (sanction.isImproperRequest()) {
                    delayLabel = "IR";
                } else if (sanction.getCard() == com.tonkar.volleyballreferee.engine.game.sanction.SanctionType.DELAY_WARNING) {
                    delayLabel = "Delay Warning";
                } else if (sanction.getCard() == com.tonkar.volleyballreferee.engine.game.sanction.SanctionType.DELAY_PENALTY) {
                    delayLabel = "Delay Penalty";
                }
            }
        } catch (Throwable ignored) {}
        if (delayLabel != null && (score == null || score.isEmpty())) {
            score = delayLabel;
        } else if (delayLabel != null) {
            score = delayLabel + " · " + score;
        }
        /*BADGE_INSERTED*/
        
        sanctionDiv.appendChild(createCellSpan(score, false, false));

        return sanctionDiv;

        // Rebuild score safely
        score = String.format(
                java.util.Locale.getDefault(),
                "%d-%d",
                teamType == com.tonkar.volleyballreferee.engine.team.TeamType.HOME ? sanction.getHomePoints() : sanction.getGuestPoints(),
                teamType == com.tonkar.volleyballreferee.engine.team.TeamType.HOME ? sanction.getGuestPoints() : sanction.getHomePoints()
        );
        // Badge for delay family
        delayLabel = null;
        try {
            if (sanction.getCard().isDelaySanctionType()) {
                if (sanction.isImproperRequest()) {
                    delayLabel = "IR";
                } else if (sanction.getCard() == com.tonkar.volleyballreferee.engine.game.sanction.SanctionType.DELAY_WARNING) {
                    delayLabel = "Delay Warning";
                } else if (sanction.getCard() == com.tonkar.volleyballreferee.engine.game.sanction.SanctionType.DELAY_PENALTY) {
                    delayLabel = "Delay Penalty";
                }
            }
        } catch (Throwable ignored) {}
        if (delayLabel != null && (score == null || score.isEmpty())) {
            score = delayLabel;
        } else if (delayLabel != null) {
            score = delayLabel + " · " + score;
        }
        sanctionDiv.appendChild(createCellSpan(score, false, false));
    


        // Rebuild score safely
        score = String.format(
                java.util.Locale.getDefault(),
                "%d-%d",
                teamType == com.tonkar.volleyballreferee.engine.team.TeamType.HOME ? sanction.getHomePoints() : sanction.getGuestPoints(),
                teamType == com.tonkar.volleyballreferee.engine.team.TeamType.HOME ? sanction.getGuestPoints() : sanction.getHomePoints()
        );
        // Badge for delay family
        delayLabel = null;
        try {
            if (sanction.getCard().isDelaySanctionType()) {
                if (sanction.isImproperRequest()) {
                    delayLabel = "IR";
                } else if (sanction.getCard() == com.tonkar.volleyballreferee.engine.game.sanction.SanctionType.DELAY_WARNING) {
                    delayLabel = "Delay Warning";
                } else if (sanction.getCard() == com.tonkar.volleyballreferee.engine.game.sanction.SanctionType.DELAY_PENALTY) {
                    delayLabel = "Delay Penalty";
                }
            }
        } catch (Throwable ignored) {}
        if (delayLabel != null && (score == null || score.isEmpty())) {
            score = delayLabel;
        } else if (delayLabel != null) {
            score = delayLabel + " · " + score;
        }
        sanctionDiv.appendChild(createCellSpan(score, false, false));
    }

    private String getSanctionImageClass(SanctionType sanctionType) {
        return switch (sanctionType) {
            case YELLOW -> "yellow-card-image";
            case RED -> "red-card-image";
            case RED_EXPULSION -> "expulsion-card-image";
            case RED_DISQUALIFICATION -> "disqualification-card-image";
            case DELAY_WARNING -> "delay-warning-image";
            default -> "delay-penalty-image";
        };
    }

    private Element createStoredLadder(int setIndex) {
        Element wrapperDiv = new Element("div");

        wrapperDiv.appendChild(createTitleDiv(mContext.getString(R.string.ladder_tab)).addClass("spacing-before"));

        int homeScore = 0;
        int guestScore = 0;

        Element ladderDiv = new Element("div");
        ladderDiv.addClass("div-flex-row");

        TeamType firstServingTeam = mStoredGame.getFirstServingTeam(setIndex);
        ladderDiv.appendChild(createServiceLadderItem(firstServingTeam));

        for (TeamType teamType : mStoredGame.getPointsLadder(setIndex)) {
            if (TeamType.HOME.equals(teamType)) {
                homeScore++;
                ladderDiv.appendChild(createLadderItem(teamType, homeScore));
            } else {
                guestScore++;
                ladderDiv.appendChild(createLadderItem(teamType, guestScore));
            }
        }

        wrapperDiv.appendChild(ladderDiv);

        return wrapperDiv;
    }

    private Element createLadderItem(TeamType teamType, int score) {
        Element ladderItemDiv = new Element("div");
        ladderItemDiv.addClass("div-flex-column").addClass("ladder-spacing");

        if (TeamType.HOME.equals(teamType)) {
            ladderItemDiv.appendChild(createCellSpan(String.valueOf(score), false, true).addClass("vbr-home-team"));
            ladderItemDiv.appendChild(createCellSpan(" ", false, true));
        } else {
            ladderItemDiv.appendChild(createCellSpan(" ", false, true));
            ladderItemDiv.appendChild(createCellSpan(String.valueOf(score), false, true).addClass("vbr-guest-team"));
        }

        return ladderItemDiv;
    }

    private Element createServiceLadderItem(TeamType teamType) {
        Element ladderItemDiv = new Element("div");
        ladderItemDiv.addClass("div-flex-column").addClass("ladder-spacing");

        if (TeamType.HOME.equals(teamType)) {
            ladderItemDiv.appendChild(createCellSpan(" ", false, true)
                                              .addClass("vbr-home-team")
                                              .appendChild(new Element("span").addClass(
                                                      getServiceImageClass(mStoredGame.getTeamColor(teamType)))));
            ladderItemDiv.appendChild(createCellSpan(" ", false, true));
        } else {
            ladderItemDiv.appendChild(createCellSpan(" ", false, true));
            ladderItemDiv.appendChild(createCellSpan(" ", false, true)
                                              .addClass("vbr-guest-team")
                                              .appendChild(new Element("span").addClass(
                                                      getServiceImageClass(mStoredGame.getTeamColor(teamType)))));
        }

        return ladderItemDiv;
    }

    private String getServiceImageClass(int backgroundColor) {
        String imageClass;

        double a = 1 - (0.299 * Color.red(backgroundColor) + 0.587 * Color.green(backgroundColor) + 0.114 * Color.blue(
                backgroundColor)) / 255;

        if (a < 0.5) {
            imageClass = "service-gray-image";
        } else {
            imageClass = "service-white-image";
        }

        return imageClass;
    }

    private Element createRemarks() {
        Element cardDiv = new Element("div");
        cardDiv.addClass("div-card").addClass("spacing-before");

        cardDiv.appendChild(createTitleDiv(mContext.getString(R.string.remarks)));

        Element remarksDiv = new Element("div");
        remarksDiv.addClass("remarks-cell").addClass("spacing-before");

        for (String line : mRemarks.split("\\n")) {
            remarksDiv.appendText(line).appendElement("br");
        }

        cardDiv.appendChild(remarksDiv);

        return cardDiv;
    }

    private Element createSignatures() {
        Element cardDiv = new Element("div");
        cardDiv.addClass("div-card").addClass("spacing-before");

        cardDiv.appendChild(createTitleDiv(mContext.getString(R.string.signatures)));

        Element refereeSignaturesDiv = new Element("div");
        refereeSignaturesDiv.addClass("div-grid-1-2-3").addClass("spacing-before");
        refereeSignaturesDiv
                .appendChild(createRefereeSignatureDiv(1))
                .appendChild(createSpacingDiv())
                .appendChild(createRefereeSignatureDiv(2))
                .appendChild(createEmptyDiv())
                .appendChild(createScorerSignatureDiv());
        cardDiv.appendChild(refereeSignaturesDiv);

        Element captainSignaturesDiv = new Element("div");
        captainSignaturesDiv.addClass("div-grid-1-2-3").addClass("spacing-before");
        captainSignaturesDiv
                .appendChild(createCaptainSignatureDiv(TeamType.HOME))
                .appendChild(createSpacingDiv())
                .appendChild(createCaptainSignatureDiv(TeamType.GUEST));
        cardDiv.appendChild(captainSignaturesDiv);

        if (GameType.INDOOR.equals(mStoredGame.getKind()) || GameType.INDOOR_4X4.equals(mStoredGame.getKind())) {
            Element coachSignaturesDiv = new Element("div");
            coachSignaturesDiv.addClass("div-grid-1-2-3").addClass("spacing-before");
            coachSignaturesDiv
                    .appendChild(createCoachSignaturesDiv(TeamType.HOME))
                    .appendChild(createSpacingDiv())
                    .appendChild(createCoachSignaturesDiv(TeamType.GUEST));
            cardDiv.appendChild(coachSignaturesDiv);
        }

        return cardDiv;
    }

    private Element createRefereeSignatureDiv(int number) {
        Element signatureDiv = new Element("div");
        signatureDiv.addClass("div-grid-signature");

        signatureDiv.appendChild(
                createSignatureTitleBox(String.format(Locale.getDefault(), "%s %d", mContext.getString(R.string.referee), number)));
        signatureDiv.appendChild(createSignatureNameBox(number == 1 ? mReferee1Name : mReferee2Name));
        signatureDiv.appendChild(createSignatureBoxWithImage(number == 1 ? mReferee1Signature : mReferee2Signature));

        return signatureDiv;
    }

    private Element createScorerSignatureDiv() {
        Element signatureDiv = new Element("div");
        signatureDiv.addClass("div-grid-signature");

        signatureDiv.appendChild(createSignatureTitleBox(mContext.getString(R.string.scorer)));
        signatureDiv.appendChild(createSignatureNameBox(mScorerName));
        signatureDiv.appendChild(createSignatureBoxWithImage(mScorerSignature));

        return signatureDiv;
    }

    private Element createCaptainSignatureDiv(TeamType teamType) {
        Element signatureDiv = new Element("div");
        signatureDiv.addClass("div-grid-signature");

        if (TeamType.HOME.equals(teamType)) {
            signatureDiv.appendChild(createSignatureTitleBox(mContext.getString(R.string.captain)).addClass("vbr-home-team"));
            signatureDiv.appendChild(createSignatureNameBox(mHomeCaptainName));
            signatureDiv.appendChild(createSignatureBoxWithImage(mHomeCaptainSignature));
        } else {
            signatureDiv.appendChild(createSignatureTitleBox(mContext.getString(R.string.captain)).addClass("vbr-guest-team"));
            signatureDiv.appendChild(createSignatureNameBox(mGuestCaptainName));
            signatureDiv.appendChild(createSignatureBoxWithImage(mGuestCaptainSignature));
        }

        return signatureDiv;
    }

    private Element createCoachSignaturesDiv(TeamType teamType) {
        Element signatureDiv = new Element("div");
        signatureDiv.addClass("div-grid-signature");

        if (TeamType.HOME.equals(teamType)) {
            signatureDiv.appendChild(createSignatureTitleBox(mContext.getString(R.string.coach)).addClass("vbr-home-team"));
            signatureDiv.appendChild(createSignatureNameBox(mHomeCoachName));
            signatureDiv.appendChild(createSignatureBoxWithImage(mHomeCoachSignature));
        } else {
            signatureDiv.appendChild(createSignatureTitleBox(mContext.getString(R.string.coach)).addClass("vbr-guest-team"));
            signatureDiv.appendChild(createSignatureNameBox(mGuestCoachName));
            signatureDiv.appendChild(createSignatureBoxWithImage(mGuestCoachSignature));
        }

        return signatureDiv;
    }

    private Element createSignatureTitleBox(String text) {
        Element div = new Element("div");
        div.addClass("signature-title-cell");
        div.appendText(text);
        return div;
    }

    private Element createSignatureNameBox(String text) {
        if (text == null || text.isEmpty()) {
            text = "";
        }

        Element div = new Element("div");
        div.addClass("signature-name-cell");
        div.appendText(text);
        return div;
    }

    private Element createEmptySignatureBox() {
        Element div = new Element("div");
        div.addClass("signature-cell");
        div.appendText(" ");
        return div;
    }

    private Element createSignatureBoxWithImage(String base64Image) {
        Element div;

        if (base64Image == null) {
            div = createEmptySignatureBox();
        } else {
            Element img = new Element("img");
            img.addClass("signature-image");
            img.attr("src", String.format("data:image/png;base64,%s", base64Image));

            div = new Element("div");
            div.addClass("signature-cell");
            div.appendChild(img);
        }

        return div;
    }

    private Element createLogoBox(String base64Image) {
        Element img = new Element("img");
        img.addClass("logo-image");
        img.attr("src", String.format("data:image/jpeg;base64,%s", base64Image));
        return img;
    }

    private Element createFooter() {
        Element div = new Element("div");
        div.addClass("div-footer");
        div.appendText("Powered by Volleyball Referee");

        Element vbrLogo = new Element("div");
        vbrLogo.addClass("vbr-logo-image");
        div.appendChild(vbrLogo);

        return div;
    }

    private String createStoredIndoor4x4Game() {
        mBody.appendChild(createStoredGameHeader());
        
        mBody.appendChild(createStoredTeams());

        for (int setIndex = 0; setIndex < mStoredGame.getNumberOfSets(); setIndex++) {
            Element cardDiv = new Element("div");
            cardDiv.addClass("div-card").addClass("spacing-before");
            if (setIndex % 2 == 1 || (setIndex == 0 && (mStoredGame.getPlayers(TeamType.HOME).size() > 14 || mStoredGame
                    .getPlayers(TeamType.GUEST)
                    .size() > 14)) || mStoredGame.getSubstitutions(TeamType.HOME, setIndex).size() > 6 || mStoredGame
                    .getSubstitutions(TeamType.GUEST, setIndex)
                    .size() > 6 || (mStoredGame.getPoints(TeamType.HOME, setIndex) + mStoredGame.getPoints(TeamType.GUEST,
                                                                                                           setIndex) > 64)) {
                cardDiv.addClass("new-page-for-printers");
            }
            cardDiv.attr("id", String.format(Locale.getDefault(), "div-set-%d", (1 + setIndex)));
            cardDiv.appendChild(createStoredSetHeader(setIndex));

            Element line2Div = new Element("div");
            line2Div.addClass("div-flex-row");
            line2Div
                    .appendChild(createStoredStartingLineup4x4(setIndex))
                    .appendChild(createSpacingDiv())
                    .appendChild(createSpacingDiv())
                    .appendChild(createStoredSubstitutions(setIndex));
            if (mStoredGame.getRules().isTeamTimeouts()) {
                line2Div.appendChild(createSpacingDiv()).appendChild(createSpacingDiv()).appendChild(createStoredTimeouts(setIndex));
            }
            cardDiv.appendChild(line2Div);

            cardDiv.appendChild(createStoredLadder(setIndex));

            mBody.appendChild(cardDiv);
        }

        mBody.appendChild(createStoredGameHeader().addClass("new-page-for-printers"));
        mBody.appendChild(createRemarks());
        mBody.appendChild(createSignatures());
        mBody.appendChild(createFooter());

        return mDocument.toString();
    }

    private Element createStoredStartingLineup4x4(int setIndex) {
        Element wrapperDiv = new Element("div");

        wrapperDiv.appendChild(createTitleDiv(mContext.getString(R.string.confirm_lineup_title)).addClass("spacing-before"));

        Element lineupsDiv = new Element("div");
        lineupsDiv.addClass("div-grid-h-g");
        lineupsDiv
                .appendChild(createLineupDiv4x4(TeamType.HOME, setIndex))
                .appendChild(createEmptyDiv())
                .appendChild(createLineupDiv4x4(TeamType.GUEST, setIndex));
        wrapperDiv.appendChild(lineupsDiv);

        return wrapperDiv;
    }

    private Element createLineupDiv4x4(TeamType teamType, int setIndex) {
        Element lineupDiv = new Element("div");
        lineupDiv.addClass("div-grid-lineup").addClass("border");

        if (mStoredGame.isStartingLineupConfirmed(teamType, setIndex)) {
            lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_4_title), false, false));
            lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_3_title), false, false));
            lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_2_title), false, false));

            lineupDiv.appendChild(
                    createPlayerSpan(teamType, mStoredGame.getPlayerAtPositionInStartingLineup(teamType, PositionType.POSITION_4, setIndex),
                                     false));
            lineupDiv.appendChild(
                    createPlayerSpan(teamType, mStoredGame.getPlayerAtPositionInStartingLineup(teamType, PositionType.POSITION_3, setIndex),
                                     false));
            lineupDiv.appendChild(
                    createPlayerSpan(teamType, mStoredGame.getPlayerAtPositionInStartingLineup(teamType, PositionType.POSITION_2, setIndex),
                                     false));

            lineupDiv.appendChild(createEmptyDiv());
            lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_1_title), false, false));
            lineupDiv.appendChild(createEmptyDiv());

            lineupDiv.appendChild(createEmptyDiv());
            lineupDiv.appendChild(
                    createPlayerSpan(teamType, mStoredGame.getPlayerAtPositionInStartingLineup(teamType, PositionType.POSITION_1, setIndex),
                                     false));
            lineupDiv.appendChild(createEmptyDiv());
        } else {
            lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_4_title), false, false));
            lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_3_title), false, false));
            lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_2_title), false, false));

            lineupDiv.appendChild(createEmptyPlayerSpan(teamType));
            lineupDiv.appendChild(createEmptyPlayerSpan(teamType));
            lineupDiv.appendChild(createEmptyPlayerSpan(teamType));

            lineupDiv.appendChild(createEmptyDiv());
            lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_1_title), false, false));
            lineupDiv.appendChild(createEmptyDiv());

            lineupDiv.appendChild(createEmptyDiv());
            lineupDiv.appendChild(createEmptyPlayerSpan(teamType));
            lineupDiv.appendChild(createEmptyDiv());
        }

        return lineupDiv;
    }

    private String createStoredBeachGame() {
        mBody.appendChild(createStoredGameHeader());
        mBody.appendChild(createStoredTeams());

        for (int setIndex = 0; setIndex < mStoredGame.getNumberOfSets(); setIndex++) {
            Element cardDiv = new Element("div");
            cardDiv.addClass("div-card").addClass("spacing-before");
            if (mStoredGame.getNumberOfSets() > 2 && setIndex % 2 == 1) {
                cardDiv.addClass("new-page-for-printers");
            }
            cardDiv.attr("id", String.format(Locale.getDefault(), "div-set-%d", (1 + setIndex)));
            cardDiv.appendChild(createStoredSetHeader(setIndex));

            if (mStoredGame.getRules().isTeamTimeouts()) {
                Element timeoutDiv = new Element("div");
                timeoutDiv.addClass("div-flex-row");
                timeoutDiv.appendChild(createStoredTimeouts(setIndex));
                cardDiv.appendChild(timeoutDiv);
            }

            cardDiv.appendChild(createStoredLadder(setIndex));

            mBody.appendChild(cardDiv);
        }

        mBody.appendChild(createStoredGameHeader().addClass("new-page-for-printers"));
        mBody.appendChild(createRemarks());
        mBody.appendChild(createSignatures());
        mBody.appendChild(createFooter());

        return mDocument.toString();
    }

    private String createStoredSnowGame() {
        mBody.appendChild(createStoredGameHeader());
        mBody.appendChild(createStoredTeams());

        for (int setIndex = 0; setIndex < mStoredGame.getNumberOfSets(); setIndex++) {
            Element cardDiv = new Element("div");
            cardDiv.addClass("div-card").addClass("spacing-before");
            if (mStoredGame.getNumberOfSets() > 2 && setIndex % 2 == 1) {
                cardDiv.addClass("new-page-for-printers");
            }
            cardDiv.attr("id", String.format(Locale.getDefault(), "div-set-%d", (1 + setIndex)));
            cardDiv.appendChild(createStoredSetHeader(setIndex));

            Element line2Div = new Element("div");
            line2Div.addClass("div-flex-row");
            line2Div
                    .appendChild(createStoredStartingLineupSnow(setIndex))
                    .appendChild(createSpacingDiv())
                    .appendChild(createSpacingDiv())
                    .appendChild(createStoredSubstitutions(setIndex));
            if (mStoredGame.getRules().isTeamTimeouts()) {
                line2Div.appendChild(createSpacingDiv()).appendChild(createSpacingDiv()).appendChild(createStoredTimeouts(setIndex));
            }
            cardDiv.appendChild(line2Div);

            cardDiv.appendChild(createStoredLadder(setIndex));

            mBody.appendChild(cardDiv);
        }

        mBody.appendChild(createStoredGameHeader().addClass("new-page-for-printers"));
        mBody.appendChild(createRemarks());
        mBody.appendChild(createSignatures());
        mBody.appendChild(createFooter());

        return mDocument.toString();
    }

    private Element createStoredStartingLineupSnow(int setIndex) {
        Element wrapperDiv = new Element("div");

        wrapperDiv.appendChild(createTitleDiv(mContext.getString(R.string.confirm_lineup_title)).addClass("spacing-before"));

        Element lineupsDiv = new Element("div");
        lineupsDiv.addClass("div-grid-h-g");
        lineupsDiv
                .appendChild(createLineupDivSnow(TeamType.HOME, setIndex))
                .appendChild(createEmptyDiv())
                .appendChild(createLineupDivSnow(TeamType.GUEST, setIndex));
        wrapperDiv.appendChild(lineupsDiv);

        return wrapperDiv;
    }

    private Element createLineupDivSnow(TeamType teamType, int setIndex) {
        Element lineupDiv = new Element("div");
        lineupDiv.addClass("div-grid-lineup").addClass("border");

        lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_1_title), false, false));
        lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_2_title), false, false));
        lineupDiv.appendChild(createCellSpan(mContext.getString(R.string.position_3_title), false, false));

        if (mStoredGame.isStartingLineupConfirmed(teamType, setIndex)) {
            lineupDiv.appendChild(
                    createPlayerSpan(teamType, mStoredGame.getPlayerAtPositionInStartingLineup(teamType, PositionType.POSITION_1, setIndex),
                                     false));
            lineupDiv.appendChild(
                    createPlayerSpan(teamType, mStoredGame.getPlayerAtPositionInStartingLineup(teamType, PositionType.POSITION_2, setIndex),
                                     false));
            lineupDiv.appendChild(
                    createPlayerSpan(teamType, mStoredGame.getPlayerAtPositionInStartingLineup(teamType, PositionType.POSITION_3, setIndex),
                                     false));
        } else {
            lineupDiv.appendChild(createEmptyPlayerSpan(teamType));
            lineupDiv.appendChild(createEmptyPlayerSpan(teamType));
            lineupDiv.appendChild(createEmptyPlayerSpan(teamType));
        }

        return lineupDiv;
    }

    private Element createSpacingDiv() {
        Element div = new Element("div");
        div.addClass("horizontal-spacing");
        return div;
    }

    private Element createEmptyDiv() {
        return new Element("div");
    }

    public void setLogo(String base64Image) {
        mLogo = base64Image;
    }

    public void setReferee1Signature(String name, String base64Image) {
        mReferee1Name = name;
        mReferee1Signature = base64Image;
    }

    public void setReferee2Signature(String name, String base64Image) {
        mReferee2Name = name;
        mReferee2Signature = base64Image;
    }

    public void setScorerSignature(String name, String base64Image) {
        mScorerName = name;
        mScorerSignature = base64Image;
    }

    public void setHomeCaptainSignature(String name, String base64Image) {
        mHomeCaptainName = name;
        mHomeCaptainSignature = base64Image;
    }

    public void setHomeCoachSignature(String name, String base64Image) {
        mHomeCoachName = name;
        mHomeCoachSignature = base64Image;
    }

    public void setGuestCaptainSignature(String name, String base64Image) {
        mGuestCaptainName = name;
        mGuestCaptainSignature = base64Image;
    }

    public void setGuestCoachSignature(String name, String base64Image) {
        mGuestCoachName = name;
        mGuestCoachSignature = base64Image;
    }

    public void setRemarks(String text) {
        mRemarks = text;
    }

    public String getRemarks() {
        return mRemarks;
    }

    private String toBase64(@DrawableRes int resource, int widthPixels, int heightPixels) {
        Drawable drawable = AppCompatResources.getDrawable(mContext, resource);
        Bitmap bitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, widthPixels, heightPixels);
        drawable.draw(canvas);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);
    }

    private String colorIntToHtml(int color) {
        return String.format("#%06X", (0xFFFFFF & color)).toLowerCase();
    }
    private Element createLicencesDiv() {
        org.jsoup.nodes.Element row = new org.jsoup.nodes.Element("div");
        row.attr("style", "display:flex;gap:8px;background:#eee;padding:8px;border:1px solid #ccc;border-radius:6px;margin-top:8px;");

        String ref1 = (mReferee1License != null && !mReferee1License.trim().isEmpty()) ? mReferee1License : "—";
        String ref2 = (mReferee2License != null && !mReferee2License.trim().isEmpty()) ? mReferee2License : "—";
        String scr  = (mScorerLicense  != null && !mScorerLicense.trim().isEmpty())  ? mScorerLicense  : "—";

        org.jsoup.nodes.Element b1 = new org.jsoup.nodes.Element("div");
        b1.attr("style", "flex:1;background:#eee;border:1px dashed #bbb;border-radius:6px;min-height:72px;padding:8px;");
        b1.appendElement("div").attr("style","font-size:11px;color:#555;margin-bottom:4px;font-weight:600;")
          .text(mContext.getString(R.string.licence_referee_1));
        b1.appendElement("div").attr("style","font-size:13px;color:#222;word-break:break-word;")
          .text(ref1);
        row.appendChild(b1);

        org.jsoup.nodes.Element b2 = new org.jsoup.nodes.Element("div");
        b2.attr("style", "flex:1;background:#eee;border:1px dashed #bbb;border-radius:6px;min-height:72px;padding:8px;");
        b2.appendElement("div").attr("style","font-size:11px;color:#555;margin-bottom:4px;font-weight:600;")
          .text(mContext.getString(R.string.licence_referee_2));
        b2.appendElement("div").attr("style","font-size:13px;color:#222;word-break:break-word;")
          .text(ref2);
        row.appendChild(b2);

        org.jsoup.nodes.Element b3 = new org.jsoup.nodes.Element("div");
        b3.attr("style", "flex:1;background:#eee;border:1px dashed #bbb;border-radius:6px;min-height:72px;padding:8px;");
        b3.appendElement("div").attr("style","font-size:11px;color:#555;margin-bottom:4px;font-weight:600;")
          .text(mContext.getString(R.string.licence_scorer));
        b3.appendElement("div").attr("style","font-size:13px;color:#222;word-break:break-word;")
          .text(scr);
        row.appendChild(b3);

        return row;
    }
    private Element createLicencesCard() {
        org.jsoup.nodes.Element card = new org.jsoup.nodes.Element("div");
        card.attr("style","margin-top:8px;margin-bottom:8px;padding:8px;border:1px solid #ccc;border-radius:6px;background:#fff;");
        // Título
        card.appendElement("div")
            .attr("style","font-weight:700;margin-bottom:6px;")
            .text(mContext.getString(R.string.licences));
        // Bloques en nuevo formato
        card.appendChild(createLicencesDiv());
        card.appendChild(createTechnicalStaffDiv());
        return card;
    }
    private String getIrLabel(com.tonkar.volleyballreferee.engine.api.model.SanctionDto s) {
        try {
            java.lang.reflect.Method getter = s.getClass().getMethod("isImproperRequest");
            Object v = getter.invoke(s);
            if (v instanceof Boolean && ((Boolean) v)) {
                return " IR";
            }
        } catch (Throwable ignored) {}
        return "";
    }


    // Setters tipo "builder"
    public ScoreSheetBuilder setReferee1License(String license) {
        this.mReferee1License = license; return this;
    }
    public ScoreSheetBuilder setReferee2License(String license) {
        this.mReferee2License = license; return this;
    }
    public ScoreSheetBuilder setScorerLicense(String license) {
        this.mScorerLicense = license; return this;
    }
    public ScoreSheetBuilder setHomeCoachLicense(String v)  { this.mHomeCoachLicence  = v; return this; }
    public ScoreSheetBuilder setGuestCoachLicense(String v) { this.mGuestCoachLicence = v; return this; }
    public ScoreSheetBuilder setHomeStaffLicense(String v)  { this.mHomeStaffLicence  = v; return this; }
    public ScoreSheetBuilder setGuestStaffLicense(String v) { this.mGuestStaffLicence = v; return this; }

    // Utilidad para nulls
    private static String safeText(String s) { return s == null ? "" : s; }

    private String htmlSkeleton(String title) {
        int homeColor  = mStoredGame.getTeamColor(TeamType.HOME);
        int guestColor = mStoredGame.getTeamColor(TeamType.GUEST);
        if (homeColor == guestColor) {
            guestColor = ContextCompat.getColor(mContext, R.color.colorReportDuplicate);
        }
    
        String homeTeamBackgroundColor     = colorIntToHtml(homeColor);
        String homeTeamColor               = colorIntToHtml(UiUtils.getTextColor(mContext, homeColor));
        String homeLiberoBackgroundColor   = colorIntToHtml(mStoredGame.getLiberoColor(TeamType.HOME));
        String homeLiberoColor             = colorIntToHtml(UiUtils.getTextColor(mContext, mStoredGame.getLiberoColor(TeamType.HOME)));
    
        String guestTeamBackgroundColor    = colorIntToHtml(guestColor);
        String guestTeamColor              = colorIntToHtml(UiUtils.getTextColor(mContext, guestColor));
        String guestLiberoBackgroundColor  = colorIntToHtml(mStoredGame.getLiberoColor(TeamType.GUEST));
        String guestLiberoColor            = colorIntToHtml(
                UiUtils.getTextColor(mContext, mStoredGame.getLiberoColor(TeamType.GUEST)));
    
        return "<!doctype html>\n"
            + "<html>\n"
            + "  <head>\n"
            + "    <meta charset=\"utf-8\">\n"
            + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
            + "    <title>" + title + "</title>\n"
            + "    <style>\n"
            + "      html * { font-family: Roboto, Arial, sans-serif; font-size: 12px !important; }\n"
            + "      .vbr-body { color: " + colorIntToHtml(ContextCompat.getColor(mContext, R.color.colorOnScoreSheetBackground)) + "; "
            + "                 width: 28cm; max-width: 28cm; margin-left:auto; margin-right:auto; }\n"
            + "      .vbr-captain { text-decoration: underline; }\n"
            + "      .vbr-home-team { color:" + homeTeamColor + "; background-color:" + homeTeamBackgroundColor + "; }\n"
            + "      .vbr-home-libero { color:" + homeLiberoColor + "; background-color:" + homeLiberoBackgroundColor + "; }\n"
            + "      .vbr-guest-team { color:" + guestTeamColor + "; background-color:" + guestTeamBackgroundColor + "; }\n"
            + "      .vbr-guest-libero { color:" + guestLiberoColor + "; background-color:" + guestLiberoBackgroundColor + "; }\n"
            + "      .div-card { background-color:" + colorIntToHtml(ContextCompat.getColor(mContext, R.color.colorScoreSheetBackground)) + "; "
            + "                 padding:6px; margin:6px; box-shadow:0 1px 3px rgba(0,0,0,.12), 0 1px 2px rgba(0,0,0,.24); }\n"
            + "      .div-title { font-weight:700; margin-bottom:6px; }\n"
            + "      .div-grid-h-g { display:grid; grid-template-columns:4fr 1fr 4fr; align-items:stretch; }\n"
            + "      .div-grid-1-2-3 { display:grid; grid-template-columns:4fr 1fr 4fr 1fr 4fr; align-items:stretch; }\n"
            + "      .div-grid-game-header-logo { display:grid; grid-template-columns:4fr 6fr; align-items:stretch; }\n"
            + "      .div-grid-game-header-info { display:grid; grid-template-columns:40fr 20fr 20fr 8fr 12fr; align-items:stretch; }\n"
            + "      .div-grid-sets-info { display:grid; grid-template-columns:40fr 5fr 5fr 5fr 5fr 5fr 5fr 10fr 20fr; align-items:stretch; }\n"
            + "      .div-flex-row { display:flex; flex-flow:row wrap; align-items:flex-start; }\n"
            + "      .div-flex-column { display:flex; flex-flow:column wrap; align-items:flex-start; }\n"
            + "      .div-grid-team { display:grid; grid-template-columns:1fr 8fr 1fr 8fr; align-items:center; }\n"
            + "      .div-grid-set-header-info { min-width:175px; display:grid; grid-template-columns:3fr 1fr; }\n"
            + "      .div-grid-set-header-time { min-width:250px; display:grid; grid-template-columns:7fr 3fr; }\n"
            + "      .set-index-cell { grid-row:1 / span 2; line-height:44px; }\n"
            + "      .div-grid-lineup { display:grid; grid-template-columns:1fr 1fr 1fr; align-items:center; }\n"
            + "      .div-grid-substitution { display:grid; grid-template-columns:24fr 16fr 24fr 34fr; align-items:center; }\n"
            + "      .div-grid-timeout { display:grid; grid-template-columns:1fr 2fr; align-items:center; }\n"
            + "      .div-grid-sanction { display:grid; grid-template-columns:3fr 2fr 4fr; align-items:center; }\n"
            + "      .div-grid-signature { display:grid; grid-template-columns:2fr 5fr; align-items:stretch; }\n"
            + "      .div-footer { font-size:10px; position:fixed; display:flex; align-items:center; bottom:12px; right:12px; }\n"
            + "      .cell { min-width:22px; text-align:center; padding:3px; }\n"
            + "      .bordered-cell { border:1px solid " + colorIntToHtml(ContextCompat.getColor(mContext, R.color.colorOnScoreSheetBackground)) + "; "
            + "                      min-width:22px; text-align:center; padding:3px; margin-right:-1px; margin-left:-1px; }\n"
            + "      .remarks-cell { border:1px solid " + colorIntToHtml(ContextCompat.getColor(mContext, R.color.colorOnScoreSheetBackground)) + "; "
            + "                      padding:3px; min-height:40px; }\n"
            + "      .signature-title-cell { border:1px solid " + colorIntToHtml(ContextCompat.getColor(mContext, R.color.colorOnScoreSheetBackground)) + "; "
            + "                             grid-row:1 / span 2; height:82px; text-align:center; padding:3px; margin:0 -1px; }\n"
            + "      .signature-name-cell { border:1px solid " + colorIntToHtml(ContextCompat.getColor(mContext, R.color.colorOnScoreSheetBackground)) + "; "
            + "                            height:14px; line-height:14px; text-align:center; padding:3px; margin:0 -1px; }\n"
            + "      .signature-cell { border:1px solid " + colorIntToHtml(ContextCompat.getColor(mContext, R.color.colorOnScoreSheetBackground)) + "; "
            + "                        height:60px; line-height:60px; text-align:center; padding:3px; margin:0 -1px; }\n"
            + "      .signature-image { width:auto; height:100%; }\n"
            + "      .logo-image { width:auto; height:60px; padding:3px; margin-left:auto; margin-right:auto; }\n"
            + "      .set-anchor { color:" + colorIntToHtml(ContextCompat.getColor(mContext, R.color.colorOnScoreSheetBackground)) + "; }\n"
            + "      .badge { min-width:22px; text-align:center; padding:3px; margin:2px; border-radius:5px; }\n"
            + "      .spacing-before { margin-top:12px; }\n"
            + "      .ladder-spacing { margin-bottom:10px; }\n"
            + "      .horizontal-spacing { min-width:34px; }\n"
            + "      .border { border:1px solid " + colorIntToHtml(ContextCompat.getColor(mContext, R.color.colorOnScoreSheetBackground)) + "; "
            + "                margin-right:-1px; margin-left:-1px; }\n"
            + "      .new-page-for-printers { break-before: page; }\n"
            + "    </style>\n"
            + "    <style type=\"text/css\" media=\"print\"> body { -webkit-print-color-adjust: exact; } </style>\n"
            + "  </head>\n"
            + "  <body class=\"vbr-body\"></body>\n"
            + "</html>\n";
    }


    
    private org.jsoup.nodes.Element createTechnicalStaffDiv() {
        org.jsoup.nodes.Element block = new org.jsoup.nodes.Element("div");
        block.attr("style", "margin-top:8px;padding:8px;border:1px solid #ccc;border-radius:6px;background:#f6f6f6;");
        block.appendElement("div").attr("style","font-weight:700;margin-bottom:6px;").text(mContext.getString(R.string.technical_staff));

        org.jsoup.nodes.Element row = new org.jsoup.nodes.Element("div");
        row.attr("style", "display:flex;gap:8px;flex-wrap:wrap;");

        String acHome  = (mHomeCoachLicence  != null && !mHomeCoachLicence.trim().isEmpty())  ? mHomeCoachLicence  : "—";
        String acGuest = (mGuestCoachLicence != null && !mGuestCoachLicence.trim().isEmpty()) ? mGuestCoachLicence : "—";
        String stHome  = (mHomeStaffLicence  != null && !mHomeStaffLicence.trim().isEmpty())  ? mHomeStaffLicence  : "—";
        String stGuest = (mGuestStaffLicence != null && !mGuestStaffLicence.trim().isEmpty()) ? mGuestStaffLicence : "—";

        row.appendChild(makeStaffBox(mContext.getString(R.string.assistant_coach) + " (Home)",  acHome));
        row.appendChild(makeStaffBox(mContext.getString(R.string.assistant_coach) + " (Guest)", acGuest));
        row.appendChild(makeStaffBox(mContext.getString(R.string.staff) + " (Home)",  stHome));
        row.appendChild(makeStaffBox(mContext.getString(R.string.staff) + " (Guest)", stGuest));

        block.appendChild(row);
        return block;
    }

    private org.jsoup.nodes.Element makeStaffBox(String label, String value) {
        org.jsoup.nodes.Element box = new org.jsoup.nodes.Element("div");
        box.attr("style","flex:1 1 48%;border:1px dashed #bbb;border-radius:6px;background:#fff;padding:8px;min-height:64px;");
        box.appendElement("div").attr("style","font-size:11px;color:#555;margin-bottom:4px;font-weight:600;").text(label);
        box.appendElement("div").attr("style","font-size:13px;color:#222;word-break:break-word;").text(value);
        return box;
    }
    
}
