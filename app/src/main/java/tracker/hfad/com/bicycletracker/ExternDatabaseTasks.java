package tracker.hfad.com.bicycletracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;


public class ExternDatabaseTasks
{

    public static class ImportTrainingDataTask extends AsyncTask<Void, Integer, String>
    {
        private Context context;
        private Double distance;
        private Integer id, finished, seconds, calories, discipline;
        private String encodedLatLngList, encodedGeoPoints, date;

        private String url_training = "http://rower.lowicz.com.pl/BicycleTracker%20www/mobile_training.php";

        private boolean importFinished = false;
        private Dialog importDialog;
        private View view;


        ImportTrainingDataTask(Context context, int seconds, double distance, int calories, String encodedLatLngList,
                             String encodedGeoPoints, int finished, String date)
        {
            this.context = context;
            this.seconds = seconds;
            this.distance = distance;
            this.calories = calories;
            this.encodedLatLngList = encodedLatLngList;
            this.encodedGeoPoints = encodedGeoPoints;
            this.finished = finished;
            this.date = date;
        }

        @Override
        protected void onPreExecute()
        {
            id = SaveSharedPreference.getUserID(context);
            discipline = SaveSharedPreference.getUserActivity(context);


            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
            view = LayoutInflater.from(context).inflate(R.layout.training_import_dialog, null);

            TextView progressText = (TextView) view.findViewById(R.id.training_import_dialog_progress_text);
            progressText.setText(R.string.import_task_progress_text);


            dialogBuilder.setView(view)
                    .setPositiveButton(R.string.positive_reaction, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            if(importFinished) dialog.cancel();
                            cancel(true);
                        }
                    })
                    .setNegativeButton(R.string.negative_reaction, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            if(importFinished) dialog.cancel();
                            cancel(true);
                        }
                    });

            importDialog = dialogBuilder.create();
            importDialog.show();
        }

        @Override
        protected String doInBackground(Void... params)
        {
            if(isCancelled()) importDialog.cancel();

            encodedLatLngList = encodedLatLngList.replaceAll("\\\\", "ab_cd");

            try
            {
                String data = URLEncoder.encode("id", "UTF-8")+"="+URLEncoder.encode(id.toString(), "UTF-8")+"&"+
                        URLEncoder.encode("time", "UTF-8")+"="+URLEncoder.encode(seconds.toString(), "UTF-8")+"&"+
                        URLEncoder.encode("distance", "UTF-8")+"="+URLEncoder.encode(distance.toString(), "UTF-8")+"&"+
                        URLEncoder.encode("calories", "UTF-8")+"="+URLEncoder.encode(calories.toString(), "UTF-8")+"&"+
                        URLEncoder.encode("date", "UTF-8")+"="+URLEncoder.encode(date, "UTF-8")+"&"+
                        URLEncoder.encode("track", "UTF-8")+"="+URLEncoder.encode(encodedLatLngList, "UTF-8")+"&"+
                        URLEncoder.encode("geopoints", "UTF-8")+"="+URLEncoder.encode(encodedGeoPoints, "UTF-8")+"&"+
                        URLEncoder.encode("discipline", "UTF-8")+"="+URLEncoder.encode(discipline.toString(), "UTF-8")+"&"+
                        URLEncoder.encode("finished", "UTF-8")+"="+URLEncoder.encode(finished.toString(), "UTF-8");


                URL url = new URL(url_training);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);

                OutputStream outputStream = httpURLConnection.getOutputStream();
                BufferedWriter bufferedWriter= new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));

                bufferedWriter.write(data);
                bufferedWriter.flush();

                bufferedWriter.close();
                outputStream.close();

                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "iso-8859-1"));

                String response="",line;

                while ((line = bufferedReader.readLine()) != null)
                {
                    response += line;
                }

                bufferedReader.close();
                inputStream.close();
                httpURLConnection.disconnect();

                return response;

            }catch (IOException e)
            {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values)
        {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String response)
        {
            String serverInfo;

            switch (response)
            {
                case "OK": serverInfo = context.getString(R.string.import_task_server_ok); break;
                case "ERROR_1": serverInfo = context.getString(R.string.import_task_server_error_1); break;
                case "ERROR_2": serverInfo = context.getString(R.string.import_task_server_error_2); break;
                default: serverInfo = context.getString(R.string.import_task_server_error_0); break;
            }

            ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.training_import_dialog_progress_bar);
            progressBar.setVisibility(View.GONE);

            TextView progressText = (TextView) view.findViewById(R.id.training_import_dialog_progress_text);
            progressText.setVisibility(View.GONE);

            TextView serverText = (TextView) view.findViewById(R.id.training_import_dialog_server_info);
            serverText.setText(serverInfo);

        }

    }

    public static class ImportDataFromServerTask extends AsyncTask<Void, Integer, String>
    {
        private Context context;
        private String url_training_data = "http://rower.lowicz.com.pl/BicycleTracker%20www/mobile_data_transfer.php";
        private JSONArray jsonArray;
        private Integer id;

        private View view;
        private Dialog transferDialog;
        private boolean capableToStop;

        private SQLiteDatabase sqLiteDatabase;

        ImportDataFromServerTask(Context context)
        {
            this.context = context;
            this.id = SaveSharedPreference.getUserID(context);

            jsonArray = null;
            capableToStop = false;
        }

        @Override
        protected void onPreExecute()
        {
            SQLiteOpenHelper helper = new MainActivity.BicycleDatabaseHelper(context);
            sqLiteDatabase = helper.getWritableDatabase();

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
            view = LayoutInflater.from(context).inflate(R.layout.training_import_dialog, null);

            TextView progressText = (TextView) view.findViewById(R.id.training_import_dialog_progress_text);
            progressText.setText(R.string.transfer_task_transering_info);


            dialogBuilder.setView(view)
                    .setPositiveButton(R.string.positive_reaction, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            if(capableToStop) dialog.cancel();
                            cancel(true);
                        }
                    })
                    .setNegativeButton(R.string.negative_reaction, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            if(capableToStop) dialog.cancel();
                            cancel(true);
                        }
                    });

            transferDialog = dialogBuilder.create();
            transferDialog.show();
        }

        @Override
        protected String doInBackground(Void... params)
        {
            if(sqLiteDatabase == null) return context.getString(R.string.transfer_task_error);

            try
            {
                String jsonStringData = transferDataFromServer();

                if(jsonStringData == null) throw new org.json.JSONException(
                        context.getString(R.string.transfer_task_error));

                if(jsonStringData.equals("ERROR_0") || jsonStringData.equals("ERROR_1"))
                    return context.getString(R.string.transfer_task_error);

                jsonArray = new JSONArray(jsonStringData);

                if(!addNewRecordsToDatabase()) throw new SQLiteException(
                        context.getString(R.string.transfer_task_error));

            }
            catch(org.json.JSONException exception)
            {
                return exception.getMessage();
            }
            catch(SQLiteException exception)
            {
                return exception.getMessage();
            }

            return context.getString(R.string.transfer_task_success_transfer_info);
        }

        @Override
        protected void onPostExecute(String response)
        {
            TextView progressText = (TextView) view.findViewById(R.id.training_import_dialog_progress_text);
            progressText.setVisibility(View.GONE);

            ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.training_import_dialog_progress_bar);
            progressBar.setVisibility(View.GONE);

            TextView serverText = (TextView) view.findViewById(R.id.training_import_dialog_server_info);
            serverText.setText(response);

            capableToStop = true;
        }

        @Override
        protected void onCancelled(String response)
        {
            TextView progressText = (TextView) view.findViewById(R.id.training_import_dialog_progress_text);
            progressText.setVisibility(View.GONE);

            ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.training_import_dialog_progress_bar);
            progressBar.setVisibility(View.GONE);

            TextView serverText = (TextView) view.findViewById(R.id.training_import_dialog_server_info);
            serverText.setText(R.string.transfer_task_cancelled);

            capableToStop = true;
        }

        private String transferDataFromServer()
        {
            try
            {
                String data = URLEncoder.encode("id", "UTF-8")+"="+URLEncoder.encode(id.toString(), "UTF-8");

                URL url = new URL(url_training_data);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);

                OutputStream outputStream = httpURLConnection.getOutputStream();
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));

                bufferedWriter.write(data);
                bufferedWriter.flush();

                bufferedWriter.close();
                outputStream.close();

                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "iso-8859-1"));

                String jsonData="",line;

                while ((line = bufferedReader.readLine()) != null)
                {
                    jsonData += line;
                }

                bufferedReader.close();
                inputStream.close();
                httpURLConnection.disconnect();

                return jsonData;

            }
            catch (IOException e)
            {
                return null;
            }
        }

        private boolean addNewRecordsToDatabase()
        {
            try
            {
                for(int i=0; i < jsonArray.length(); i+=8)
                {
                    if(isCancelled()) return true;

                    Integer time = jsonArray.getInt(i);
                    String date = jsonArray.getString(i+3);

                    if(existsInLocalDatabase(time, date)) continue;

                    ContentValues activityValues = new ContentValues();

                        activityValues.put("time", time);
                        activityValues.put("distance", jsonArray.getDouble(i+1));
                        activityValues.put("calories", jsonArray.getInt(i+2));
                        activityValues.put("date", date);
                        activityValues.put("track", jsonArray.getString(i+4));
                        activityValues.put("geopoints", jsonArray.getString(i+5));
                        activityValues.put("discipline", jsonArray.getInt(i+6));
                        activityValues.put("finished", jsonArray.getInt(i+7));


                    if(sqLiteDatabase.insert("ACTIVITIES", null, activityValues) < 0) return false;
                }

            }
            catch(org.json.JSONException exception)
            {
                return false;
            }

            return true;
        }

        private boolean existsInLocalDatabase(Integer time, String date)
        {
            Cursor cursor = sqLiteDatabase.query("ACTIVITIES", new String[]{"_id"}, "time=? AND date=?",
                    new String[]{time.toString(), date}, null, null, null);

            int recordsNumber  = cursor.getCount();

            cursor.close();

            return (recordsNumber > 0);
        }
    }
}
