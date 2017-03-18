package tracker.hfad.com.bicycletracker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedList;
import java.util.List;


public class TrainingActivity extends Activity {

    private boolean connected=false, bound=false;
    private TrackerService trackerService;
    private Handler handler, databaseHandler;

    public static int numberOfSeconds, calories;
    private double distanceInMeters;

    private TrackerService.MyGeoPointList myGeoPointList;


    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            Log.v("TrainingActivity", "polacz z usluga!");
            TrackerService.TrackerBinder trackerBinder = (TrackerService.TrackerBinder) service;

            boolean recreate = SaveSharedPreference.getRecreated(getApplicationContext());

            if(recreate)
            {
                trackerService = trackerBinder.getService(myGeoPointList, numberOfSeconds, distanceInMeters);
                if(myGeoPointList != null) myGeoPointList.clear();
                SaveSharedPreference.setRecreated(getApplicationContext(), false);
            }
            else trackerService =  trackerBinder.getService();

            connected=true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            connected = false;
            Log.v("TrainingActivity", "rozlacz z usluga!");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);

        if(SaveSharedPreference.getRecreated(this))
        {
            recreateData();
        }


        final TextView training_time = (TextView) findViewById(R.id.huge_time_view);
        final TextView training_distance = (TextView) findViewById(R.id.training_distance);
        final TextView training_moment_velocity = (TextView) findViewById(R.id.training_moment_velocity);
        final TextView training_avarange_velocity = (TextView) findViewById(R.id.training_avarange_velocity);
        final TextView training_calories = (TextView) findViewById(R.id.training_calories);
        final TextView training_altitude = (TextView) findViewById(R.id.training_altitude);
        final TextView training_gps = (TextView) findViewById(R.id.training_gps);



        handler= new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {

                if(connected && bound)
                {
                    //odmierzanie czasu:

                    numberOfSeconds = trackerService.getCurrentNumberOfSeconds();
                    distanceInMeters = trackerService.getDistanceInMeters();
                    calories = trackerService.getCalories();

                    float moment_velocity = trackerService.getMomentVelocity();
                    double altitude = trackerService.getAltitudeInMeters();
                    double avarange_velocity = TrackerService.getAvarangePace(numberOfSeconds, distanceInMeters);


                    training_time.setText(generateTimeNotation(numberOfSeconds));
                    training_distance.setText(generateDistanceInKilometersNotation(distanceInMeters));
                    training_moment_velocity.setText(generateVelocityNotation(moment_velocity));
                    training_avarange_velocity.setText(generateVelocityNotation((float) avarange_velocity));
                    training_altitude.setText(generateAltitudeNotation(altitude));
                    training_calories.setText(generateCaloriesNotation(calories));

                    setGPSStatus(trackerService.isGPSFixed(), training_gps);

                }
                    handler.postDelayed(this, 1000);
            }});

        databaseHandler = new Handler();
        databaseHandler.post(new Runnable() {
            @Override
            public void run()
            {
                if(trackerService!=null) updateData();

                databaseHandler.postDelayed(this, 3*60*1000);
            }
        });

    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if(!bound)
        {
            Log.v("Training", "bind = false");
            Intent intent = new Intent(TrainingActivity.this, TrackerService.class);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
            startService(intent);
            bound = true;
        }

        if(trackerService == null) return;

        Button pause = (Button) findViewById(R.id.pause_button);

        if(trackerService.paused) {pause.setText(R.string.resume);}
        else {pause.setText(R.string.pause_button);}
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if(bound && connected) unbindService(connection);
        connected=false;
        bound=false;

    }


    public void onPauseClicked(View view)
    {
        trackerService.paused = !trackerService.paused;

        Button pause = (Button) view;

        if(trackerService.paused) {pause.setText(R.string.resume);}
        else {pause.setText(R.string.pause_button);}

    }

    public void onStopClicked(View view)
    {
        unbindService(connection);
        connected=false;
        bound=false;
        trackerService.paused = true;
        //trackerService.stopSelf();

        Intent serviceIntent = new Intent(TrainingActivity.this, TrackerService.class);
        stopService(serviceIntent);

        this.finish();

        Intent intent = new Intent(TrainingActivity.this, SummaryActivity.class);
        intent.putExtra(SummaryActivity.TIME, numberOfSeconds);
        intent.putExtra(SummaryActivity.DISTANCE, distanceInMeters);
        intent.putExtra(SummaryActivity.CALORIES, calories);

        TrackerService.MyGeoPointList myGeoPointList = trackerService.getGeoPointList();
        intent.putParcelableArrayListExtra(SummaryActivity.GEOPOINTLIST, myGeoPointList);

        startActivity(intent);
    }


    public static String generateTimeNotation(int numberOfSeconds)
    {
        int hours = numberOfSeconds/3600;
        int minutes = (numberOfSeconds%3600)/60;
        int secs = numberOfSeconds%60;

        return String.format("%d:%02d:%02d", hours, minutes,secs);
    }

    public static String generateVelocityNotation(double velocity)
    {
        if(Double.isNaN(velocity)) return "0.00 km/h";
        return String.format("%.2f km/h", velocity);
    }

    public static String generateDistanceInKilometersNotation(double distanceInMeters)
    {
        distanceInMeters/=1000;
        return String.format("%.2f km", distanceInMeters);
    }

    public static String generateAltitudeNotation(double altitudeInMeters)
    {
        String altitude = Double.toString(altitudeInMeters) + " m";
        return altitude;
    }

    public static String generateCaloriesNotation(int calories)
    {
        String cal = Integer.toString(calories) + " cal";
        return cal;
    }

    public void setGPSStatus(boolean isFixed, TextView textView)
    {
        if(textView == null) return;

        if(isFixed)
        {
            textView.setText(R.string.training_activity_GPSConnection);
            textView.setBackgroundResource(R.color.GPSconnection);
        }
        else
        {
            textView.setText(R.string.training_activity_noGPSConnection);
            textView.setBackgroundResource(R.color.noGPSconnection);
        }
    }

    private void recreateData()
    {
        try
        {
            String JSONencodedGeoPointList, encodedPolyLine;

            SQLiteOpenHelper dbHelper = new MainActivity.BicycleDatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor cursor = db.query("ACTIVITIES", new String[]{"time", "distance", "track", "geopoints"},
                    "finished = ?", new String[]{Integer.toString(0)}, null, null, null);

            if(cursor.getCount() != 1)
            {
                cursor.close();
                db.close();
                return;
            }

            cursor.moveToFirst();

            numberOfSeconds = cursor.getInt(0);
            distanceInMeters = cursor.getDouble(1);
            encodedPolyLine = cursor.getString(2);
            JSONencodedGeoPointList = cursor.getString(3);

            JSONArray jsonArray = new JSONArray(JSONencodedGeoPointList);
            List<LatLng> track = PolyUtil.decode(encodedPolyLine);
            myGeoPointList = new TrackerService.MyGeoPointList();

            for(int i=0; i<track.size(); i+=3)
            {
                TrackerService.MyGeoPoint point = new TrackerService.MyGeoPoint(jsonArray.getInt(i), jsonArray.getDouble(i+1),
                        jsonArray.getDouble(i+2), track.get(i/3));

                myGeoPointList.add(point);
            }

            cursor.close();
            db.close();

        }catch (JSONException e)
        {
            e.printStackTrace();
        }catch(SQLiteException e)
        {
            Log.v("ERROR: ", "Cannot add record to database");
        }
    }

    private void updateData()
    {
        TrackerService.MyGeoPointList list;
        list = trackerService.getGeoPointList();
        if(list == null || list.isEmpty()) return;;

        String encodedLatLngList = PolyUtil.encode(list.getLatLngList());
        String JSONEncodedGeopoints = SummaryActivity.encodeGeoPointListToJSON(list);

        LocalDatabaseTask.UpdateLocalDatabaseTask updateTask = new LocalDatabaseTask.UpdateLocalDatabaseTask(this, numberOfSeconds, distanceInMeters,
                calories, encodedLatLngList, JSONEncodedGeopoints, 0);

        updateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if(bound && connected) unbindService(connection);
        connected=false;
        bound=false;

        Log.v("TrainingActivity", "onDestroy()");
    }

}
