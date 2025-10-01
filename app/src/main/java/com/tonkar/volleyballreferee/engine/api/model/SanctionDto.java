package com.tonkar.volleyballreferee.engine.api.model;

import com.google.gson.annotations.SerializedName;
import com.tonkar.volleyballreferee.engine.game.sanction.SanctionType;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Sanction DTO (retrocompatible)
 * - Mantiene helpers estáticos e instancia: isPlayer/isCoach/isTeam
 * - Campo opcional improperRequest (JSON: "ir")
 * - Ctor 6-args (Lombok) y ctor 5-args legacy
 */
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class SanctionDto {

    private SanctionType card;
    private int num;
    private int set;
    private int homePoints;
    private int guestPoints;
    private boolean improperRequest;

    // ÚNICO ctor de 6 args (NO uses @AllArgsConstructor)
    public SanctionDto(SanctionType card, int num, int set, int homePoints, int guestPoints, boolean improperRequest) {
        this.card = card;
        this.num = num;
        this.set = set;
        this.homePoints = homePoints;
        this.guestPoints = guestPoints;
        this.improperRequest = improperRequest;
    }

    // Ctor de conveniencia (5 args) que delega al de 6
    public SanctionDto(SanctionType card, int num, int set, int homePoints, int guestPoints) {
        this(card, num, set, homePoints, guestPoints, false);
    }
}

    // ---- Helpers estáticos usados por motor y UI ----
    public static boolean isCoach(int num) { return num == COACH; }
    public static boolean isTeam(int num)  { return num == TEAM; }
    public static boolean isPlayer(int num){ return !isCoach(num) && !isTeam(num); }

    // ---- Conveniencias de instancia (sin parámetros) ----
    public boolean isCoach()  { return isCoach(this.num); }
    public boolean isTeam()   { return isTeam(this.num); }
    public boolean isPlayer() { return isPlayer(this.num); }

    public static final int COACH = 100;
    public static final int TEAM  = 200;

    public boolean isImproperRequest() { return improperRequest; }
    public void setImproperRequest(boolean improperRequest) { this.improperRequest = improperRequest; }

    public SanctionDto(SanctionType card, int num, int set, int homePoints, int guestPoints, boolean improperRequest) {
        this(card, num, set, homePoints, guestPoints);
        this.improperRequest = improperRequest;
    }
}
