package tracker.hfad.com.bicycletracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SaveSharedPreference
{
    static final String PREF_USER_ID= "user_id";
    static final String PREF_USER_ACTIVITY_TYPE = "activity type";
    static final String PREF_USER_LANGUAGE = "language";
    static final String PREF_USER_STARTED = "is tracking started?";
    static final String PREF_USER_RECREATE = "recreate";

    static SharedPreferences getSharedPreferences(Context ctx)
    {
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public static void setUserID(Context ctx, Integer id)
    {
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        editor.putInt(PREF_USER_ID, id);
        editor.commit();
    }

    public static Integer getUserID(Context ctx)
    {
        return getSharedPreferences(ctx).getInt(PREF_USER_ID, -1);
    }

    public static void clear(Context ctx)
    {
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        editor.clear();
        editor.commit();
    }

    public static void setUserActivity(Context ctx, Integer position)
    {
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        editor.putInt(PREF_USER_ACTIVITY_TYPE, position);
        editor.commit();
    }

    public static Integer getUserActivity(Context ctx)
    {
        return getSharedPreferences(ctx).getInt(PREF_USER_ACTIVITY_TYPE, -1);
    }

    public static void setUserLanguage(Context ctx, Integer position)
    {
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        editor.putInt(PREF_USER_LANGUAGE, position);
        editor.commit();
    }

    public static Integer getUserLanguage(Context ctx)
    {
        return getSharedPreferences(ctx).getInt(PREF_USER_LANGUAGE, -1);
    }


    public static Boolean getStarted(Context ctx)
    {
        return getSharedPreferences(ctx).getBoolean(PREF_USER_STARTED, false);
    }

    public static void setStarted(Context ctx, Boolean position)
    {
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        editor.putBoolean(PREF_USER_STARTED, position);
        editor.commit();
    }

    public static Boolean getRecreated(Context ctx)
    {
        return getSharedPreferences(ctx).getBoolean(PREF_USER_RECREATE, false);
    }

    public static void setRecreated(Context ctx, Boolean position)
    {
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        editor.putBoolean(PREF_USER_RECREATE, position);
        editor.commit();
    }
}