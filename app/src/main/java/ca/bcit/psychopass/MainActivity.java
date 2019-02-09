package ca.bcit.psychopass;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressDialog pDialog;
    private String TAG = MainActivity.class.getSimpleName();
    private MyLocationService locationService;
    private boolean isBoundLocation = false;

    public static final String GOOGLE_MAP_URL = "https://www.google.com/maps/";
    public static final String SERVICE_URL = "https://opendata.arcgis.com/datasets/28c37c4693fc4db68665025c2874e76b_7.geojson";
    public static final String INITIAL_LOCATION =
        "https://www.google.com/maps/place/Maple+Ridge,+BC/@49.2599033,-122.6800957,11z/data=!3m1!4b1!4m5!3m4!1s0x5485d3614f013ecb:0x47a5c3ea30cde8ea!8m2!3d49.2193226!4d-122.5983981";

    public boolean permissionRequested = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).hide();

        webView = findViewById(R.id.mapWebView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(INITIAL_LOCATION);

        ImageView setting = findViewById(R.id.settingImg);
        setting.setClickable(true);
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,SettingsActivity.class);
                startActivity(intent);
            }
        });

        //new GetCrimeData().execute();
    }

    public void onClickBtn(View v) {
        double testLongitude = -122.6039533;
        double testLatitude = 49.2178709;

        Intent intent = new Intent(MainActivity.this,CrimeListActivity.class);
        intent.putExtra("Longitude", testLongitude);
        intent.putExtra("Latitude", testLatitude);
        //startActivity(intent);

        webView = findViewById(R.id.mapWebView);
        String webUrl = webView.getUrl();
        Toast.makeText(MainActivity.this, webUrl, Toast.LENGTH_LONG).show();
        Log.e(TAG, webUrl);
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
        if(MyLocationService.isRunning){
            Intent intent = new Intent(this, MyLocationService.class);
            isBoundLocation = bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
        Log.e("checkingpoint", "starting--------");
    }

    public void registerCallback(){
        MyLocationService.LocationCallback cb = new MyLocationService.LocationCallback() {
            @Override
            public void onCallback(Location location) {
                Toast.makeText(MainActivity.this,
                        location.getLatitude() + ":" + location.getLongitude(),
                        Toast.LENGTH_SHORT).show();
            }
        };
        locationService.registerCallback(MainActivity.class, cb);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBoundLocation) {
            locationService.removeAllCallback(MainActivity.class);
            unbindService(mConnection);
            isBoundLocation = false;
        }
    }

    private void locationServiceCheck(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(this, MyLocationService.class);
            startService(intent);
            MyLocationService.isRunning = true;
        } else {
            if(permissionRequested){
                Timer timer = new Timer();

                timer.scheduleAtFixedRate(new TimerTask() {

                    synchronized public void run() {

                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "This App won't work without location permission." +
                                        "Please turn it on in Setting.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                }, TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(30));
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        1);
                permissionRequested = true;
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            MyLocationService.LocationBinder binder = (MyLocationService.LocationBinder) service;
            locationService = binder.getService();
            isBoundLocation = true;
            registerCallback();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBoundLocation = false;
        }
    };

    //this is not used, but keep it for reference
    private class GetCrimeData extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();

            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(SERVICE_URL);

            Log.e(TAG, "Response from url: " + jsonStr);

            if (jsonStr != null) {
                myJsonUtil ut = new myJsonUtil(MainActivity.this, getApplicationContext());

                //assign below list<Crime> to a global variable
                ut.getAllCrimeObj(jsonStr);
            } else {
                Log.e(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Couldn't get json from server. Check LogCat for possible errors!",
                                Toast.LENGTH_LONG)
                                .show();
                    }
                });
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();

            Log.e(TAG, "Load data complete!");
        }
    }

}
