package tracker.hfad.com.bicycletracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


public class StartFragment extends Fragment {
    private static final int REQUEST_LOCATION_PERMISSION_CODE = 43;

    private static final int LOCATION_UPDATES_MINIMAL_TIME = 1600;
    private static final int LOCATION_UPDATES_MINIMAL_DISTANCE = 1;

    private static final String TAG = StartFragment.class.getSimpleName();

    private LocationListener locationListener;
    private LocationManager locationManager;

    private boolean requestingLocationUpdates = false;
    private long lastLocationUpdateTime = 0;
    private boolean locationProviderAvailable = false;

    private Handler gpsStatusHandler;
    private Runnable gpsStatusUpdatingTask;

    ImageButton gpsButton;

    public StartFragment() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_start, container, false);

        gpsButton = (ImageButton) view.findViewById(R.id.start_gps_button);
        ImageButton browserButton = (ImageButton) view.findViewById(R.id.start_browser_button);
        ImageButton logoutButton = (ImageButton) view.findViewById(R.id.start_logout_button);
        ImageButton startButton = (ImageButton) view.findViewById(R.id.start_button);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Starting training.");
                Activity activity = getActivity();
                SaveSharedPreference.setStarted(activity, true);
                Intent intent = new Intent(activity, TrainingActivity.class);
                startActivity(intent);
                activity.finish();
            }
        });

        browserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String URL = "http://rower.lowicz.com.pl/BicycleTracker%20www/index.php";

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL));
                startActivity(browserIntent);
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                final View view = LayoutInflater.from(getActivity()).inflate(R.layout.basic_dialog, null);

                TextView text = (TextView) view.findViewById(R.id.basic_dialog_text);
                text.setText(getString(R.string.logout_question));

                builder.setView(view)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {

                                SaveSharedPreference.clear(getActivity());

                                Intent intent = new Intent(getActivity(), LogActivity.class);
                                startActivity(intent);

                                getActivity().finish();

                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                Dialog dialog = builder.create();
                dialog.show();
            }
        });


        gpsStatusHandler = new Handler();
        gpsStatusUpdatingTask = new Runnable() {
            @Override
            public void run()
            {
                if(isGPSAvailable()) {
                    gpsButton.setBackgroundResource(R.drawable.start_round_button);
                    gpsButton.setImageResource(R.mipmap.ic_gps_fixed_white_48dp);
                } else {
                    gpsButton.setBackgroundResource(R.drawable.start_feature_round_button);
                    gpsButton.setImageResource(R.mipmap.ic_gps_off_black_48dp);
                }

                gpsStatusHandler.postDelayed(this, 1600);
            }
        };

        createLocationListener();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if(checkLocationPermission()) {
            startLocationUpdates();
            gpsStatusHandler.post(gpsStatusUpdatingTask);
        } else requestLocationPermission();
    }

    @Override
    public void onPause() {
        stopLocationUpdates();
        gpsStatusHandler.removeCallbacks(gpsStatusUpdatingTask);
        super.onPause();
    }


    @Override
    public void onStart() {
        super.onStart();
        provideRecreationOfTraining();
    }

    private boolean isGPSAvailable() {
        return requestingLocationUpdates && locationProviderAvailable && (lastLocationUpdateTime > 0) &&
                (System.currentTimeMillis() - lastLocationUpdateTime < 8000);
    }

    private void provideRecreationOfTraining()
    {
        boolean finished = true;

        try
        {

            SQLiteOpenHelper dbHelper = new MainActivity.BicycleDatabaseHelper(getActivity());
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor cursor = db.query("ACTIVITIES", null, "finished = ?", new String[]{Integer.toString(0)}, null, null, null);

            if(cursor.getCount() == 1) finished = false;

            cursor.close();
            db.close();

        }catch(SQLiteException e)
        {
            Log.v("ERROR: ", "Cannot check last record in database");
        }

        if(finished) return;


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.large_dialog, null);

        TextView text = (TextView) view.findViewById(R.id.large_dialog_text);
        text.setText(R.string.start_activity_recreation_info);

        builder.setView(view)
                .setPositiveButton(R.string.positive_reaction, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        SaveSharedPreference.setRecreated(mainActivity, true);

                        mainActivity.startTraining();

                    }
                })
                .setNegativeButton(R.string.negative_reaction, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        dialog.cancel();
                    }
                });

        Dialog dialog = builder.create();
        dialog.show();

    }

    private void createLocationListener() {
        locationListener =  new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                lastLocationUpdateTime = System.currentTimeMillis();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                locationProviderAvailable = status != LocationProvider.OUT_OF_SERVICE;
            }

            @Override
            public void onProviderEnabled(String provider) { locationProviderAvailable = true; }

            @Override
            public void onProviderDisabled(String provider) { locationProviderAvailable = false; }
        };

    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        locationManager = (LocationManager)
                getActivity().getSystemService(Context.LOCATION_SERVICE);

        if(locationManager != null) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATES_MINIMAL_TIME,
                    LOCATION_UPDATES_MINIMAL_DISTANCE, locationListener);
            requestingLocationUpdates = true;
        }
    }

    private void stopLocationUpdates() {
        if(requestingLocationUpdates) {
            locationManager.removeUpdates(locationListener);
            requestingLocationUpdates = false;
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private boolean checkLocationPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Activity activity = getActivity();
            if(activity == null) return false;

            int status = ActivityCompat.checkSelfPermission(activity,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            return status == PackageManager.PERMISSION_GRANTED;
        } else return true;
    }

    private void requestLocationPermission() {
        boolean shouldShowRationale = FragmentCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if(shouldShowRationale) {
            Log.i(TAG, "Show location permission rationale to user.");
            showToast(getActivity(), R.string.location_permission_rationale);
        }

        Log.i(TAG, "Requesting location permission.");
        FragmentCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_LOCATION_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_LOCATION_PERMISSION_CODE) {
            if(permissions.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Requesting location permission.");
                startLocationUpdates();
                gpsStatusHandler.post(gpsStatusUpdatingTask);
            } else {
                Log.i(TAG, "Location permission denied.");
                showToast(getActivity(), R.string.location_permission_denied);
            }
        }
    }

    public static void showToast(Context context, int resId) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show();
    }


}
