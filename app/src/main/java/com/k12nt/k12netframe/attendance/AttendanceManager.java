package com.k12nt.k12netframe.attendance;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.k12nt.k12netframe.BuildConfig;
import com.k12nt.k12netframe.K12NetSettingsDialogView;
import com.k12nt.k12netframe.R;
import com.k12nt.k12netframe.WebViewerActivity;
import com.k12nt.k12netframe.WebViewerActivity;
import com.k12nt.k12netframe.async_tasks.TaskHandler;
import com.k12nt.k12netframe.fcm.MyFirebaseMessagingService;
import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;
import com.k12nt.k12netframe.utils.webConnection.K12NetHttpClient;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.ResponseHandlerInterface;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.BasicHttpEntity;
import cz.msebera.android.httpclient.entity.ByteArrayEntity;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.impl.cookie.BasicClientCookie;
import cz.msebera.android.httpclient.message.BasicHeader;
import cz.msebera.android.httpclient.protocol.HTTP;

public class AttendanceManager extends Service {

    private static final int LOCATION_INTERVAL = 1000 * 60 * 2; // 2 minute
    public static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    public static final int REQUEST_CHECK_SETTINGS = 35;

    private static final String TAG = AttendanceManager.class.getSimpleName();
    private List<Geofence> mGeofenceList;

    /**
     * Used when requesting to add or remove geofences.
     */
    private PendingIntent mGeofencePendingIntent;
    /**
     * Provides access to the Geofencing API.
     */
    private GeofencingClient mGeofencingClient;

    private static AttendanceManager mInstance;

    public static AttendanceManager Instance()
    {
        if(mInstance == null) mInstance = new AttendanceManager();
        return  mInstance;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
       K12NetSettingsDialogView.setLanguageToDefault(getBaseContext());
       Notification notification =  MyFirebaseMessagingService.sendNotification(this,
                 this.getString(R.string.attendance_service_started) ,
                 this.getString(R.string.attendance_service_title),
                "","*","", MyFirebaseMessagingService.REQUEST_ID_ATTENDANCE);

        startForeground(1001, notification);

        if (mGeofencingClient == null) {
            mGeofencePendingIntent = null;
            mGeofencingClient = LocationServices.getGeofencingClient(this);
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        try {
            //WifiManager.setEnabled();
            Context ctx = this;

            boolean hasPermission = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                hasPermission = ActivityCompat.checkSelfPermission(ctx.getApplicationContext(),
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
            }

            if(hasPermission) {
                // Initially set the PendingIntent used in startMonitorLocations() and stopMonitorLocations() to null.
                K12NetSettingsDialogView.setLanguageToDefault(getBaseContext());
                mGeofencePendingIntent = null;
                mGeofencingClient = LocationServices.getGeofencingClient(ctx);

                bindUserLocationFences(ctx, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                        // called when response HTTP status is "200 OK"
                        bindFenceData(ctx, statusCode,headers,response);
                        startMonitorLocations();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                        // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                        e.printStackTrace();
                        Toast.makeText(ctx, "Error Code 1071 : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                super.onCreate();
            } else {
               // final Intent intent = new Intent(ctx, AttendanceManager.class);
                //ctx.stopService(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error AttendanceManager.onCreate : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        try {
            stopMonitorLocations(this);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error AttendanceManager.onCreate : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startAttendanceService(WebViewerActivity activity) {
        final Intent intent = new Intent(activity, AttendanceManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startForegroundService(intent);
        } else {
            activity.startService(intent);
        }
    }

    public void stopAttendanceService(WebViewerActivity activity) {
        final Intent intent = new Intent(activity, AttendanceManager.class);
        activity.stopService(intent);
    }

    public void initialize(WebViewerActivity activity, TaskHandler handler) {
        try {
            if(K12NetUserReferences.isPermitBackgroundLocation() != null && K12NetUserReferences.isPermitBackgroundLocation() == false) {
                handler.onTaskCompleted("Cancel");
                return;
            }

            // Initially set the PendingIntent used in startMonitorLocations() and stopMonitorLocations() to null.
            mGeofencePendingIntent = null;
            mGeofencingClient = LocationServices.getGeofencingClient(activity);

            bindUserLocationFences(activity, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                    try {
                        // called when response HTTP status is "200 OK"
                        bindFenceData(activity, statusCode,headers,response);

                        if(mGeofenceList.isEmpty()) {
                            K12NetUserReferences.setPermitBackgroundLocation(false);
                            if(K12NetUserReferences.isPermitBackgroundLocation() != null) stopMonitorLocations(activity);
                            Toast.makeText(activity, activity.getString(R.string.no_geofence), Toast.LENGTH_LONG).show();
                            handler.onTaskCompleted("Cancel");
                            return;
                        }

                        if(checkAndRequestPermissions(activity)) {
                            bindLocationRequest(activity);
                            handler.onTaskCompleted("FencesAdded");
                        } else {
                            handler.onTaskCompleted("PermissionRequested");
                        }
                    } catch (Exception e) {
                        Toast.makeText(activity, "initialize.bindUserLocationFences.onSuccess.error : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }

                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                    // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                    e.printStackTrace();

                    String json = null;
                    try {
                        json = new String(errorResponse, "UTF-8");
                        Log.i("WEB", "bindFenceData.onFailure: " + json);
                    } catch (UnsupportedEncodingException ex) {
                        ex.printStackTrace();
                    }

                    Toast.makeText(activity, "Error Code 1071 : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    handler.onTaskCompleted("Error");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(activity, "Error AttendanceManager.initialize : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void bindLocationRequest(WebViewerActivity activity) {
        try
        {
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(LOCATION_INTERVAL); // Set the desired interval for active location updates, in milliseconds.
            locationRequest.setFastestInterval(LOCATION_INTERVAL / 4); //This controls the fastest rate at which your application will receive location updates, which might be faster than setInterval(long) in some situations (for example, if other applications are triggering location updates).
            locationRequest.setSmallestDisplacement(10); // Set the minimum displacement between location updates in meters

            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);
            //builder.setNeedBle(true);//bluetooth

            // alternative => check LocationUpdatesService =>
            //            fusedLocationProviderClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
            Task<LocationSettingsResponse> task =
                    LocationServices.getSettingsClient(activity).checkLocationSettings(builder.build());

            task.addOnCompleteListener(task1 -> {
                try {
                    LocationSettingsResponse response = task1.getResult(ApiException.class);
                    // All location settings are satisfied. The client can initialize location
                    // requests here.
                    startAttendanceService(activity);
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                resolvable.startResolutionForResult(
                                        activity,
                                        REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                                startAttendanceService(activity);
                            } catch (ClassCastException e) {
                                // Ignore, should be an impossible error.
                                startAttendanceService(activity);
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            startAttendanceService(activity);
                            break;
                    }
                }
            });
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
            startAttendanceService(activity);
        }
    }

    private void bindFenceData(Context ctx, int statusCode, Header[] headers, byte[] response) {
        try {
            mGeofenceList.clear();
            if(response.length <= 0) return;

            String json = new String(response, "UTF-8");
            //Type listType = new TypeToken<List<GeoFenceData>>() {}.getType();
            Gson gson = new Gson();
            GeoFenceData[] locations = gson.fromJson(json, GeoFenceData[].class);

            Geofence.Builder geoBuild = new Geofence.Builder();

            for (GeoFenceData location : locations) {
                if(location.RadiusInMeter <= 0) continue;

                Geofence geofence = geoBuild
                        .setRequestId(location.GetRequestID())
                        //.setLoiteringDelay(2 * 1000)                   // Dwell after 2 seconds
                        //.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                        .setNotificationResponsiveness(1000 * 60 * 2)
                        //.setNotificationResponsiveness((int) TimeUnit.SECONDS.toMillis(2))       // 10 * 1000 * 60 => Notify within 10 minute
                        .setCircularRegion(location.Latitude, location.Longitude, location.RadiusInMeter) // first lat,then lng,then radius
                        //.setCircularRegion(36.797330779140715, 34.589732884679925, 10000) // first lat,then lng,then radius
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .build();

                mGeofenceList.add(geofence);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Toast.makeText(ctx, "Error Code 1453 : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void bindUserLocationFences(Context ctx, ResponseHandlerInterface responseHandler) {
        try {
            mGeofenceList = new ArrayList<Geofence>();
            AsyncHttpClient client = K12NetHttpClient.getClient();

            K12CookieStore myCookieStore = new K12CookieStore();

            BasicClientCookie newCookie = new BasicClientCookie("UICulture" , K12NetUserReferences.getLanguageCode());
            //newCookie.setVersion(cookie.getVersion());
            newCookie.setDomain(".k12net.com");
            newCookie.setPath("/");
            myCookieStore.addCookie(newCookie);

            client.setCookieStore(myCookieStore);

            K12NetUserReferences.initUserReferences(ctx);
            String deviceID = K12NetUserReferences.getDeviceToken();
            String connString = K12NetUserReferences.getConnectionAddress() + "/SISCore.Web/api/MyAttendances/GeoFences";

            if(deviceID == null || deviceID.equals("")) return;

            //RequestParams params = new RequestParams();
            JSONObject jsonParams = new JSONObject();
            jsonParams.put("UserName", K12NetUserReferences.getUsername().trim());
            jsonParams.put("DeviceID", deviceID);
            StringEntity entity = new StringEntity(jsonParams.toString());
            //StringEntity entity = new StringEntity(K12NetUserReferences.getUsername().trim(), "UTF-8");
            entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

            client.cancelRequests(ctx, true);

            //BasicHttpEntity entity=new BasicHttpEntity();
           // entity.setContent(new ByteArrayInputStream(K12NetUserReferences.getUsername().trim().getBytes()));
           // entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            //ByteArrayEntity be = new ByteArrayEntity(K12NetUserReferences.getUsername().trim().toString().getBytes());
            //client.addHeader("Content-Type:", "application/json");
            //client.addHeader("Accept", "application/json");
            client.post(ctx, connString, entity, "application/json", responseHandler);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    public void startMonitorLocations() {
        try {
            //this.registerReceiver(Receiver, new IntentFilter("GeoFence"));

           /* final Intent intent = new Intent(activity, LocationUpdatesService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(intent);
            } else {
                activity.startService(intent);
            } */

            final Context ctx = this;

            GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT | GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofences(mGeofenceList).build();

            getGeofencePendingIntent(ctx);
            if(mGeofencePendingIntent == null) {
                boolean isGooglePlayServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ctx) == ConnectionResult.SUCCESS;

                Toast.makeText(ctx, "Error Geofence Broadcast PendingIntent is null.  isGooglePlayServicesAvailable : " + (isGooglePlayServicesAvailable ? "true" : "false"), Toast.LENGTH_SHORT).show();
                return;
            }

            mGeofencingClient.addGeofences(geofencingRequest, mGeofencePendingIntent)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Geofences added
                            Log.w(TAG, ctx.getString(R.string.geofences_added));
                            Toast.makeText(ctx, ctx.getString(R.string.geofences_added), Toast.LENGTH_SHORT).show();
                            K12NetUserReferences.setPermitBackgroundLocation(true);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Failed to add geofences
                            K12NetUserReferences.setPermitBackgroundLocation(false);

                            // Get the status code for the error and log it using a user-friendly message.
                            String errorMessage = GeofenceErrorMessages.getErrorString(ctx, e);

                            if(errorMessage.equals(ctx.getString(R.string.geofence_too_many_pending_intents))) {
                                stopMonitorLocations(ctx);
                            }
                            Log.e(TAG, errorMessage);
                            Toast.makeText(ctx, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopMonitorLocations(Context ctx) {
       /*final Intent intent = new Intent(activity, LocationUpdatesService.class);
        activity.stopService(intent); */
        getGeofencePendingIntent(ctx);
        if(mGeofencePendingIntent == null) {
            boolean isGooglePlayServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ctx) == ConnectionResult.SUCCESS;

            Toast.makeText(ctx, "Error Geofence Broadcast PendingIntent is null.  isGooglePlayServicesAvailable : " + (isGooglePlayServicesAvailable ? "true" : "false"), Toast.LENGTH_SHORT).show();
            return;
        }
        if (mGeofencingClient == null) {
            return;
        }
        K12NetUserReferences.initUserReferences(ctx);
        mGeofencingClient.removeGeofences(mGeofencePendingIntent)
                .addOnSuccessListener( new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Geofences removed
                        Log.w(TAG, ctx.getString(R.string.geofences_removed));
                        Toast.makeText(ctx, ctx.getString(R.string.geofences_removed), Toast.LENGTH_SHORT).show();
                        K12NetUserReferences.setPermitBackgroundLocation(false);
                    }
                })
                .addOnFailureListener( new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        K12NetUserReferences.setPermitBackgroundLocation(true);
                        // Failed to remove geofences
                        // Get the status code for the error and log it using a user-friendly message.
                        String errorMessage = GeofenceErrorMessages.getErrorString(ctx, e);
                        Log.e(TAG, errorMessage);
                        Toast.makeText(ctx, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent(Context ctx) {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(ctx, GeofenceBroadcastReceiver.class);
        //Intent intent = new Intent(activity, GeofenceTransitionsIntentService.class);
        //intent.setAction(INTENT_ACTION_SEND_EVENT_CIRCULAR);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // startMonitorLocations() and stopMonitorLocations().
        int requestID = 1978;//(int) System.currentTimeMillis();

        mGeofencePendingIntent = PendingIntent.getBroadcast(ctx, requestID, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        return mGeofencePendingIntent;
    }

    public boolean checkAndRequestPermissions(WebViewerActivity activity) {

        List<String> permissions = new ArrayList<String>();
        boolean showMessage = false;

        int result = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (result != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                showMessage = true;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            result = ActivityCompat.checkSelfPermission(activity.getApplicationContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            if (result != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    showMessage = true;
                }
            }
        }

        result = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION);
        if (result != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                showMessage = true;
            }
        }

        result = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS);
        if (result != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS);
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS)) {
                showMessage = true;
            }
        }

        if(showMessage) {
            //showAlertDialogForRequestingPermission(activity);
            //Toast.makeText(activity, R.string.locationAccessAppSettings, Toast.LENGTH_LONG).show();
        }

        if(!permissions.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.requestPermissions(permissions.toArray(new String[permissions.size()]), REQUEST_PERMISSIONS_REQUEST_CODE);

                return false;
            }
        }

        return true;
    }

    private void showAlertDialogForRequestingPermission(WebViewerActivity activity) {
        Runnable confirmation = () -> {
            if(!activity.isConfirmed) return;
            // Build intent that displays the App settings screen.
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
            intent.setData(uri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        };

        activity.setConfirmDialog(activity.getString(R.string.settings),activity.getString(R.string.permission_denied_explanation),confirmation);
    }

    private void checkLocationService() {
        boolean gps_enabled = true;
        boolean network_enabled = true;

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
            try {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            try {
                network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            if (!gps_enabled && !network_enabled) {

            }
        }
    }
}
