package tracker.hfad.com.bicycletracker;


import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;


public class ArcheivesListFragment extends ListFragment {

    private SQLiteDatabase db;
    private Cursor cursor;

    public ArcheivesListFragment() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public void onStart()
    {
        super.onStart();

        try
        {
            SQLiteOpenHelper helper = new MainActivity.BicycleDatabaseHelper(getActivity());
            db = helper.getReadableDatabase();

            cursor = db.query("ACTIVITIES", new String[]{"_id", "date", "distance", "time"},null,null,null,null,"date DESC");

            ActivitiesItemAdapter adapter = new ActivitiesItemAdapter(getActivity(), cursor);
            setListAdapter(adapter);


        }catch(SQLiteException e)
        {
            Toast.makeText(getActivity(), "Datebase is not accessable", Toast.LENGTH_SHORT).show();
        }
    }

    public void onListItemClick(ListView l, View v, int position, long id)
    {
        Intent intent = new Intent(getActivity(), DetailActivity.class);
        intent.putExtra(DetailActivity.ID,(int) id);

        startActivity(intent);
    }

    final public static class ActivitiesItemAdapter extends CursorAdapter
    {
        private LayoutInflater layoutInflater;
        private Context context;

        public ActivitiesItemAdapter(Context context, Cursor c) {
            super(context, c);
            this.context = context;
            layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent)
        {
            View v = layoutInflater.inflate(R.layout.activities_list_item, parent, false);
            return v;
        }

        @Override
        public void bindView(View v, Context context, Cursor c)
        {

            String date = c.getString(c.getColumnIndexOrThrow("date"));
            Double distance = c.getDouble(c.getColumnIndexOrThrow("distance"));
            Integer seconds = c.getInt(c.getColumnIndexOrThrow("time"));

                Double velocity = distance/seconds;
                velocity*=3.6;



            TextView date_label = (TextView) v.findViewById(R.id.list_text1);
            TextView distance_label = (TextView) v.findViewById(R.id.list_text2);
            TextView time_label = (TextView) v.findViewById(R.id.list_text3);
            TextView velocity_label = (TextView) v.findViewById(R.id.list_text4);

            date_label.setText(date);
            distance_label.setText(TrainingActivity.generateDistanceInKilometersNotation(distance));
            time_label.setText(TrainingActivity.generateTimeNotation(seconds));
            velocity_label.setText(TrainingActivity.generateVelocityNotation(velocity));
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();
        cursor.close();
        db.close();
    }

}
