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

    LinearLayout goalsContainer;

    public GoalsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_goals, container, false);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        ImageButton imageView = (ImageButton) getView().findViewById(R.id.delete_goal);
        ImageButton add_button = (ImageButton) getView().findViewById(R.id.add_goal);
        imageView.setOnDragListener(new MyDragListener());

        add_button.setOnClickListener(new View.OnClickListener() {//TO CHANGE!!!!
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


    }




    public class SingleGoalView extends LinearLayout
    {
        private int id;

        public SingleGoalView(Context context, int id)
        {
            super(context);
            this.id = id;

            setOrientation(LinearLayout.VERTICAL);
        }

        void createSingleGoalView(int max, int progress, String finishDate)
        {
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.single_goal, null);

            if(view == null || goalsContainer == null) return;

            TextView finishDateField = (TextView) view.findViewById(R.id.single_goal_finish_date);
            TextView progressField = (TextView) view.findViewById(R.id.single_goal_progress);
            ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.single_goal_progressbar);

            if(progress > max) progress = max;

            progressBar.setMax(max);
            progressBar.setProgress(progress);

            finishDateField.setText(getString(R.string.single_goal_finish_date_description, finishDate));
            progressField.setText(getString(R.string.single_goal_progress_description, progress, max));

            setOnTouchListener(new MyTouchListener());

            this.addView(view);
            goalsContainer.addView(this);
        }

        public int getId()
        {
            return  this.id;
        }
    }

    public void refreshGoalsContainer()
    {
        if(goalsContainer == null) return;

        if(goalsContainer.getChildCount() > 0)
        {
            goalsContainer.removeAllViews();
        }

        //ASCYNC TASK, ADD SINGLE GOALS TO CONTAINER


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
                    SingleGoalView box = (SingleGoalView) event.getLocalState();
                    Integer id = box.getId();
                    deleteSingleGoal(id);
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    refreshGoalsContainer();
                    imageButton = (ImageButton) v;
                    imageButton.setImageResource(R.drawable.delete_white);
                default:
                    break;
            }
            return true;
        }
    }

    void deleteSingleGoal(Integer id)
    {

        //ASCYNC TASK, DELETE SINGLE GOAL

    }

    boolean isGoalExpired(Date record_date)
    {
        return (new Date().after(record_date));
    }

}
