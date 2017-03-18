package tracker.hfad.com.bicycletracker;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import java.util.ArrayList;
import java.util.LinkedList;


public class TrackerService extends Service{

    private IBinder binder = new TrackerBinder();

    private long startTime, pauseTime, lastLocationTime;
    public boolean paused = false;

    private Handler timeHandler;

    private MyGeoPoint actualGeoPoint;
    private MyGeoPointList geoPointList;
    private double distanceInMeters;
    private Location lastLocation;



    public class TrackerBinder extends Binder
    {
        TrackerService getService() { return TrackerService.this; }

        TrackerService getService(TrackerService.MyGeoPointList list, int time,  double distance)
        {
            startTime -= time*1000;
            distanceInMeters += distance;
            geoPointList = new MyGeoPointList(list);

            return TrackerService.this;
        }
    }

    public TrackerService() {}

    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return START_STICKY;
    }


    @Override
    public void onCreate() {


        startTime += System.currentTimeMillis();
        pauseTime = -1;
        lastLocationTime = -1;

        Intent nextIntent = new Intent(this, TrainingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, nextIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("BicycleTracker")
                .setTicker("Start of training")
                .setContentText("tracking ...")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_timer_white_24dp)
                .build();

        startForeground(3456, notification);

        timeHandler = new Handler();
        timeHandler.post(new Runnable() {
            @Override
            public void run()
            {

                if(!paused)
                {
                    if(pauseTime>0)
                    {
                        long pausedTime = System.currentTimeMillis() - pauseTime;
                        startTime += pausedTime;

                        pauseTime = -1;
                    }

                }else
                {
                    if(!(pauseTime>0))
                    {
                        pauseTime = System.currentTimeMillis();
                    }
                }

                timeHandler.postDelayed(this, 1000);
            }
        });

        if(!SaveSharedPreference.getRecreated(this))
        {
            if(geoPointList == null) geoPointList = new MyGeoPointList();
            insertTraining();
        }


        LocationListener locationListener = new LocationListener()
        {
            @Override
            public void onLocationChanged(Location location)
            {
                lastLocationTime = System.currentTimeMillis();

                if(!paused)
                {
                    if(lastLocation == null)
                    {
                        lastLocation = location;
                    }

                    distanceInMeters += location.distanceTo(lastLocation);
                    lastLocation = location;

                    actualGeoPoint = new MyGeoPoint(lastLocation, getCurrentNumberOfSeconds());

                    if(geoPointList == null)  geoPointList = new MyGeoPointList();

                    geoPointList.add(actualGeoPoint);

                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };


        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {return;}

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 2, locationListener);

    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.v("Training Activity", "koniec uslugi");
    }

    //Functions which give access to private values

    Integer getCurrentNumberOfSeconds()
    {
        int secondsNumber;
        long start = startTime;

        if(pauseTime>0)
        {
            long pausedTime = System.currentTimeMillis() - pauseTime;
            start += pausedTime;
        }

        secondsNumber = (int) (System.currentTimeMillis() - start);
        secondsNumber/=1000;

        return secondsNumber;
    }

    double getDistanceInMeters()
    {
        return distanceInMeters;
    }

    double getAltitudeInMeters()
    {
        if(actualGeoPoint != null) return actualGeoPoint.getAltitude();
        return 0.00;
    }

    float getMomentVelocity()
    {
        if(actualGeoPoint != null) return (float) actualGeoPoint.getVelocity();
        return 0;
    }

    static double getAvarangePace(int numberOfSeconds, double distanceInMeters)
    {
        return (distanceInMeters/numberOfSeconds*3.6);
    }

    int getCalories()
    {
       if(distanceInMeters == 0) return 0;

        int numberOfSeconds, factor;

        numberOfSeconds = getCurrentNumberOfSeconds();
        factor = 0;

        double avg = getAvarangePace(numberOfSeconds, distanceInMeters);
        avg /= 1.61; // kmph -> mph

        if(avg < 10) factor = 281;
        else if(avg >= 10 && avg < 12) factor = 422;
        else if(avg >= 12 && avg < 14) factor = 536;
        else if(avg >= 14 && avg < 16) factor = 704;
        else if(avg >= 16 && avg < 20) factor = 844;
        else if(avg >= 20) factor = 1126;

        return (int) (numberOfSeconds * factor / 3600);
    }

    boolean isGPSFixed()
    {
        if(!(lastLocationTime>0)) return false;
        else
        {
            if((System.currentTimeMillis() - lastLocationTime) < 8000) return true;
            return false;
        }
    }


    public static class MyGeoPoint implements Parcelable
    {
        private double velocity, altitude;
        private Integer second;
        private LatLng latLng;

        MyGeoPoint(Location location, Integer second)
        {
            this.latLng = new LatLng(location.getLatitude(), location.getLongitude());
            this.velocity = (location.getSpeed()*3.6);
            this.altitude = location.getAltitude();
            this.second = second;
        }

        MyGeoPoint(Integer second, double velocity, double altitude, LatLng latLng)
        {
            this.latLng = latLng;
            this.velocity = velocity;
            this.altitude = altitude;
            this.second = second;
        }

        MyGeoPoint(Parcel in)
        {
            second = in.readInt();
            velocity = in.readDouble();
            altitude = in.readDouble();

            double v1 = in.readDouble();
            double v2 = in.readDouble();

            latLng = new LatLng(v1, v2);
        }

        public LatLng getLatLng() {return latLng;}

        public Integer getSecond()
        {
            return second;
        }

        public double getVelocity()
        {
            return velocity;
        }

        public double getAltitude()
        {
            return altitude;
        }

        @Override
        public int describeContents() { return 0; }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeInt(second);
            dest.writeDouble(velocity);
            dest.writeDouble(altitude);
            dest.writeDouble(latLng.latitude);
            dest.writeDouble(latLng.longitude);
        }

        public static final Parcelable.Creator<MyGeoPoint> CREATOR = new Parcelable.Creator<MyGeoPoint>(){

            public MyGeoPoint createFromParcel(Parcel in) { return new MyGeoPoint(in); }

            public MyGeoPoint[] newArray(int size) { return new MyGeoPoint[size]; }

        };
    }

    final static class MyGeoPointList extends ArrayList<MyGeoPoint>
    {
        MyGeoPointList() {}

        MyGeoPointList(ArrayList<MyGeoPoint> arrayList)
        {
            if(arrayList!=null && !arrayList.isEmpty())
            {
                for(MyGeoPoint point : arrayList)
                {
                    add(point);
                }
            }
        }

        LinkedList<LatLng> getLatLngList()
        {
            LinkedList<LatLng> locationLinkedList = new LinkedList<LatLng>();

            for( MyGeoPoint geoPoint : this)
            {
                locationLinkedList.add(geoPoint.getLatLng());
            }

            return locationLinkedList;
        }

        LinkedList<Double> getVelocityList()
        {
            LinkedList<Double> locationLinkedList = new LinkedList<Double>();

            for( MyGeoPoint geoPoint : this)
            {
                locationLinkedList.add(geoPoint.getVelocity());
            }

            return locationLinkedList;
        }

        LinkedList<Double> getAltitudeList()
        {
            LinkedList<Double> locationLinkedList = new LinkedList<Double>();

            for( MyGeoPoint geoPoint : this)
            {
                locationLinkedList.add(geoPoint.getAltitude());
            }

            return locationLinkedList;
        }
    }

    MyGeoPointList getGeoPointList()
    {
        return geoPointList;
    }


    private void insertTraining()
    {
        if(geoPointList != null)
        {
            String encodedLatLngList = PolyUtil.encode(geoPointList.getLatLngList());
            String JSONEncodedGeopoints = SummaryActivity.encodeGeoPointListToJSON(geoPointList);

            LocalDatabaseTask.InsertIntoLocalDatabaseTask saveTask = new LocalDatabaseTask.InsertIntoLocalDatabaseTask(this, getCurrentNumberOfSeconds(), distanceInMeters, getCalories(),
                    encodedLatLngList, JSONEncodedGeopoints, 0);

            saveTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }
    }
}

