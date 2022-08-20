package ca.bcit.psychopass;

import com.google.android.gms.maps.model.LatLng;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static java.lang.Math.abs;

public class DataAnalysis {

    private static final double RADIUS_LONGTITUDE = 0.0025;
    private static final double RADIUS_LATITUDE = 0.00125;

    private double Longitude;
    private double Latitude;
    private List<Crime> crimeList;

    DataAnalysis(){
        //initialize with a default location in maple ridge
        Longitude = -122.6039533;
        Latitude = 49.2178709;
        Crime sample = new Crime();
        crimeList = new ArrayList<Crime>();
        crimeList.add(sample);
    }

    DataAnalysis(LatLng location, List<Crime> crimeList){
        this.Longitude = location.longitude;
        this.Latitude = location.latitude;
        this.crimeList = crimeList;
    }

    public ArrayList<Crime> getNearbyCrime() {
        ArrayList<Crime> tempList = new ArrayList<Crime>();
        for(int i=0; i<crimeList.size(); ++i){
            Crime c = crimeList.get(i);
            if(abs(c.getLongitude() - this.Longitude) < RADIUS_LONGTITUDE && abs(c.getLatitude() - this.Latitude) < RADIUS_LATITUDE){
                tempList.add(c);
            }
        }
        sortCimeList(tempList);

        return tempList;
    }

    public void sortCimeList(ArrayList<Crime> crimeList){

        Collections.sort(crimeList,new Comparator<Crime>() {
            @Override
            public int compare(Crime c1, Crime c2) {
                return c2.getReportedTime().compareTo(c1.getReportedTime());
            }
        });

    }


    public boolean isDangerZone() {

        //danger zone is the location where crime number exceeds 200
        int threshold = 200;

        ArrayList<Crime> list = getNearbyCrime();
        return (list.size()> threshold);
    }

}
