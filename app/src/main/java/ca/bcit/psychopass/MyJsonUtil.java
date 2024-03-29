package ca.bcit.psychopass;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class MyJsonUtil {
    private static final String TAG = "MyJsonUtil";
    public static List<Crime> crimeList = new ArrayList<Crime>();

    Context mContext;
    Activity v;


    public MyJsonUtil(Activity v, Context context) {
        this.mContext=context;
        this.v = v;
    }

    public void parseLocalJSON(){
        String jsonStr;

        try {
            InputStream is = mContext.getAssets().open("Property_Crimes.geojson");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            jsonStr = new String(buffer, "UTF-8");
            crimeList = getAllCrimeObj(jsonStr);

        } catch (IOException e) {
            Log.e(TAG, "Error reading JSON file:" + e.getMessage());
        }
    }

    public List<Crime> getAllCrimeObj(String jsonStr) {
        List<Crime> crimeList = new ArrayList<Crime>();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

        try {
            // Getting JSON Array node
            JSONObject dataObj = new JSONObject(jsonStr);
            JSONArray crimeDataArray = dataObj.getJSONArray("features");

            // looping through All countries
            for (int i = 0; i < crimeDataArray.length(); i++) {
                JSONObject geometry = crimeDataArray.getJSONObject(i).getJSONObject("geometry");
                JSONObject properties = crimeDataArray.getJSONObject(i).getJSONObject("properties");

                Double Longitude = geometry.getJSONArray("coordinates").getDouble(0);
                Double Latitude = geometry.getJSONArray("coordinates").getDouble(1);
                String City = properties.getString("City");
                String OccuranceYear = properties.getString("OccuranceYear");
                String ReportedTime = properties.getString("ReportedTime");
                String ReportedTimeSub = ReportedTime.substring(0,23);
                String ReportedWeekday = properties.getString("ReportedTime");
                String StreetName = properties.getString("StreetName");
                String Offense = properties.getString("Offense");
                String OffenseCategory = properties.getString("OffenseCategory");
                String ReportedDateText = properties.getString("ReportedDateText");
                String HouseNumber = properties.getString("HouseNumber");

                Crime crime = new Crime();

                // adding each child node to HashMap key => value
                crime.setLongitude(Longitude);
                crime.setLatitude(Latitude);
                crime.setCity(City);
                crime.setHouseNumber(HouseNumber);
                crime.setOccuranceYear(OccuranceYear);
                try {
                    crime.setReportedTime(df.parse(ReportedTimeSub));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                crime.setReportedWeekday(ReportedWeekday);
                crime.setStreetName(StreetName);
                crime.setOffense(Offense);
                crime.setOffenseCategory(OffenseCategory);
                crime.setReportedDateText(ReportedDateText);

                // adding contact to contact list
                crimeList.add(crime);
            }
        } catch (final JSONException e) {
            Log.e(TAG, "Json parsing error: " + e.getMessage());
            v.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext,
                            "Json parsing error: " + e.getMessage(),
                            Toast.LENGTH_LONG)
                            .show();
                }
            });

        } finally {
            return crimeList;
        }

    }

}
