package ca.bcit.psychopass;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.DrawableRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener {

    private static final String TAG = "MainActivity";
    private static final String MARKER_TAG_ENABLED = "MTE";
    private static final String MARKER_TAG_DISABLED = "MTD";
    private static final double CIRCLE_RADIUS = 200;

    public boolean permissionRequested = false;

    private MyLocationService locationService;
    private boolean isBoundLocation = false;
    private Timer timer = new Timer();
    private Location curLocation;
    private GoogleMap mMap;
    private Marker locationMarker;
    private Circle locationCircle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set customize menu bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //check if location permission is enabled
        locationServiceCheck();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //read json file and parse into array of crime objects
        MyJsonUtil jsonUtil = new MyJsonUtil(MainActivity.this, getApplicationContext());
        jsonUtil.parseLocalJSON();

    }

    public void onClickCrimeNearbyBtn(View v) {

        if (locationMarker != null) {

            if(locationMarker.getTag().equals(MARKER_TAG_ENABLED)){
                Log.e(TAG, "Already exist");
                return;
            }

            //get marker coordinates
            LatLng position = locationMarker.getPosition();

            //check how many crime nearby
            DataAnalysis d = new DataAnalysis(position, MyJsonUtil.crimeList);
            int size = d.getNearbyCrime().size();

            if (size == 0) {
                //no crime data
                Toast.makeText(MainActivity.this, R.string.noCrimeData, Toast.LENGTH_LONG).show();
            } else {
                //add a circle around the location and show a message
                Drawable circleDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_alert, null);
                BitmapDescriptor markerIcon = getMarkerIconFromDrawable(circleDrawable);

                locationMarker.setTitle("Found " + size + " crimes nearby");
                locationMarker.setSnippet("--Tap to view details--");
                locationMarker.setIcon(markerIcon);
                locationMarker.setTag(MARKER_TAG_ENABLED);
                locationMarker.showInfoWindow();

                //add a circle to current location
                locationCircle = mMap.addCircle(new CircleOptions()
                        .center(position)
                        .radius(CIRCLE_RADIUS)
                        .strokeColor(Color.RED)
                        .fillColor(0x220000FF)
                        .strokeWidth(5));

                //move camera to current location
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder()
                                .target(position)
                                .zoom(15.5f)
                                .bearing(0)
                                .tilt(25)
                                .build()));
            }

        } else {
            Toast.makeText(MainActivity.this, R.string.nomarker, Toast.LENGTH_LONG).show();
        }

    }

    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable) {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;
            case R.id.action_location:
                markCurrentLocation();
                return true;
            case R.id.action_search:
                openSearchDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Search Location");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                List<Address> addressList = null;
                String location = input.getText().toString();

                if (location != null && location != "") {
                    Geocoder geocoder = new Geocoder(getApplicationContext());
                    try {
                        addressList = geocoder.getFromLocationName(location, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Address adr = addressList.get(0);
                    LatLng latLng = new LatLng(adr.getLatitude(), adr.getLongitude());
                    removeMarker();
                    locationMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(location));
                    locationMarker.setTag(MARKER_TAG_DISABLED);
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                }

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void markCurrentLocation() {
        DecimalFormat df = new DecimalFormat("#.#######");
        double Lat = Double.valueOf(df.format(curLocation.getLatitude()));
        double Long = Double.valueOf(df.format(curLocation.getLongitude()));
        Log.e(TAG, Lat + ", " + Long);

        removeMarker();

        // Add a marker to current location
        LatLng loc = new LatLng(Lat, Long);
        locationMarker = mMap.addMarker(new MarkerOptions().position(loc));
        locationMarker.setTag(MARKER_TAG_DISABLED);
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder()
                        .target(loc)
                        .zoom(16f)
                        .bearing(0)
                        .tilt(25)
                        .build()));
    }

    private void removeMarker() {
        if (locationMarker != null) {
            locationMarker.remove();
        }

        if (locationCircle != null) {
            locationCircle.remove();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        locationServiceCheck();
        Log.e("checkingpoint", "resuming--------");
    }

    @Override
    protected void onStart() {
        super.onStart();
        permissionRequested = false;
        if (!isBoundLocation) {
            Intent intent = new Intent(this, MyLocationService.class);
            isBoundLocation = bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
        Log.e("checkingpoint", "starting--------");
    }

    public void registerCallback() {
        MyLocationService.LocationCallback cb = new MyLocationService.LocationCallback() {
            @Override
            public void onCallback(Location location) {
                Log.e(TAG, location.getLatitude() + ":" + location.getLongitude());
                curLocation = location;
            }
        };
        locationService.registerCallback(MainActivity.class, cb);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBoundLocation) {
            locationService.removeCallback(MainActivity.class);
            unbindService(mConnection);
            isBoundLocation = false;
        }
    }

    private void locationServiceCheck() {
        Intent intent = new Intent(this, MyLocationService.class);
        if (hasAllPermission()) {

            timer.cancel();

            if (MyLocationService.isRunning)
                return;

            startService(intent);

        } else {
            if (MyLocationService.isRunning)
                stopService(intent);

            if (permissionRequested) {

                timer.scheduleAtFixedRate(new TimerTask() {

                    synchronized public void run() {

                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, R.string.requirePermission, Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                }, TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(30));
            } else {
                requestForAllPermission();
                permissionRequested = true;
            }
        }
    }

    public boolean hasAllPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this,
                Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestForAllPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.SEND_SMS,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.VIBRATE},
                1);
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            MyLocationService.LocationBinder binder = (MyLocationService.LocationBinder) service;
            locationService = binder.getService();
            isBoundLocation = true;
            registerCallback();
            curLocation = locationService.getLastLocation();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBoundLocation = false;
        }
    };

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //when map ready, move camera to maple ridge
        LatLng mapleRidge = new LatLng(49.216027, -122.5984445);
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder()
                        .target(mapleRidge)
                        .zoom(14.5f)
                        .bearing(0)
                        .tilt(25)
                        .build()));

        //set onclick event for map marker
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        //retrieve marker type
        String type = (String) marker.getTag();
        Log.e(TAG, "Clicked Marker - type: " + type);

        //if the marker is info marker, show the crime list
        if (type.equals(MARKER_TAG_ENABLED)) {
            Intent i = new Intent(this, CrimeListActivity.class);
            i.putExtra("Longitude", marker.getPosition().longitude);
            i.putExtra("Latitude", marker.getPosition().latitude);
            startActivity(i);
        }

        return false;
    }


    @Override
    public void onMapClick(LatLng latLng) {
        //if circle exist, check distance. Return when click within circle
        if (locationMarker != null && locationCircle != null && CalculationByDistance(latLng, locationMarker.getPosition()) <= CIRCLE_RADIUS) {
            return;
        }

        //remove marker and circle
        removeMarker();

        //add new marker
        locationMarker = mMap.addMarker(new MarkerOptions().position(latLng));
        locationMarker.setTag(MARKER_TAG_DISABLED);
    }

    private double CalculationByDistance(LatLng StartP, LatLng EndP) {
        int Radius = 6371;// radius of earth in Km
        double lat1 = StartP.latitude;
        double lat2 = EndP.latitude;
        double lon1 = StartP.longitude;
        double lon2 = EndP.longitude;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double valueResult = Radius * c * 1000;

        Log.i("Radius Value", "Distance: " + valueResult + "meters");

        return valueResult;
    }
}
