package tracker.hfad.com.bicycletracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class GoalsFragment extends Fragment {

    LinearLayout container;

    public GoalsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        return inflater.inflate(R.layout.fragment_goals, container, false);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        ImageButton imageView = (ImageButton) getView().findViewById(R.id.delete_goal);
        ImageButton add_button = (ImageButton) getView().findViewById(R.id.add_goal);
        imageView.setOnDragListener(new MyDragListener());

        add_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                LayoutInflater inflater = getActivity().getLayoutInflater();

                final View view = LayoutInflater.from(getActivity()).inflate(R.layout.new_goal_dialog, null);

                final Spinner spinner = (Spinner) view.findViewById(R.id.goal_spinner);

                ArrayList<String> list = new ArrayList<String>();
                list.add("during one training");
                list.add("in day");
                list.add("in week");
                list.add("in month");
                list.add("in year");
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.spiner_style,list);

                spinner.setAdapter(adapter);



                builder.setView(view)
                        .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id)
                            {
                                //Adding goal to database, table goals

                                EditText text =(EditText) view.findViewById(R.id.goal_max);

                                int type = spinner.getSelectedItemPosition();

                                try
                                {
                                    SQLiteOpenHelper helper = new MainActivity.BicycleDatabaseHelper(getActivity());
                                    SQLiteDatabase db = helper.getWritableDatabase();
                                    ContentValues contentValues = new ContentValues();

                                        String max = text.getText().toString();
                                        if(max.equals("") || max.equals("0")) max="1";

                                    contentValues.put("max", Integer.parseInt(max)*1000);
                                    contentValues.put("progress", 0);

                                    Calendar c = Calendar.getInstance();
                                    DateFormat dateFormat1 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");


                                    contentValues.put("type", type);

                                    if(type!=0)
                                    {
                                        switch (type)
                                        {
                                            case 1: c.add(Calendar.DAY_OF_YEAR, 1);
                                                break;
                                            case 2: c.add(Calendar.WEEK_OF_YEAR, 1);
                                                break;
                                            case 3: c.add(Calendar.MONTH, 1);
                                                break;
                                            case 4: c.add(Calendar.YEAR, 1);
                                                break;
                                        }

                                        contentValues.put("date", dateFormat1.format(c.getTime()));
                                    }
                                    else contentValues.put("date", "");

                                    contentValues.put("sync", 1);

                                    db.insert("GOALS", null, contentValues);

                                    db.close();

                                }catch (SQLiteException e)
                                {
                                    Toast.makeText(getActivity(), "Cannot reach database", Toast.LENGTH_SHORT).show();
                                }

                                refreshView();
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
        });

        container = (LinearLayout) getActivity().findViewById(R.id.content_container);
        refreshView();
    }




    public class MyLayout extends LinearLayout
    {
        int id; //referes to _id field in SQlite db in table GOALS

        public MyLayout(Context context, int id)
        {
            super(context);
            this.id = id;

            setOrientation(LinearLayout.VERTICAL);
        }

        void createView(int max,int progress, String date)
        {
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.single_goal, null);

            TextView label1 = (TextView) view.findViewById(R.id.goal_date);
            TextView label2 = (TextView) view.findViewById(R.id.goal_progress);

            ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.goal_progressbar);

            progressBar.setMax(max);
            progressBar.setProgress(progress);

                max/=1000;
                progress/=1000;

            if(!date.equals(""))  label1.setText("Active to " + date);

            label2.setText(Integer.toString(progress) + " / " + Integer.toString(max) + " km");


            setOnTouchListener(new MyTouchListener());

            this.addView(view);
            container.addView(this);
        }


        void createView(int max,int progress, int type)
        {
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.single_goal, null);

            TextView label1 = (TextView) view.findViewById(R.id.goal_date);
            TextView label2 = (TextView) view.findViewById(R.id.goal_progress);

            ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.goal_progressbar);

            progressBar.setMax(max);
            progressBar.setProgress(progress);

            max/=1000;
            progress/=1000;

            switch(type)
            {
                case 0: label1.setText("Achieved one training goal");
                        break;
                case 1: label1.setText("Achieved one day goal");
                        break;
                case 2: label1.setText("Achieved one week goal");
                        break;
                case 3: label1.setText("Achieved one month goal");
                        break;
                case 4: label1.setText("Achieved one year goal");
                        break;
            }


            label2.setText(Integer.toString(progress) + " / " + Integer.toString(max) + " km");


            setOnTouchListener(new MyTouchListener());

            this.addView(view);
            container.addView(this);
        }

        public int getId()
        {
            return  this.id;
        }

    }

    public void refreshView()
    {

        if(container.getChildCount() > 0)
        {
            container.removeAllViews();
        }

        try{
            SQLiteOpenHelper dbHelper = new MainActivity.BicycleDatabaseHelper(getActivity());
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor cursor = db.query("GOALS", new String[] {"_id", "max", "progress", "date", "type", "sync"}, "sync = ? OR sync = ?", new String[] {Integer.toString(0), Integer.toString(1)}, null, null, "_id DESC");

            while(cursor.moveToNext()) {

                Integer id = cursor.getInt(0);
                Date record_date;
                boolean isGoalActual = true;

                int max = cursor.getInt(1);
                int progress = cursor.getInt(2);
                int type = cursor.getInt(4);

                if(type!=0)
                {
                    String date = cursor.getString(3);
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    record_date = dateFormat.parse(date);

                    isGoalActual = !isGoalExpired(record_date);
                }


                if (progress >= max && isGoalActual) {
                    //goal achieved

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                    final View view = LayoutInflater.from(getActivity()).inflate(R.layout.basic_dialog, null);

                    TextView text_field =(TextView) view.findViewById(R.id.basic_dialog_text);
                    text_field.setText("Congratulation!\nYou have achieved a new goal!");

                    builder.setView(view)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    Dialog dialog = builder.create();
                    dialog.show();

                    ContentValues contentValues = new ContentValues();
                    contentValues.put("sync", 4);
                    db.update("GOALS", contentValues, "_id = ?", new String[]{id.toString()});

                    MyLayout layout = new MyLayout(getActivity(), id);
                    layout.createView(cursor.getInt(1), cursor.getInt(2), cursor.getInt(4));

                } else if (isGoalActual || type == 0) {
                    //goals to display

                    MyLayout layout = new MyLayout(getActivity(), id);
                    layout.createView(cursor.getInt(1), cursor.getInt(2), cursor.getString(3));

                } else if (cursor.getInt(5) == 4) {
                    //achieved goals to display

                    MyLayout layout = new MyLayout(getActivity(), id);
                    layout.createView(cursor.getInt(1), cursor.getInt(2), cursor.getInt(4));
                } else {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("sync", 2);

                    db.update("GOALS", contentValues, "_id = ?", new String[]{id.toString()});
                }
            }

                cursor.close();
                db.close();

        } catch(SQLiteException e)
        {
            Toast toast = Toast.makeText(getActivity(), "Database is not accessable", Toast.LENGTH_SHORT);
            toast.show();
        }
        catch(ParseException e)
        {
            Toast toast = Toast.makeText(getActivity(), "Parse exception", Toast.LENGTH_SHORT);
            toast.show();
        }

    }

    private final class MyTouchListener implements View.OnTouchListener
    {

        @Override
        public boolean onTouch(View view, MotionEvent event)
        {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                ClipData data = ClipData.newPlainText("", "");
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                view.startDrag(data, shadowBuilder, view, 0);
                view.setVisibility(View.INVISIBLE);

                return true;

            }else
            {
                return false;
            }
        }
    }

    class MyDragListener implements View.OnDragListener {


        @Override
        public boolean onDrag(View v, DragEvent event) {
            int action = event.getAction();
            ImageButton imageButton;
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    // do nothing
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:

                    imageButton = (ImageButton) v;
                    imageButton.setImageResource(R.drawable.delete_white_drop);

                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    imageButton = (ImageButton) v;
                    imageButton.setImageResource(R.drawable.delete_white);
                    break;
                case DragEvent.ACTION_DROP:
                    MyLayout box = (MyLayout) event.getLocalState();
                    Integer id = box.getId();
                    updateRecord(id);
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    refreshView();
                    imageButton = (ImageButton) v;
                    imageButton.setImageResource(R.drawable.delete_white);
                default:
                    break;
            }
            return true;
        }
    }

    void updateRecord(Integer id)
    {
        final int ID = id;

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.basic_dialog, null);

        TextView text =(TextView) view.findViewById(R.id.basic_dialog_text);
        text.setText("Do you want to delete this goal?");

        builder.setView(view)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {

                        SQLiteOpenHelper dbHelper = new MainActivity.BicycleDatabaseHelper(getActivity());
                        SQLiteDatabase db = dbHelper.getWritableDatabase();

                        ContentValues values = new ContentValues();
                        values.put("sync", 2);

                        db.update("GOALS", values, "_id = ?", new String[] {Integer.toString(ID)});
                        db.close();

                        refreshView();

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

    boolean isGoalExpired(Date record_date)
    {
        return (new Date().after(record_date));
    }

}
