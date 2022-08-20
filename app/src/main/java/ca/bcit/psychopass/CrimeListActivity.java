package ca.bcit.psychopass;

import android.location.Location;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CrimeListActivity extends AppCompatActivity {

    private static final String TAG = "CrimeListActivity";

    ListView listView;
    ArrayList<Crime> crimeModels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crime_list);

        prepareCrimeList();
    }

    public void prepareCrimeList() {
        listView = findViewById(R.id.crimList);

        //get location coordinates
        Double Longitude = (Double)getIntent().getExtras().get("Longitude");
        Double Latitude = (Double)getIntent().getExtras().get("Latitude");
        LatLng location = new LatLng(Latitude, Longitude);

        //filter crime data near this location
        DataAnalysis d = new DataAnalysis(location, MyJsonUtil.crimeList);
        crimeModels = d.getNearbyCrime();

        //convert coordinates to location - used for adaptor
        Location curlocation = new Location("service Provider");
        curlocation.setLatitude(Latitude);
        curlocation.setLongitude(Longitude);

        //apply adaptor to list view
        CrimeListAdaptor adapter = new CrimeListAdaptor(crimeModels, getApplicationContext());
        adapter.setCurLocation(curlocation);
        listView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        switch (item.getItemId())
        {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


}
