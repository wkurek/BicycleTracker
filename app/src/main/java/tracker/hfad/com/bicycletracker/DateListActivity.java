package tracker.hfad.com.bicycletracker;


import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class DateListActivity extends ListActivity {

    public static final String RODZAJ = "rodzaj";

    private SQLiteDatabase db;
    private Cursor  cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onStart()
    {
        super.onStart();


        try
        {

            SQLiteOpenHelper helper = new MainActivity.BicycleDatabaseHelper(this);
            db = helper.getReadableDatabase();

            cursor = db.query("ACTIVITIES", new String[]{"_id", "date", "distance", "time"}, "date LIKE ?", new String[]{"%"+getIntent().getExtras().get(RODZAJ)+"%"},null,null,"date DESC");

            ArcheivesListFragment.ActivitiesItemAdapter adapter = new ArcheivesListFragment.ActivitiesItemAdapter(this, cursor);
            setListAdapter(adapter);

        }catch(SQLiteException e)
        {
            Toast.makeText(this, "Database is not accessable", Toast.LENGTH_SHORT).show();
        }


    }

    public void onListItemClick(ListView l, View v, int position, long id)
    {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(DetailActivity.ID,(int) id);

        startActivity(intent);
    }


    @Override
    public void onDestroy()
    {
        cursor.close();
        db.close();

        super.onDestroy();
    }

}
