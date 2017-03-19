package tracker.hfad.com.bicycletracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
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

    public static class LogTask extends AsyncTask<Void, Void, String>
    {
        private Activity context;
        private String login, password;

        private String login_url = "http://rower.lowicz.com.pl/BicycleTracker%20www/mobile_log.php";

        LogTask(Activity context, String login, String password)
        {
            this.context = context;
            this.login = login;
            this.password = password;
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected String doInBackground(Void... params)
        {
            String response = "";

            try
            {
                //Send data to server

                String data = URLEncoder.encode("login", "UTF-8")+"="+URLEncoder.encode(login, "UTF-8")+"&"+
                        URLEncoder.encode("password", "UTF-8")+"="+URLEncoder.encode(password, "UTF-8");

                URL url = new URL(login_url);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);
                OutputStream outputStream = httpURLConnection.getOutputStream();
                BufferedWriter bufferedWriter= new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));

                bufferedWriter.write(data);
                bufferedWriter.flush();

                bufferedWriter.close();
                outputStream.close();

                //Chceck the server answer

                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "iso-8859-1"));

                String line;

                while ((line = bufferedReader.readLine()) != null)
                {
                    response += line;
                }

                bufferedReader.close();
                inputStream.close();
                httpURLConnection.disconnect();


            }catch (IOException e)
            {
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onProgressUpdate(Void... values)
        {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String response)
        {
            if((!response.equals("error")) && (!response.equals("")))
            {
                SaveSharedPreference.setUserID(context,Integer.parseInt(response));

                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);
                context.finish();

            }
            else
            {
                Toast.makeText(context, "Incorrect login or password. Try again!", Toast.LENGTH_LONG).show();
            }

        }

    }

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
}
