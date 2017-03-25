package tracker.hfad.com.bicycletracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.app.Fragment;
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

    private ActionBarDrawerToggle drawerToggle;
    private ListView drawerList;
    private DrawerLayout drawerLayout;
    private int current;


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
                        +"max INTEGER,"
                        +"progress INTEGER,"
                        +"type INTEGER,"
                        + "date TEXT," //date of end, yyyy-MM-dd HH:mm:ss format
                        +"sync INTEGER);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {

        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
                selectItem(position);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(SaveSharedPreference.getStarted(this))
        {
            Log.v("Training", "Start, main activ");
            startTraining();
        }

        drawerList = (ListView) findViewById(R.id.drawer);
        drawerList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_activated_1, getResources().getStringArray(R.array.options)));
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.app_name, R.string.app_name);


        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);


        if(savedInstanceState!=null)
        {
            current=savedInstanceState.getInt("current");
            selectItem(current);
            drawerList.setItemChecked(current, true);
        }
        else
        {
            current=0;
            selectItem(0);
        }


        getFragmentManager().addOnBackStackChangedListener(new android.app.FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {

                Fragment fragment = getFragmentManager().findFragmentByTag("visible");

                if(fragment instanceof SettingsFragment)
                {
                    current=3;
                }
                if(fragment instanceof ArcheivesListFragment)
                {
                    current=2;
                }
                if(fragment instanceof StartFragment)
                {
                    current=0;
                }
                if(fragment instanceof GoalsFragment)
                {
                    current=4;
                }
                if(fragment instanceof CallendarFragment)
                {
                    current=5;
                }
                if(fragment instanceof PersonalBestFragment)
                {
                    current=6;
                }

                setActivityBar(current);
                drawerList.setItemChecked(current, true);

            }
        });

    }


    @Override
    protected void onResume()
    {
        super.onResume();
        setActivityBar(0);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.top_level_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(drawerToggle.onOptionsItemSelected(item))
        {
            return true;
        }


        switch(item.getItemId())
        {
            case R.id.settings: selectItem(3);
                                drawerList.setItemChecked(3, true);
                                return true;

            case R.id.music:    music();
                                return true;

            default:  return onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onPostCreate(Bundle saveInstanceState)
    {
        super.onPostCreate(saveInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }


    private void selectItem(int position)
    {
        if(position!=1 && position!=7)
        {
            Fragment fragment;

            switch (position)
            {
                case 0: fragment =  new StartFragment();
                    break;
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


                default: fragment = new SettingsFragment();
            }

            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.contener, fragment, "visible");
            ft.addToBackStack(null);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            setActivityBar(position);
            ft.commit();
        }
        else if(position==1)
        {
            startTraining();
        }
        else if(position==7)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final View view = LayoutInflater.from(this).inflate(R.layout.basic_dialog, null);

            TextView text =(TextView) view.findViewById(R.id.basic_dialog_text);
            text.setText("Do you want to log out?");

            builder.setView(view)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id)
                        {

                            SaveSharedPreference.clear(MainActivity.this);

                            Intent intent = new Intent(MainActivity.this, LogActivity.class);
                            startActivity(intent);

                            finish();

                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
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

    public void startActivity(View view)
    {
        startTraining();
    }

    public void startTraining()
    {
        SaveSharedPreference.setStarted(this, true);
        Intent intent = new Intent(MainActivity.this, TrainingActivity.class);
        startActivity(intent);
        finish();
    }

    public void music()
    {
        Intent intent = new Intent("android.intent.action.MUSIC_PLAYER");
        startActivity(intent);
    }


    public void setActivityBar(int position)
    {
        if(position==0 || position==6)
        {
            ActionBar bar = getActionBar();

            bar.setTitle(R.string.app_name);
            bar.setIcon(R.mipmap.logo);
        }
        else if(position==3)
        {
            ActionBar bar = getActionBar();
            bar.setIcon(R.drawable.ic_setting_light);
            bar.setTitle(R.string.Settings);
        }
        else if(position==2)
        {
            ActionBar bar = getActionBar();
            bar.setIcon(R.mipmap.logo);
            bar.setTitle("Last trainings");

        }
        else if(position==4)
        {
            ActionBar bar = getActionBar();
            bar.setIcon(R.mipmap.logo);
            bar.setTitle("Training goals");
        }else if(position==5)
        {
            ActionBar actionBar = getActionBar();
            actionBar.setIcon(R.drawable.ic_event_black_24dp);
            actionBar.setTitle("Callendar");
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putInt("current", current);
    }

}
