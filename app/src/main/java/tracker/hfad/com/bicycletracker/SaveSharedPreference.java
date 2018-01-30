package tracker.hfad.com.bicycletracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SaveSharedPreference
{
    private static final String PREF_USER_ID = "user_id";
    private static final String PREF_USER_ACTIVITY_TYPE = "activity type";
    private static final String PREF_USER_LANGUAGE = "language";
    private static final String PREF_USER_STARTED = "is tracking started?";
    private static final String PREF_USER_RECREATE = "recreate";

    private static SharedPreferences getSharedPreferences(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    static void setUserID(Context ctx, Integer id) {
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        editor.putInt(PREF_USER_ID, id);
        editor.apply();
    }

    static Integer getUserID(Context ctx) {
        return getSharedPreferences(ctx).getInt(PREF_USER_ID, -1);
    }

    public static void clear(Context ctx) {
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        editor.clear();
        editor.apply();
    }

    static void setUserActivity(Context ctx, Integer position) {
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        editor.putInt(PREF_USER_ACTIVITY_TYPE, position);
        editor.apply();
    }

    static Integer getUserActivity(Context ctx) {
        return getSharedPreferences(ctx).getInt(PREF_USER_ACTIVITY_TYPE, -1);
    }

    static void setUserLanguage(Context ctx, Integer position) {
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        editor.putInt(PREF_USER_LANGUAGE, position);
        editor.apply();
    }

    static Integer getUserLanguage(Context ctx) {
        return getSharedPreferences(ctx).getInt(PREF_USER_LANGUAGE, -1);
    }


    static Boolean getStarted(Context ctx) {
        return getSharedPreferences(ctx).getBoolean(PREF_USER_STARTED, false);
    }

    static void setStarted(Context ctx, Boolean position) {
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        editor.putBoolean(PREF_USER_STARTED, position);
        editor.apply();
    }

    static Boolean getRecreated(Context ctx) {
        return getSharedPreferences(ctx).getBoolean(PREF_USER_RECREATE, false);
    }

    static void setRecreated(Context ctx, Boolean position) {
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        editor.putBoolean(PREF_USER_RECREATE, position);
        editor.apply();
    }
}