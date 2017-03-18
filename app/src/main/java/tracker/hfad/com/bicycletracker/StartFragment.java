package tracker.hfad.com.bicycletracker;

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
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;


public class StartFragment extends Fragment {

    private boolean gpsFixed;
    private long lastLocationChangeTime;
    private Handler gpsStatusHandler;
    private Runnable gpsStatusUpdatingTask;

    public StartFragment() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_start, container, false);
    }


    @Override
    public void onStart() {
        super.onStart();

        provideRecreationOfTraining();

        lastLocationChangeTime = -1;

        final ImageButton gps_button = (ImageButton) getView().findViewById(R.id.start_gps_button);
        ImageButton browser_button = (ImageButton) getView().findViewById(R.id.start_browser_button);
        ImageButton logout_button = (ImageButton) getView().findViewById(R.id.start_logout_button);

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location)
            {
                lastLocationChangeTime = System.currentTimeMillis();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras)
            {
                if(status == LocationProvider.OUT_OF_SERVICE) gpsFixed = false;
                else gpsFixed = true;
            }

            @Override
            public void onProviderEnabled(String provider) { gpsFixed = true; }

            @Override
            public void onProviderDisabled(String provider) { gpsFixed = false; }
        };

        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {return;}

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1600, 1, locationListener);

        gpsStatusUpdatingTask = new Runnable()
        {
            @Override
            public void run()
            {
                if(isGPSFixed())
                {
                    gps_button.setBackgroundResource(R.drawable.start_round_button);
                    gps_button.setImageResource(R.mipmap.ic_gps_fixed_white_48dp);

                }else
                {
                    gps_button.setBackgroundResource(R.drawable.start_feature_round_button);
                    gps_button.setImageResource(R.mipmap.ic_gps_off_black_48dp);
                }

                gpsStatusHandler.postDelayed(this, 1600);
            }
        };

        gpsStatusHandler = new Handler();
        gpsStatusHandler.post(gpsStatusUpdatingTask);



        browser_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String URL = "http://rower.lowicz.com.pl/BicycleTracker%20www/index.php";

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL));
                startActivity(browserIntent);
            }
        });

        logout_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                final View view = LayoutInflater.from(getActivity()).inflate(R.layout.basic_dialog, null);

                TextView text = (TextView) view.findViewById(R.id.basic_dialog_text);
                text.setText("Do you want to log out?");

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
    }

    private boolean isGPSFixed()
    {
        if(gpsFixed || ((System.currentTimeMillis() - lastLocationChangeTime < 8000) && (lastLocationChangeTime>0))) return true;
        return false;
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


    @Override
    public void onStop()
    {
        super.onStop();
        gpsStatusHandler.removeCallbacks(gpsStatusUpdatingTask);
    }

}
