package ca.bcit.psychopass;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MyLocationService extends Service {

    public static boolean isRunning = false;

    private static final String TAG = "BACKGROUNDLOCATION";
    private static final int LOCATION_INTERVAL = 10000;
    private static final float LOCATION_DISTANCE = 10f;

    private LocationManager mLocationManager = null;
    private final IBinder mBinder = new LocationBinder();

    private Map<String, LocationCallback> callbackMap = new HashMap<>();

    public interface LocationCallback {
        void onCallback(Location location);
    }

    public class LocationBinder extends Binder {
        MyLocationService getService() {
            return MyLocationService.this;
        }
    }

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        public LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.e(TAG, "onLocationChanged: " + location);

            for(Map.Entry<String, LocationCallback> entry : callbackMap.entrySet()){
                entry.getValue().onCallback(location);
            }

            DecimalFormat df = new DecimalFormat("#.#######");
            double Lat = Double.valueOf(df.format(location.getLatitude()));
            double Long = Double.valueOf(df.format(location.getLongitude()));
            LatLng loc = new LatLng(Lat, Long);

            DataAnalysis da = new DataAnalysis(loc,MyJsonUtil.crimeList);
            if(da.isDangerZone()){
                sendWarningByUserPreference();
        }
            mLastLocation.set(location);
        }



        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");
        initializeLocationManager();
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
        isRunning = true;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    public void removeCallback(Class<? extends Context> classType) {
        callbackMap.remove(classType.getName());
    }

    public void registerCallback(Class<? extends Context> classType, LocationCallback cb){
        String className = classType.getName();
        callbackMap.put(className, cb);
    }

    public Location getLastLocation(){
        return mLocationListeners[0].mLastLocation;
    }

    public void sendWarningByUserPreference(){
        SharedPreferences mSetting = PreferenceManager.getDefaultSharedPreferences(MyLocationService.this);
        boolean pref_push = mSetting.getBoolean("pref_push", true);
        boolean pref_text = mSetting.getBoolean("pref_text", true);
        boolean pref_phone = mSetting.getBoolean("pref_phone", true);
        boolean pref_vibrate = mSetting.getBoolean("pref_vibrate", true);
        Log.e(TAG, "User preferences: " + pref_push + pref_text + pref_phone + pref_vibrate);
        if(pref_push){
            sendNotification();
        }
        if(pref_text){
            sendSMS();
        }
        if(pref_phone){
            sendSound();
        }

        if(pref_vibrate){
            sendVibrate();
        }
    }

    public void sendNotification() {
        Intent intent = new Intent(MyLocationService.this,MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(MyLocationService.this, 0, intent, 0);
        String msgTitle = getString(R.string.warning);
        String msgBody = getString(R.string.warning_content);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(MyLocationService.this)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle(msgTitle)
                .setContentIntent(pendingIntent)
                .setContentText(msgBody)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        Notification notification = mBuilder.build();

        NotificationManager notificationManager = (NotificationManager) MyLocationService.this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }

    public void sendSMS(){
        String msgBody = getString(R.string.warning_content);

        try {
            TelephonyManager tMgr = (TelephonyManager)getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            String mPhoneNumber = tMgr.getLine1Number();
            Log.e(TAG, "Got Phone Number");
            PendingIntent pi = PendingIntent.getActivity(this, 0,
                    new Intent(this, MyLocationService.class), 0);
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(mPhoneNumber, null, msgBody, pi, null);
            Log.e(TAG, "Text Msg Sent");
        } catch (java.lang.SecurityException ex) {
            ex.printStackTrace();
        }

    }

    public void sendVibrate(){
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            Log.e(TAG, "Vibration Sent via new Api");
        } else {
            //deprecated in API 26
            v.vibrate(500);
            Log.e(TAG, "Vibration Sent via old Api");
        }
    }

    public void sendSound(){
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.psycho_pass_dominator);
        mediaPlayer.start();
        Log.e(TAG, "Playing Sound Notification");
    }

}
