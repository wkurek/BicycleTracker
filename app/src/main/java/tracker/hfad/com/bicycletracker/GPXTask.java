package tracker.hfad.com.bicycletracker;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class GPXTask
{


    public static class GenerateGPX extends AsyncTask<Void, Integer, String>
    {
        private Context context;
        private SQLiteDatabase database;

        private Integer trainingID;


        GenerateGPX(Context context, Integer id)
        {
            this.context = context;
            this.trainingID = id;
        }


        @Override
        protected void onPreExecute()
        {
            SQLiteOpenHelper heleper = new MainActivity.BicycleDatabaseHelper(context);
            database = heleper.getReadableDatabase();
        }

        @Override
        protected String doInBackground(Void... params)
        {

            Cursor cursor = database.query("ACTIVITIES", new String[]{"date", "track", "geopoints"}, "_id = ?",
                    new String[]{Integer.toString(trainingID)},null,null,null);

            if(cursor.moveToFirst())
            {
                try
                {
                    String encodedTrack, encodedJSONgeopoints, date;
                    List<LatLng> track;
                    JSONArray jsonArray;

                    date = cursor.getString(0);
                    encodedTrack = cursor.getString(1);
                    encodedJSONgeopoints = cursor.getString(2);

                    track = PolyUtil.decode(encodedTrack);
                    jsonArray = new JSONArray(encodedJSONgeopoints);


                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder parser =  factory.newDocumentBuilder();
                    Document xml = parser.newDocument();

                    for(int i = 0; i < jsonArray.length(); i+=3)
                    {
                        Element trkpt = xml.createElement("trkpt");
                        trkpt.setAttribute("lat", Double.toString(track.get(i/3).latitude));
                        trkpt.setAttribute("lon", Double.toString(track.get(i/3).longitude));

                        Element ele =  xml.createElement("ele");
                        ele.appendChild(xml.createTextNode(generateGPXDoubleValue(jsonArray.getDouble(i+2))));
                        trkpt.appendChild(ele);

                        Element time =  xml.createElement("time");
                        time.appendChild(xml.createTextNode(generateGPXDateFormat(date, jsonArray.getInt(i))));
                        trkpt.appendChild(time);

                        Element speed =  xml.createElement("speed");
                        speed.appendChild(xml.createTextNode(generateGPXDoubleValue(jsonArray.getDouble(i+1))));
                        trkpt.appendChild(speed);

                    }

                    TransformerFactory transformerfactory = TransformerFactory.newInstance();
                    Transformer transformer = transformerfactory.newTransformer();

                    DOMSource source = new DOMSource(xml);
                    StreamResult streamResult = new StreamResult(new File("f.xml"));
                    transformer.transform(source, streamResult);

                }
                catch(Exception exception)
                {
                    cursor.close();
                    return exception.getMessage();
                }
            }

            cursor.close();

            return context.getString(R.string.gpx_task_successful_generation_info);
        }

        @Override
        protected void onProgressUpdate(Integer... params)
        {

        }


        @Override
        protected void onPostExecute(String info)
        {
            Log.v("GPX:",info);

            database.close();
        }

        String generateGPXDateFormat(String startDate, int delay)
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

        String generateGPXDoubleValue(double value)
        {
            return Integer.toString((int) value);
        }

        String generateGPXFilename(int id)
        {
            return Integer.toString(id)+".xml";
        }


    }

}
