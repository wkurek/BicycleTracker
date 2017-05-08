package tracker.hfad.com.bicycletracker;


import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.imanoweb.calendarview.CalendarListener;
import com.imanoweb.calendarview.CustomCalendarView;
import com.imanoweb.calendarview.DayDecorator;
import com.imanoweb.calendarview.DayView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class CallendarFragment extends Fragment {

    CustomCalendarView calendarView;

    private class DaysDecorator implements DayDecorator
    {

        @Override
        public void decorate(DayView dayView) {

            SQLiteDatabase db;

            try{
                SQLiteOpenHelper helper = new MainActivity.BicycleDatabaseHelper(getActivity());
                db = helper.getReadableDatabase();

                Date date = dayView.getDate();
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

                if(!((date).after(new Date())))
                {
                    String day = dateFormat.format(date);
                    Cursor cursor = db.query("ACTIVITIES", new String[]{"date"},"date LIKE ?", new String[]{"%"+ day +"%"}, "DATE", null, null);

                    if(cursor.moveToFirst())
                    {
                        dayView.setBackgroundColor(Color.parseColor("#f3f7f9"));
                    }

                    cursor.close();
                }

                db.close();

            }catch(SQLiteException e)
            {
                Toast.makeText(getActivity(), "Database is not accessable", Toast.LENGTH_SHORT).show();
            }
        }
    }


    public CallendarFragment() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_callendar, container, false);
    }


    @Override
    public void onStart()
    {
        super. onStart();

        calendarView = (CustomCalendarView) getActivity().findViewById(R.id.calendar_view);
        Calendar currentCalendar = Calendar.getInstance(Locale.getDefault());
        calendarView.setFirstDayOfWeek(Calendar.MONDAY);
        calendarView.setShowOverflowDate(false);

        calendarView.setCalendarListener(new CalendarListener() {
            @Override
            public void onDateSelected(Date date)
            {
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

                final String selectedDate = dateFormat.format(date);

                Intent intent = new Intent(getActivity(), DateListActivity.class);
                intent.putExtra(DateListActivity.RODZAJ, selectedDate);
                startActivity(intent);

            }

            @Override
            public void onMonthChanged(Date date) {

            }
        });

        List decorators = new ArrayList<>();
        decorators.add(new DaysDecorator());
        calendarView.setDecorators(decorators);
        calendarView.refreshCalendar(currentCalendar);

    }
}

