package tracker.hfad.com.bicycletracker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.gms.maps.model.LatLng;
import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class GPXTask
{

    public static class GenerateGPX extends AsyncTask<Void, Integer, String>
    {
        private Context context;
        private Integer id;
        private String date;
        private List<LatLng> track;
        private JSONArray jsonArray;

        private View view;
        private Dialog dialog;
        private ProgressBar progressBar;


        GenerateGPX(Context context, Integer id, List<LatLng> track , String encodedJSONgeopoints, String date)
        {
            this.context = context;
            this.id = id;
            this.track = track;
            this.date = date;

            try
            {
                this.jsonArray = new JSONArray(encodedJSONgeopoints);
            }
            catch(org.json.JSONException exception)
            {
                 jsonArray = null;
            }
        }


        @Override
        protected void onPreExecute()
        {
            //Create Dialog

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            view = LayoutInflater.from(context).inflate(R.layout.training_import_dialog, null);

            TextView loadingText = (TextView) view.findViewById(R.id.training_import_dialog_progress_text);
            loadingText.setText(R.string.gpx_task_generation_info);

            builder.setView(view)
                    .setPositiveButton(R.string.positive_reaction, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            cancel(true);
                            dialog.cancel();
                        }
                    })
                    .setNegativeButton(R.string.negative_reaction, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            cancel(true);
                            dialog.cancel();
                        }
                    });

            dialog = builder.create();
            dialog.show();
        }

        @Override
        protected String doInBackground(Void... params)
        {
            if(jsonArray == null) return context.getString(R.string.gpx_task_parse_exception);

            publishProgress(jsonArray.length());

            GPXParser.Element gpx = new GPXParser.Element("gpx", "");

                GPXParser.Parameter creator = new GPXParser.Parameter("creator", context.getString(R.string.app_name));

            gpx.appendParameter(creator);

                GPXParser.Element metadata = createMetadataSection(generateGPXTrackName(), generateGPXDateFormat(date, 0));
                GPXParser.Element trk = createTrkSection(generateGPXTrackName());

            if(metadata == null || trk == null) return context.getString(R.string.gpx_task_parse_exception);

            gpx.appendChild(metadata);
            gpx.appendChild(trk);


            if(createGPXFile(gpx)) return context.getString(R.string.gpx_task_successful_generation_info);
            else return context.getString(R.string.gpx_task_parse_exception);
        }

        @Override
        protected void onProgressUpdate(Integer... params)
        {
            if(progressBar == null)
            {
                progressBar = (ProgressBar) view.findViewById(R.id.training_import_dialog_progress_bar);
                progressBar.setMax(params[0]);

            }
            else
            {
                progressBar.setProgress(params[0]);
            }
        }

        @Override
        protected void onCancelled(String result)
        {
            if(progressBar == null)
            {
                progressBar = (ProgressBar) view.findViewById(R.id.training_import_dialog_progress_bar);
            }

            progressBar.setVisibility(View.GONE);

            TextView progressText = (TextView) view.findViewById(R.id.training_import_dialog_progress_text);
            progressText.setVisibility(View.GONE);

            TextView generationInfo = (TextView) view.findViewById(R.id.training_import_dialog_server_info);
            generationInfo.setText(result);

        }


        @Override
        protected void onPostExecute(String info)
        {
            if(progressBar == null)
            {
                progressBar = (ProgressBar) view.findViewById(R.id.training_import_dialog_progress_bar);
            }

            progressBar.setVisibility(View.GONE);

            TextView progressText = (TextView) view.findViewById(R.id.training_import_dialog_progress_text);
            progressText.setVisibility(View.GONE);

            TextView generationInfo = (TextView) view.findViewById(R.id.training_import_dialog_server_info);
            generationInfo.setText(info);

        }

        static String generateGPXDateFormat(String startDate, int delay)
        {
            SimpleDateFormat simpleDateFormatInput = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            SimpleDateFormat simpleDateFormatOutput = new SimpleDateFormat("dd-MM-yyyy'T'HH:mm:ss'Z'");

            Date date;

            try
            {
                date = simpleDateFormatInput.parse(startDate);
                date.setTime(date.getTime() + (delay*1000));
            }
            catch(java.text.ParseException exception)
            {
                Log.v("GPX generation: ", "date parse exception");

                date = new Date();
                return simpleDateFormatOutput.format(date);
            }

            return simpleDateFormatOutput.format(date);
        }

        private String generateGPXDoubleValue(double value)
        {
            return Integer.toString((int) value);
        }

        private String generateGPXTrackName()
        {
            return "Track "+this.id;
        }

        private GPXParser.Element createMetadataSection(String trackName, String trackTime)
        {
            GPXParser.Element metadata = new GPXParser.Element("metadata", "");

                GPXParser.Element name = new GPXParser.Element("name", trackName);
                GPXParser.Element time = new GPXParser.Element("time", trackTime);

            metadata.appendChild(name);
            metadata.appendChild(time);

            return metadata;
        }

        private GPXParser.Element createTrkSection(String trackName)
        {
            GPXParser.Element trk = new GPXParser.Element("trk", "");

                GPXParser.Element name = new GPXParser.Element("name", trackName);
                GPXParser.Element number = new GPXParser.Element("number", this.id.toString());

                GPXParser.Element trkseg = new GPXParser.Element("trkseg", "");

                for(int i = 0; i < jsonArray.length(); i+=3)
                {
                    GPXParser.Element trkpt = createTrkptSection(i);
                    trkseg.appendChild(trkpt);
                }

            trk.appendChild(name);
            trk.appendChild(number);
            trk.appendChild(trkseg);

            return trk;
        }

        private GPXParser.Element createTrkptSection(int i)
        {
            GPXParser.Element trkpt = new GPXParser.Element("trkpt", "");

                GPXParser.Parameter lat = new GPXParser.Parameter("lat", Double.toString(track.get(i/3).latitude));
                GPXParser.Parameter lon = new GPXParser.Parameter("lon", Double.toString(track.get(i/3).longitude));

            trkpt.appendParameter(lat);
            trkpt.appendParameter(lon);

                GPXParser.Element ele, time, speed;

                try
                {
                    ele = new GPXParser.Element("ele", generateGPXDoubleValue(jsonArray.getDouble(i+2)));
                    time = new GPXParser.Element("time", generateGPXDateFormat(date, jsonArray.getInt(i)));
                    speed = new GPXParser.Element("speed", generateGPXDoubleValue(jsonArray.getDouble(i+1)));
                }
                catch(org.json.JSONException exception)
                {
                    return null;
                }

            trkpt.appendChild(ele);
            trkpt.appendChild(time);
            trkpt.appendChild(speed);

            return trkpt;
        }

        private boolean createGPXFile(GPXParser.Element gpx)
        {
            String state = Environment.getExternalStorageState();
            if (!Environment.MEDIA_MOUNTED.equals(state)) {return false;}

            try
            {
                File file = new File(Environment.getExternalStorageDirectory(), "track.gpx");
                FileOutputStream f = new FileOutputStream(file);
                PrintWriter pw = new PrintWriter(f);
                pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                pw.println(gpx.toGPX());
                pw.flush();
                pw.close();
                f.close();
            }
            catch(java.io.IOException exception)
            {
                return false;
            }

            return true;
        }

    }
}
