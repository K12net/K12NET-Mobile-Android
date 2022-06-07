package com.k12nt.k12netframe.attendance;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.k12nt.k12netframe.fcm.MyFirebaseMessagingService;


public class LocationUpdatesService extends Service {

    public final static int NOTIFICATION_ID = 1001;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback mLocationCallback;

    private static final int LOCATION_INTERVAL = 30 * 1000; // 30 second
    Location mLocation;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "LocationUpdatesService.onStartCommand", Toast.LENGTH_LONG).show();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(LOCATION_INTERVAL); // Set the desired interval for active location updates, in milliseconds.
        locationRequest.setFastestInterval(LOCATION_INTERVAL / 2); //This controls the fastest rate at which your application will receive location updates, which might be faster than setInterval(long) in some situations (for example, if other applications are triggering location updates).
        locationRequest.setSmallestDisplacement(10); // Set the minimum displacement between location updates in meters

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null) {
                    mLocation = locationResult.getLastLocation();
                    sendNotification();
                }

            }
        };

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && checkBackgroundLocation()) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
        } else {

        }

        return START_STICKY;
    }

    double lastDist = 100000;
    private void sendNotification() {
        double dist = distance(mLocation.getLatitude(),39.862214,mLocation.getLongitude(),32.737033,0,0);

        if( Math.abs(lastDist - dist) > 5) {
            String text = mLocation == null ? null : ("Distance :" + dist +  " / Lat,Long : " + mLocation.getLatitude() + "," + mLocation.getLongitude());
            Toast.makeText(this, text, Toast.LENGTH_LONG).show();
            MyFirebaseMessagingService.sendNotification(this, "Android Location",text,"","","",null);
        }

        lastDist = dist;
    }

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     *
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     * @returns Distance in Meters
     */
    public static double distance(double lat1, double lat2, double lon1,
                                  double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    public boolean checkBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return ActivityCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(mLocationCallback);

        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}