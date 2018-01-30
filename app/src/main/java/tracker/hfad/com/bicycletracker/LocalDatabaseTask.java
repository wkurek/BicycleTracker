package tracker.hfad.com.bicycletracker;


import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

public class LocalDatabaseTask
{
    public static final class InsertIntoLocalDatabaseTask extends AsyncTask<Void , Void, Void>
    {
        private Context context;
        private SQLiteDatabase db;
        private ContentValues activityValues;
        private String encodedLatLngList, encodedGeoPoints;
        private int finished, seconds, calories;
        private double distance;


        InsertIntoLocalDatabaseTask(Context context, int seconds, double distance, int calories, String encodedLatLngList, String encodedGeoPoints, int finished)
        {
            this.context = context;
            this.seconds = seconds;
            this.distance = distance;
            this.calories = calories;
            this.encodedLatLngList = encodedLatLngList;
            this.encodedGeoPoints = encodedGeoPoints;
            this.finished = finished;
        }

        @Override
        protected void onPreExecute()
        {
            String actualDate = SummaryActivity.getActualDate();
            int dyscypline = SaveSharedPreference.getUserActivity(context);

            activityValues = new ContentValues();

            //data -> ContentValue

            activityValues.put("time", seconds);
            activityValues.put("distance", distance);
            activityValues.put("calories", calories);
            activityValues.put("date", actualDate);
            activityValues.put("track", encodedLatLngList);
            activityValues.put("geopoints", encodedGeoPoints);
            activityValues.put("discipline", dyscypline);
            activityValues.put("finished", finished);

        }

        @Override
        protected Void doInBackground(Void... params)
        {
            try
            {
                SQLiteOpenHelper dbHelper = new MainActivity.BicycleDatabaseHelper(context);
                db = dbHelper.getWritableDatabase();

                db.insert("ACTIVITIES", null, activityValues);
                Log.v("INSERT", "finished = 0");

                db.close();

            }catch(SQLiteException e)
            {
                Log.v("ERROR: ", "Cannot add record to database");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {}

    }

    public static final class UpdateLocalDatabaseTask extends AsyncTask<Void , Void, Void>
    {
        private Context context;
        private SQLiteDatabase db;
        private ContentValues activityValues;
        private String encodedLatLngList, encodedGeoPoints;
        private int finished, seconds, calories;
        private double distance;


        UpdateLocalDatabaseTask(Context context, int seconds, double distance, int calories, String encodedLatLngList, String encodedGeoPoints, int finished)
        {
            this.context = context;
            this.seconds = seconds;
            this.distance = distance;
            this.calories = calories;
            this.encodedLatLngList = encodedLatLngList;
            this.encodedGeoPoints = encodedGeoPoints;
            this.finished = finished;
        }

        @Override
        protected void onPreExecute()
        {
            String actualDate = SummaryActivity.getActualDate();
            int dyscypline = SaveSharedPreference.getUserActivity(context);

            activityValues = new ContentValues();

            //data -> ContentValue

            activityValues.put("time", seconds);
            activityValues.put("distance", distance);
            activityValues.put("calories", calories);
            activityValues.put("date", actualDate);
            activityValues.put("track", encodedLatLngList);
            activityValues.put("geopoints", encodedGeoPoints);
            activityValues.put("discipline", dyscypline);
            activityValues.put("finished", finished);

        }

        @Override
        protected Void doInBackground(Void... params)
        {
            try
            {
                SQLiteOpenHelper dbHelper = new MainActivity.BicycleDatabaseHelper(context);
                db = dbHelper.getWritableDatabase();

                int rows = db.update("ACTIVITIES", activityValues, "finished = ?", new String[]{Integer.toString(0)});
                if(rows != 1) throw new SQLiteException();

                db.close();

            }catch(SQLiteException e)
            {
                Log.v("ERROR: ", "Cannot add record to database");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {}

    }

    public static final class DeleteLocalDatabaseTask extends AsyncTask<Void , Void, Void>
    {
        private Context context;
        private SQLiteDatabase db;

        DeleteLocalDatabaseTask(Context context)
        {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected Void doInBackground(Void... params)
        {
            try
            {
                SQLiteOpenHelper dbHelper = new MainActivity.BicycleDatabaseHelper(context);
                db = dbHelper.getWritableDatabase();
                db.delete("ACTIVITIES", "finished = ?", new String[]{Integer.toString(0)});
                Log.v("DELETE", "finished = 0");
                db.close();

            }catch(SQLiteException e)
            {
                Log.v("ERROR: ", "Cannot remove record from database");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {}

    }

}
