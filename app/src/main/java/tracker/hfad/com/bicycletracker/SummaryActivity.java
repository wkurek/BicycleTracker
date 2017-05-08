package tracker.hfad.com.bicycletracker;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class SummaryActivity extends Activity implements OnMapReadyCallback {

    public static final String TIME ="time";
    public static final String DISTANCE = "disctance";
    public static final String CALORIES = "calories";
    public static final String GEOPOINTLIST = "MyGeoPointList";

    private TrackerService.MyGeoPointList myGeoPointList;
    private Integer numberOfSeconds, calories;
    private Double distanceInMeters;
    private LinkedList<LatLng> latLngList;

    private GoogleMap googleMap;
    private LineChart lineChart;
    private Marker marker;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        SaveSharedPreference.setStarted(this, false);

        LocalDatabaseTask.DeleteLocalDatabaseTask deleteTask = new LocalDatabaseTask.DeleteLocalDatabaseTask(this);
        deleteTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);


        getActionBar().setDisplayHomeAsUpEnabled(true);

        ArrayList<TrackerService.MyGeoPoint> myGeoPointArrayList = getIntent().getExtras().getParcelableArrayList(GEOPOINTLIST);
        if(myGeoPointArrayList!=null && !myGeoPointArrayList.isEmpty()) myGeoPointList = new TrackerService.MyGeoPointList(myGeoPointArrayList);

        numberOfSeconds = (Integer) getIntent().getExtras().get(TIME);
        distanceInMeters = (Double) getIntent().getExtras().get(DISTANCE);
        calories = (Integer) getIntent().getExtras().get(CALORIES);

        MapFragment map = ((MapFragment) getFragmentManager().findFragmentById(R.id.summary_map));
        map.getMapAsync(this);

        final Button save_button = (Button) findViewById(R.id.summary_save_button);
        save_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                addToDatabase();
            }
        });

        TextView summary_distance = (TextView) findViewById(R.id.summary_distance);
        TextView summary_time = (TextView) findViewById(R.id.summary_time);
        TextView summary_avarange_velocity = (TextView) findViewById(R.id.summary_avarange_velocity);
        TextView summary_max_velocity = (TextView) findViewById(R.id.summary_max_velocity);
        TextView summary_calories = (TextView) findViewById(R.id.summary_calories);
        TextView summary_altitude = (TextView) findViewById(R.id.summary_altitude);

        final TextView summary_chart_altitude = (TextView) findViewById(R.id.summary_chart_max_altitude);
        final TextView summary_chart_time = (TextView) findViewById(R.id.summary_chart_time);
        final TextView summary_chart_velocity = (TextView) findViewById(R.id.summary_chart_max_velocity);

        summary_distance.setText(TrainingActivity.generateDistanceInKilometersNotation(distanceInMeters));
        summary_time.setText(TrainingActivity.generateTimeNotation(numberOfSeconds));
        summary_avarange_velocity.setText(TrainingActivity.generateVelocityNotation(calculateAvarangeVelocity()));
        summary_calories.setText(TrainingActivity.generateCaloriesNotation(calories));

        if(myGeoPointList!=null && !myGeoPointList.isEmpty())
        {
            summary_max_velocity.setText(generateVelocityNotation(Collections.max(myGeoPointList.getVelocityList())));
            summary_altitude.setText(TrainingActivity.generateAltitudeNotation(Collections.max(myGeoPointList.getAltitudeList())));
        }

        lineChart = (LineChart) findViewById(R.id.summary_chart);

        lineChart.setHighlightPerTapEnabled(true);
        lineChart.setNoDataTextColor(R.color.standardBlack);
        lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {


            @Override
            public void onValueSelected(Entry e, Highlight h)
            {
                TrackerService.MyGeoPoint point;

                if(e instanceof VelocityEntry)
                {
                    point = ((VelocityEntry) e).geoPoint;
                }
                else
                {
                    point = ((AltitudeEntry) e).geoPoint;
                }

                summary_chart_time.setText(TrainingActivity.generateTimeNotation(point.getSecond()));
                summary_chart_altitude.setText(TrainingActivity.generateAltitudeNotation(point.getAltitude()));
                summary_chart_velocity.setText(generateVelocityNotation(point.getVelocity()));

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
    public void onMapReady(GoogleMap map)
    {
        googleMap = map;
        if(myGeoPointList!=null && !myGeoPointList.isEmpty())
        {
            latLngList = myGeoPointList.getLatLngList();

            if(!(latLngList.isEmpty()))
            {
                PolylineOptions line = new PolylineOptions().color(Color.parseColor("#164d54")).geodesic(true);
                LatLngBounds.Builder boundsBuilder = LatLngBounds.builder();

                for(LatLng latLng : latLngList)
                {
                    line.add(latLng);
                    boundsBuilder.include(latLng);
                }


                final LatLngBounds latLngBounds = boundsBuilder.build();

                map.addPolyline(line);

                map.addMarker(new MarkerOptions().position(latLngList.getFirst()).title("Start"));
                map.addMarker(new MarkerOptions().position(latLngList.getLast()).title("End"));

                map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded()
                    {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 30));
                    }
                });
            }
        }
    }



    public void addToDatabase()
    {
        if(myGeoPointList != null)
        {
            Log.v("UPDATE", "finished invoke");

            //Preparation of data
            if(latLngList == null) { latLngList = myGeoPointList.getLatLngList(); }

            String encodedLatLngList = PolyUtil.encode(latLngList);
            String JSONEncodedGeopoints = encodeGeoPointListToJSON(myGeoPointList);

            //local database
            LocalDatabaseTask.InsertIntoLocalDatabaseTask saveTask = new LocalDatabaseTask.InsertIntoLocalDatabaseTask(this, numberOfSeconds, distanceInMeters, calories,
                    encodedLatLngList, JSONEncodedGeopoints, 1);

            saveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            //extern MySql database

            if(DetailActivity.isNetworkAvailable(this)) //Chech if network connection is avaliable
            {
                ExternDatabaseTasks.ImportTrainingDataTask externSaveTask = new ExternDatabaseTasks.ImportTrainingDataTask(
                        this, numberOfSeconds, distanceInMeters, calories,
                        encodedLatLngList, JSONEncodedGeopoints, 1, getActualDate());

                externSaveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

        }

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

        this.finish();

    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        switch(keycode) {
            case KeyEvent.KEYCODE_BACK:

                this.finish();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);

                return true;
        }

        return super.onKeyDown(keycode, e);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.summary_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.share:    String message = " I've runned "+ distanceInMeters/1000 + " km in "+ TrainingActivity.generateTimeNotation(numberOfSeconds) +" with application BT";
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, message);
                startActivity(Intent.createChooser(share, "Choose application: "));
                return true;

            case R.id.delete:   startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;

            case R.id.add:      addToDatabase();
                return true;

            default:            return true;

        }
    }


    public static String generateVelocityNotation(double velocity)
    {
        return String.format("%.2f km/h", velocity);
    }

    public float calculateAvarangeVelocity()
    {
        return (float) (distanceInMeters/numberOfSeconds*3.6);
    }

    public void fillChart()
    {
        List<Entry> velocityEntryList = new ArrayList<>();
        List<Entry> altitudeEntryList = new ArrayList<>();


        for(TrackerService.MyGeoPoint point : myGeoPointList)
        {
            VelocityEntry velocityEntry = new VelocityEntry(point);
            AltitudeEntry altitudeEntry = new AltitudeEntry(point);

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

    public static class VelocityEntry extends Entry
    {
        TrackerService.MyGeoPoint geoPoint;

        VelocityEntry(TrackerService.MyGeoPoint myGeoPoint)
        {
            super((float) myGeoPoint.getSecond(), (float) myGeoPoint.getVelocity());
            this.geoPoint = myGeoPoint;
        }
    }

    public static class AltitudeEntry extends Entry
    {
        TrackerService.MyGeoPoint geoPoint;

        AltitudeEntry(TrackerService.MyGeoPoint myGeoPoint)
        {
            super((float) myGeoPoint.getSecond(), (float) myGeoPoint.getAltitude());
            this.geoPoint = myGeoPoint;
        }
    }


    public static String encodeGeoPointListToJSON(TrackerService.MyGeoPointList list)
    {
        ArrayList<Float> arrayList = new ArrayList<Float>();

        for(TrackerService.MyGeoPoint point : list)
        {
            arrayList.add((float) point.getSecond());
            arrayList.add((float) point.getVelocity());
            arrayList.add((float) point.getAltitude());
        }

        JSONArray jsonArray = new JSONArray(arrayList);

        return jsonArray.toString();
    }

    public static String getActualDate()
    {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();

        return dateFormat.format(date);
    }

}