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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.Locale;


public class TrainingActivity extends Activity {
    private static final String TAG = TrainingActivity.class.getSimpleName();

    private boolean connected = false, bound = false;
    private TrackerService trackerService;
    private Handler handler, databaseHandler;

    public static int numberOfSeconds, calories;
    private double distanceInMeters;

    private TrackerService.MyGeoPointList myGeoPointList;

    Button pauseButton;
    TextView timeView, distanceView, momentVelocityView, averageVelocityView,
            caloriesView, altitudeView, gpsView;


    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "Connect with service.");
            TrackerService.TrackerBinder trackerBinder = (TrackerService.TrackerBinder) service;

            //TODO: consider deleting this section
            boolean recreate = SaveSharedPreference.getRecreated(getApplicationContext());
            if(recreate) {
                trackerService = trackerBinder.getService(myGeoPointList, numberOfSeconds, distanceInMeters);
                if(myGeoPointList != null) myGeoPointList.clear();
                SaveSharedPreference.setRecreated(getApplicationContext(), false);
            } else trackerService =  trackerBinder.getService();

            connected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Service disconnected.");
            connected = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);

        if(SaveSharedPreference.getRecreated(this)) {
            recreateData();
        }

        pauseButton = (Button) findViewById(R.id.pause_button);

        timeView = (TextView) findViewById(R.id.huge_time_view);
        distanceView = (TextView) findViewById(R.id.training_distance);
        momentVelocityView = (TextView) findViewById(R.id.training_moment_velocity);
        averageVelocityView = (TextView) findViewById(R.id.training_avarange_velocity);
        caloriesView = (TextView) findViewById(R.id.training_calories);
        altitudeView = (TextView) findViewById(R.id.training_altitude);
        gpsView = (TextView) findViewById(R.id.training_gps);


        handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(connected && bound) {
                    //Update UI with actual values
                    numberOfSeconds = trackerService.getCurrentNumberOfSeconds();
                    distanceInMeters = trackerService.getDistanceInMeters();
                    calories = trackerService.getCalories();

                    float momentVelocity = trackerService.getMomentVelocity();
                    double altitude = trackerService.getAltitudeInMeters();
                    double averageVelocity = TrackerService.getAvarangePace(numberOfSeconds, distanceInMeters);

                    timeView.setText(generateTimeNotation(numberOfSeconds));
                    distanceView.setText(generateDistanceInKilometersNotation(distanceInMeters));
                    momentVelocityView.setText(generateVelocityNotation(momentVelocity));
                    averageVelocityView.setText(generateVelocityNotation((float) averageVelocity));
                    altitudeView.setText(generateAltitudeNotation(altitude));
                    caloriesView.setText(generateCaloriesNotation(calories));

                    setGPSStatus(trackerService.isGPSFixed());
                }

                handler.postDelayed(this, 1000);
            }});

        databaseHandler = new Handler();
        databaseHandler.post(new Runnable() {
            @Override
            public void run() {
                if(trackerService != null) updateData();
                databaseHandler.postDelayed(this, 3*60*1000);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!bound) {
            Log.i(TAG, "Binding service.");
            Intent intent = new Intent(TrainingActivity.this, TrackerService.class);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
            startService(intent);
            bound = true;
        }

        if(trackerService == null) {
            Log.i(TAG, "Cannot obtain TrackerService object.");
            return;
        }

        pauseButton.setText(trackerService.paused ? R.string.resume : R.string.pause_button);
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "Unbind service.");
        if(bound && connected) unbindService(connection);
        connected = false;
        bound = false;

        super.onPause();
    }


    public void onPauseClicked(View view) {
        trackerService.paused = !trackerService.paused;
        pauseButton.setText(trackerService.paused ? R.string.resume : R.string.pause_button);
    }

    public void onStopClicked(View view) {
        Log.i(TAG, "Unbind service.");
        unbindService(connection);
        connected = false;
        bound = false;
        trackerService.paused = true;

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

        return String.format(Locale.GERMANY,"%d:%02d:%02d", hours, minutes,secs);
    }

    public static String generateVelocityNotation(double velocity) {
        if(Double.isNaN(velocity)) return "0.00 km/h";
        return String.format(Locale.GERMANY,"%.2f km/h", velocity);
    }

    public static String generateDistanceInKilometersNotation(double distanceInMeters) {
        distanceInMeters/=1000;
        return String.format(Locale.GERMANY,"%.2f km", distanceInMeters);
    }

    public static String generateAltitudeNotation(double altitudeInMeters) {
        return String.format(Locale.GERMANY,"%.2f m", altitudeInMeters);
    }

    public static String generateCaloriesNotation(int calories) {
        return String.format(Locale.GERMANY,"%s cal", calories);
    }

    public void setGPSStatus(boolean isFixed) {
        if(isFixed) {
            gpsView.setText(R.string.training_activity_GPSConnection);
            gpsView.setBackgroundResource(R.color.GPSconnection);
        } else {
            gpsView.setText(R.string.training_activity_noGPSConnection);
            gpsView.setBackgroundResource(R.color.noGPSconnection);
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
        if(list == null || list.isEmpty()) return;

        String encodedLatLngList = PolyUtil.encode(list.getLatLngList());
        String JSONEncodedGeopoints = SummaryActivity.encodeGeoPointListToJSON(list);

        LocalDatabaseTask.UpdateLocalDatabaseTask updateTask = new LocalDatabaseTask.UpdateLocalDatabaseTask(this, numberOfSeconds, distanceInMeters,
                calories, encodedLatLngList, JSONEncodedGeopoints, 0);

        updateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Unbind service.");
        if(bound && connected) unbindService(connection);
        connected = false;
        bound = false;

        super.onDestroy();
    }

}
