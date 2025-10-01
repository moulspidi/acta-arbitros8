package com.tonkar.volleyballreferee.ui.prefs;

import android.content.Context;
import android.content.SharedPreferences;

public class LicencePrefs {
    private static final String K_HOME_COACH  = "lic_home_coach_";
    private static final String K_GUEST_COACH = "lic_guest_coach_";
    private static final String K_HOME_STAFF  = "lic_home_staff_";
    private static final String K_GUEST_STAFF = "lic_guest_staff_";

    private static final String FILE = "score_sheet_licences";
    private final SharedPreferences sp;

    public LicencePrefs(Context ctx) {
        sp = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private String k(String gameId, String who) { return gameId + "_" + who; }

    public void setRef1(String gameId, String v){ sp.edit().putString(k(gameId,"ref1"), v == null ? "" : v).apply(); }
    public void setRef2(String gameId, String v){ sp.edit().putString(k(gameId,"ref2"), v == null ? "" : v).apply(); }
    public void setScorer(String gameId, String v){ sp.edit().putString(k(gameId,"scorer"), v == null ? "" : v).apply(); }

    public String getRef1(String gameId){ return sp.getString(k(gameId,"ref1"), ""); }
    public String getRef2(String gameId){ return sp.getString(k(gameId,"ref2"), ""); }
    public String getScorer(String gameId){ return sp.getString(k(gameId,"scorer"), ""); }

    // --- Added persisted fields for assistant coach & staff ---
    public String getHomeCoach(String gameId){ return sp.getString(k(gameId,"homeCoach"), ""); }
    public String getGuestCoach(String gameId){ return sp.getString(k(gameId,"guestCoach"), ""); }
    public String getHomeStaff(String gameId){ return sp.getString(k(gameId,"homeStaff"), ""); }
    public String getGuestStaff(String gameId){ return sp.getString(k(gameId,"guestStaff"), ""); }

    public void setHomeCoach(String gameId, String v){ sp.edit().putString(k(gameId,"homeCoach"), v == null ? "" : v).apply(); }
    public void setGuestCoach(String gameId, String v){ sp.edit().putString(k(gameId,"guestCoach"), v == null ? "" : v).apply(); }
    public void setHomeStaff(String gameId, String v){ sp.edit().putString(k(gameId,"homeStaff"), v == null ? "" : v).apply(); }
    public void setGuestStaff(String gameId, String v){ sp.edit().putString(k(gameId,"guestStaff"), v == null ? "" : v).apply(); }
}
