package tracker.hfad.com.bicycletracker;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class PersonalBestFragment extends Fragment {


    public PersonalBestFragment() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_personal_best, container, false);
    }

    @Override
    public void onStart()
    {
        super.onStart();

        //Personal records

        SQLiteDatabase db;
        Cursor cursor;

        View view = getView();

        SQLiteOpenHelper helper =  new MainActivity.BicycleDatabaseHelper(getActivity());
        db = helper.getReadableDatabase();

        cursor = db.query("ACTIVITIES", new String[]{"MAX(distance) AS distance", "MAX(time) AS time", "MAX(calories) AS calories"}, null, null, null, null, null);

        if(cursor.moveToFirst())
        {
            double distance = cursor.getDouble(0);
            int seconds = cursor.getInt(1);
            int calories = cursor.getInt(2);

            TextView distance_record_label = (TextView) view.findViewById(R.id.personal_record_distance);
            TextView time_record_label = (TextView) view.findViewById(R.id.personal_record_time);
            TextView calories_record_label = (TextView) view.findViewById(R.id.personal_record_calories);

            distance_record_label.setText(TrainingActivity.generateDistanceInKilometersNotation(distance));
            time_record_label.setText(TrainingActivity.generateTimeNotation(seconds));
            calories_record_label.setText(TrainingActivity.generateCaloriesNotation(calories));

        }

        cursor.close();
        db.close();


        //Table

        Spinner table_spinner = (Spinner) view.findViewById(R.id.personal_table_spinner);

        List<String> type_list = new LinkedList<>();
        type_list.add("day");
        type_list.add("month");
        type_list.add("year");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.spiner_small_style, type_list);
        table_spinner.setAdapter(adapter);

        table_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                DateFormat dateFormat;

                if(position==0)
                {
                    dateFormat = new SimpleDateFormat("yyyy/MM/dd");
                }
                else if(position==1)
                {
                    dateFormat = new SimpleDateFormat("yyyy/MM");
                }
                else
                {
                    dateFormat = new SimpleDateFormat("yyyy");
                }


                try
                {
                    SQLiteOpenHelper table_helper = new MainActivity.BicycleDatabaseHelper(getActivity());
                    SQLiteDatabase database = table_helper.getReadableDatabase();

                    Date date = new Date();
                    final String ActualDate = dateFormat.format(date);


                    Cursor cursor = database.query("ACTIVITIES", new String[]{"COUNT(_id) AS count, SUM(distance) AS distance, SUM(time) AS seconds_number, SUM(calories) AS calories"}, "DATE LIKE ?", new String[]{"%"+ActualDate+"%"},null,null,null);

                    if(cursor.moveToFirst())
                    {
                        //filling the table
                        int trainingsNumber = cursor.getInt(0);
                        double distance = cursor.getDouble(1);
                        int seconds = cursor.getInt(2);
                        int calories = cursor.getInt(3);

                        TextView table_number_of_trainings = (TextView) getActivity().findViewById(R.id.personal_table_number);
                        TextView table_distance = (TextView) getActivity().findViewById(R.id.personal_table_distance);
                        TextView table_time = (TextView) getActivity().findViewById(R.id.personal_table_time);
                        TextView table_calories = (TextView) getActivity().findViewById(R.id.personal_table_calories);

                        table_time.setText(TrainingActivity.generateTimeNotation(seconds));
                        table_number_of_trainings.setText(generateNumberOfTrainingsNotation(trainingsNumber));
                        table_distance.setText(TrainingActivity.generateDistanceInKilometersNotation(distance));
                        table_calories.setText(TrainingActivity.generateCaloriesNotation(calories));
                    }

                    cursor.close();
                    database.close();

                }catch (SQLiteException e)
                {
                    Toast.makeText(getActivity(), "Database is not accessable", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    public static String generateNumberOfTrainingsNotation(int number)
    {
        return Integer.toString(number)+" tracked";
    }

}
