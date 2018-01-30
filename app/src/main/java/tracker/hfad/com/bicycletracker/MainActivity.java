package tracker.hfad.com.bicycletracker;

import android.*;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;



public class MainActivity extends Activity {
    private static final int REQUEST_LOCATION_PERMISSION_CODE = 41;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String TOP_FRAGMENT_TAG = "top fragment";
    private static final String KEY_CURRENT = "current fragment key";

    private ActionBarDrawerToggle drawerToggle;
    private ListView drawerList;
    private DrawerLayout drawerLayout;
    private int current = 0;


    public static class BicycleDatabaseHelper extends SQLiteOpenHelper
    {
        private static final String DB_NAME = "bicycletracker";
        private static final int DB_VERSION = 1;

        BicycleDatabaseHelper(Context ctx)
        {
            super(ctx, DB_NAME, null, DB_VERSION);
        }


        @Override
        public void onCreate(SQLiteDatabase db)
        {
            //Creating db tables

            db.execSQL("CREATE TABLE ACTIVITIES ( _id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "time INTEGER,"
                        + "distance REAL,"
                        + "calories INTEGER,"
                        + "date TEXT," //yyyy-MM-dd HH:mm:ss format
                        + "track TEXT,"
                        + "geopoints TEXT,"
                        + "discipline INTEGER,"
                        + "finished INTEGER);");

            db.execSQL("CREATE TABLE GOALS ( _id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        +"start TEXT,"
                        +"finish TEXT,"
                        +"max INTEGER,"
                        +"progress INTEGER);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {

        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(SaveSharedPreference.getStarted(this)) {
            Log.i(TAG, "Starting training automatically.");
            if(checkLocationPermission()) {
                startTraining();
            } else requestLocationPermission();
        }

        drawerList = (ListView) findViewById(R.id.drawer);
        drawerList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_activated_1,
                getResources().getStringArray(R.array.options)));
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.app_name, R.string.app_name);

        ActionBar actionBar = getActionBar();
        if(actionBar != null) {
            getActionBar().setHomeButtonEnabled(true);
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if(savedInstanceState != null) {
            Log.i(TAG, "Recreate saved state of activity.");
            current = savedInstanceState.getInt(KEY_CURRENT);
            selectItem(current);
            drawerList.setItemChecked(current, true);
        }


        getFragmentManager().addOnBackStackChangedListener( new android.app.FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {

                Fragment fragment = getFragmentManager().findFragmentByTag(TOP_FRAGMENT_TAG);

                if(fragment instanceof SettingsFragment) {
                    current = 3;
                } else if(fragment instanceof ArcheivesListFragment) {
                    current = 2;
                } else if(fragment instanceof StartFragment) {
                    current = 0;
                } else if(fragment instanceof GoalsFragment) {
                    current = 4;
                } else if(fragment instanceof CallendarFragment) {
                    current = 5;
                } else if(fragment instanceof PersonalBestFragment) {
                    current = 6;
                }

                setActivityBar(current);
                drawerList.setItemChecked(current, true);
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        setActivityBar(0);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_level_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch(item.getItemId()) {
            case R.id.settings: selectItem(3);
                                drawerList.setItemChecked(3, true);
                                return true;
            case R.id.music:    music();
                                return true;
            default:  return onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPostCreate(Bundle saveInstanceState) {
        super.onPostCreate(saveInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }


    private void selectItem(int position) {
        if(position !=1 && position != 7) {
            Fragment fragment;

            switch (position) {
                case 2: fragment =  new ArcheivesListFragment();
                                    break;
                case 3: fragment =  new SettingsFragment();
                                    break;
                case 4: fragment =  new GoalsFragment();
                                    break;
                case 5: fragment =  new CallendarFragment();
                                    break;
                case 6: fragment =  new PersonalBestFragment();
                                    break;
                default: fragment = new StartFragment();
            }

            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.contener, fragment, TOP_FRAGMENT_TAG);
            ft.addToBackStack(null);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            setActivityBar(position);
            ft.commit();
        }
        else if(position == 1) {
            if(checkLocationPermission()) {
                startTraining();
            } else requestLocationPermission();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final View view = LayoutInflater.from(this).inflate(R.layout.basic_dialog, null);

            TextView text =(TextView) view.findViewById(R.id.basic_dialog_text);
            text.setText(getString(R.string.logout_question));

            builder.setView(view)
                    .setPositiveButton(getString(R.string.positive_reaction), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id)
                        {

                            SaveSharedPreference.clear(MainActivity.this);

                            Intent intent = new Intent(MainActivity.this, LogActivity.class);
                            startActivity(intent);

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

        drawerLayout.closeDrawer(drawerList);
    }

    public void startTraining() {
        Log.i(TAG, "Starting training.");
        SaveSharedPreference.setStarted(this, true);
        Intent intent = new Intent(MainActivity.this, TrainingActivity.class);
        startActivity(intent);
        finish();
    }

    public void music() {
        Intent intent = new Intent("android.intent.action.MUSIC_PLAYER");
        startActivity(intent);
    }

    public void setActivityBar(int position) {
        ActionBar bar = getActionBar();
        if(bar == null) return;

        if(position == 0 || position == 6) {
            bar.setTitle(R.string.app_name);
            bar.setIcon(R.mipmap.logo);
        } else if(position == 3) {
            bar.setIcon(R.drawable.ic_setting_light);
            bar.setTitle(R.string.settings_title);
        } else if(position == 2) {
            bar.setIcon(R.mipmap.logo);
            bar.setTitle(getString(R.string.archive_title));
        } else if(position==4) {
            bar.setIcon(R.mipmap.logo);
            bar.setTitle(getString(R.string.goals_title));
        } else if(position == 5) {
            bar.setIcon(R.drawable.ic_event_black_24dp);
            bar.setTitle(getString(R.string.calendar_title));
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private boolean checkLocationPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int status = ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION);
            return status == PackageManager.PERMISSION_GRANTED;
        } else return true;
    }

    private void requestLocationPermission() {
        boolean shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        if(shouldShowRationale) {
            Log.i(TAG, "Show location permission rationale to user.");
            StartFragment.showToast(this, R.string.location_permission_rationale);
        }

        Log.i(TAG, "Requesting location permission.");
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_LOCATION_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_LOCATION_PERMISSION_CODE) {
            if(permissions.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Requesting location permission.");
                startTraining();
            } else {
                Log.i(TAG, "Location permission denied.");
            }
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT, current);
    }

}
