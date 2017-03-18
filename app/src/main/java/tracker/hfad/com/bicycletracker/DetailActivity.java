package tracker.hfad.com.bicycletracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

public class DetailActivity extends Activity implements OnMapReadyCallback {

    public static final String ID = "training _id";

    private double distance;
    private int trainingID, seconds, calories, discipline, finished;
    private String date;
    private TrackerService.MyGeoPointList myGeoPointList;

    private SQLiteDatabase db;

    private LineChart lineChart;
    private GoogleMap googleMap;
    private Marker marker;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        trainingID = (Integer) getIntent().getExtras().get(ID);

        try
        {
            SQLiteOpenHelper helper = new MainActivity.BicycleDatabaseHelper(this);
            db = helper.getReadableDatabase();

            Cursor cursor = db.query("ACTIVITIES", new String[]{"time", "distance", "calories", "date", "track", "geopoints", "discipline", "finished"}, "_id = ?",
                    new String[]{Integer.toString(trainingID)},null,null,null);

            if(cursor.moveToFirst())
            {
                String encodedPolyLine, JSONencodedGeoPointList;


                //collecting and preparing data from database

                seconds = cursor.getInt(0);
                distance = cursor.getDouble(1);
                calories = cursor.getInt(2);
                date = cursor.getString(3);
                encodedPolyLine = cursor.getString(4);
                JSONencodedGeoPointList = cursor.getString(5);
                discipline = cursor.getInt(6);
                finished = cursor.getInt(7);

                Log.v("TRACK: ", encodedPolyLine);

                myGeoPointList = new TrackerService.MyGeoPointList();
                JSONArray jsonArray = new JSONArray(JSONencodedGeoPointList);
                List<LatLng> track = PolyUtil.decode(encodedPolyLine);

                for(int i=0; i<jsonArray.length(); i+=3)
                {
                    TrackerService.MyGeoPoint point = new TrackerService.MyGeoPoint(jsonArray.getInt(i), jsonArray.getDouble(i+1), jsonArray.getDouble(i+2), track.get(i/3));
                    myGeoPointList.add(point);
                }

            }

            cursor.close();

        }catch (SQLiteException e)
        {
            Toast.makeText(DetailActivity.this, "Database is not accessable", Toast.LENGTH_SHORT).show();
        } catch (JSONException e)
        {
            e.printStackTrace();
        }

        //Filling fields with data

        TextView date_field = (TextView) findViewById(R.id.detail_date);
        TextView distance_field = (TextView) findViewById(R.id.detail_distance);
        TextView time_field = (TextView) findViewById(R.id.detail_time);
        TextView velocity_field = (TextView) findViewById(R.id.detail_velocity);
        TextView calories_field = (TextView) findViewById(R.id.detail_calories);

        final TextView detail_chart_time = (TextView) findViewById(R.id.detail_chart_time);
        final TextView detail_chart_altitude = (TextView) findViewById(R.id.detail_chart_max_altitude);
        final TextView detail_chart_velocity = (TextView) findViewById(R.id.detail_chart_max_velocity);


        date_field.setText(date);
        distance_field.setText(TrainingActivity.generateDistanceInKilometersNotation(distance));
        time_field.setText(TrainingActivity.generateTimeNotation(seconds));
        velocity_field.setText(TrainingActivity.generateVelocityNotation((float) TrackerService.getAvarangePace(seconds, distance)));
        calories_field.setText(TrainingActivity.generateCaloriesNotation(calories));


        //Add buttons listeners

        ImageButton delete_button = (ImageButton) findViewById(R.id.detail_button_delete);
        ImageButton share_button = (ImageButton) findViewById(R.id.detail_button_share);
        ImageButton upload_button = (ImageButton) findViewById(R.id.detail_button_upload);

        delete_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                deleteActivity();
            }
        });

        share_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                shareActivity();
            }
        });

        upload_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                uploadToInternet();
            }
        });

        MapFragment map = ((MapFragment) getFragmentManager().findFragmentById(R.id.detail_map));
        map.getMapAsync(this);

        lineChart = (LineChart) findViewById(R.id.detail_chart);

        lineChart.setHighlightPerTapEnabled(true);
        lineChart.setNoDataTextColor(R.color.standardBlack);
        lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {


            @Override
            public void onValueSelected(Entry e, Highlight h)
            {
                TrackerService.MyGeoPoint point;

                if(e instanceof SummaryActivity.VelocityEntry)
                {
                    point = ((SummaryActivity.VelocityEntry) e).geoPoint;
                }
                else
                {
                    point = ((SummaryActivity.AltitudeEntry) e).geoPoint;
                }

                detail_chart_time.setText(TrainingActivity.generateTimeNotation(point.getSecond()));
                detail_chart_altitude.setText(TrainingActivity.generateAltitudeNotation(point.getAltitude()));
                detail_chart_velocity.setText(SummaryActivity.generateVelocityNotation(point.getVelocity()));

                if(googleMap!=null)
                {
                    if(marker != null) marker.remove();
                    marker = googleMap.addMarker(new MarkerOptions().position(point.getLatLng()));
                }

            }

            @Override
            public void onNothingSelected()
            {
                if(marker != null) marker.remove();
            }
        });

        if(myGeoPointList!=null && !myGeoPointList.isEmpty()) fillChart();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.detail_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
            switch (item.getItemId())
            {
                case R.id.share:    shareActivity();
                                    return true;

                case R.id.delete:   deleteActivity();
                                    return true;

                default:            return true;

            }
    }

    @Override
    public void onMapReady(final GoogleMap map)
    {
        googleMap = map;

        if(myGeoPointList!=null &&!myGeoPointList.isEmpty())
        {
            List<LatLng> track = myGeoPointList.getLatLngList();

            if(!track.isEmpty())
            {
                PolylineOptions line = new PolylineOptions().color(Color.parseColor("#164d54")).geodesic(true);
                LatLngBounds.Builder boundsBuilder = LatLngBounds.builder();

                for(LatLng x : track)
                {
                    line.add(x);
                    boundsBuilder.include(x);
                }

                final LatLngBounds latLngBounds = boundsBuilder.build();

                map.addPolyline(line);
                map.addMarker(new MarkerOptions().position(track.get(0)));
                map.addMarker(new MarkerOptions().position(track.get(track.size()-1)));

                map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        map.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 30));
                    }
                });
            }
        }
    }


    public void deleteActivity()
    {
        final Context ctx = this;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View view = LayoutInflater.from(this).inflate(R.layout.basic_dialog, null);

        TextView text =(TextView) view.findViewById(R.id.basic_dialog_text);
        text.setText("Do you want to detele this activity?");

        builder.setView(view)
                .setPositiveButton(R.string.positive_reaction, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        db.delete("ACTIVITIES", "_id = ?", new String[]{Integer.toString(trainingID)});
                        startActivity(new Intent(ctx, MainActivity.class));
                        finish();
                    }
                })
                .setNegativeButton(R.string.negative_reaction, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        dialog.cancel();
                    }
                });

        Dialog dialog = builder.create();
        dialog.show();
    }

    public void shareActivity()
    {

    }

    public void uploadToInternet()
    {
        if(isNetworkAvailable() && myGeoPointList!=null)
        {

            Log.v("wysyłanie", "danych");
            //Preparation of data
            List<LatLng> latLngList = myGeoPointList.getLatLngList();

            String encodedLatLngList = PolyUtil.encode(latLngList);
            String JSONEncodedGeopoints = SummaryActivity.encodeGeoPointListToJSON(myGeoPointList);


            //extern MySql database
            ExternDatabaseTasks.ImportTrainingDataTask externSaveTask = new ExternDatabaseTasks.ImportTrainingDataTask(this, seconds, distance, calories,
                    encodedLatLngList, JSONEncodedGeopoints, finished, date);

            externSaveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        }else
        {
            Toast.makeText(DetailActivity.this, "Internet connection is not accessable", Toast.LENGTH_SHORT).show();
        }

    }

    public boolean isNetworkAvailable()
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void fillChart()
    {
        List<Entry> velocityEntryList = new ArrayList<>();
        List<Entry> altitudeEntryList = new ArrayList<>();


        for(TrackerService.MyGeoPoint point : myGeoPointList)
        {
            SummaryActivity.VelocityEntry velocityEntry = new SummaryActivity.VelocityEntry(point);
            SummaryActivity.AltitudeEntry altitudeEntry = new SummaryActivity.AltitudeEntry(point);

            velocityEntryList.add(velocityEntry);
            altitudeEntryList.add(altitudeEntry);
        }

        LineDataSet actualVelocityDataSet = new LineDataSet( velocityEntryList, "velocity");
        LineDataSet actualAltitudeDataSet = new LineDataSet( altitudeEntryList, "altitude");

        LineData chartData = new LineData();

        actualAltitudeDataSet.setColor(R.color.standardGreen);
        actualVelocityDataSet.setColor(R.color.standardBlack);

        chartData.addDataSet(actualVelocityDataSet);
        chartData.addDataSet(actualAltitudeDataSet);
        lineChart.setData(chartData);
        lineChart.invalidate();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        db.close();
    }
}
