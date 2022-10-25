package com.k12nt.k12netframe.attendance;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;
import com.k12nt.k12netframe.R;
import com.k12nt.k12netframe.WebViewerActivity;
import com.k12nt.k12netframe.fcm.MyFirebaseMessagingService;
import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;
import com.k12nt.k12netframe.utils.webConnection.K12NetHttpClient;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.message.BasicHeader;
import cz.msebera.android.httpclient.protocol.HTTP;

/**
 * Receiver for geofence transition changes.
 * <p>
 * Receives geofence transition events from Location Services in the form of an Intent containing
 * the transition type and geofence id(s) that triggered the transition. Creates a JobIntentService
 * that will handle the intent in the background.
 */
public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final int JOB_ID = 573;

    private static final String TAG = "GeofenceTransitionsIS";

    private static final String CHANNEL_ID = "channel_01";

    /**
     * Receives incoming intents.
     *
     * @param context the application context.
     * @param intent  sent by Location Services. This Intent is provided to Location
     *                Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "k12Mobile:MyWakelockTag");
        wakeLock.acquire();

        // Enqueues a JobIntentService passing the context and intent as parameters
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes
                    .getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            Toast.makeText(context, "onReceive errorMessage : " + errorMessage, Toast.LENGTH_LONG).show();
            MyFirebaseMessagingService.sendNotification(context,
                    "onReceive errorMessage : " + errorMessage,
                    "Geo Fence Event",
                    "","","",null);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            Location eventLocation = geofencingEvent.getTriggeringLocation();
            String accuracy = eventLocation.hasAccuracy() ? String.valueOf(eventLocation.getAccuracy()) : "x";

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            ArrayList<GeoFenceData> fenceList = new ArrayList<>();
            NumberFormat nf = NumberFormat.getInstance();
            for (Geofence geofence : geofencingEvent.getTriggeringGeofences()) {
                String[] parts = geofence.getRequestId().split(Pattern.quote("$$$"));
                try {
                    GeoFenceData data = new GeoFenceData();
                    data.Latitude = nf.parse(parts[0]).doubleValue();
                    data.Longitude = nf.parse(parts[1]).doubleValue();
                    data.RadiusInMeter = nf.parse(parts[2]).floatValue();
                    data.LocationIX = nf.parse(parts[3]).intValue();
                    data.Portal = parts[4];
                    data.LocationSummary = parts[5];

                    fenceList.add(data);
                } catch (ParseException e) {
                    WebViewerActivity.Toast(e,context);
                }
            }

            GeoFenceData nearestFence = fenceList.get(0);

            Location locationOfNearestFence = new Location("");//provider name is unnecessary
            locationOfNearestFence.setLatitude(nearestFence.Latitude);
            locationOfNearestFence.setLongitude(nearestFence.Longitude);

            int distanceOfNearest = (int) Math.abs(eventLocation.distanceTo(locationOfNearestFence));

            for (GeoFenceData geofence : fenceList) {
                Location locationOfFence = new Location("");//provider name is unnecessary
                locationOfFence.setLatitude(geofence.Latitude);
                locationOfFence.setLongitude(geofence.Longitude);

                int distanceOfFence = (int) Math.abs(eventLocation.distanceTo(locationOfFence));

                if(distanceOfNearest < distanceOfFence) {
                    nearestFence = geofence;
                    distanceOfNearest = distanceOfFence;
                }
            }

            try {
                K12NetUserReferences.initUserReferences(context);
                String connString = K12NetUserReferences.getConnectionAddress() +
                        "/SISCore.Web/api/MyAttendances/Edit/TakeAttendance";

                final String locationSummary = nearestFence.LocationSummary;
                final String portal = nearestFence.Portal;
                final int distanceInM = distanceOfNearest;

                AsyncHttpClient client = K12NetHttpClient.getClient();

                JSONObject jsonParams = new JSONObject();

                jsonParams.put("UserName", URLEncoder.encode(K12NetUserReferences.getUsername().trim(), StandardCharsets.UTF_8.toString()));
                jsonParams.put("DeviceID", K12NetUserReferences.getDeviceToken());
                jsonParams.put("LocationIX", nearestFence.LocationIX);
                jsonParams.put("Way", geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ? "enter" : "exit");
                jsonParams.put("DistanceFromRange", (distanceOfNearest - nearestFence.RadiusInMeter));

                StringEntity entity = new StringEntity(jsonParams.toString());
                entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

                client.cancelRequests(context, true);
                client.post(context, connString, entity, "application/json", new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                        try {
                            if(response == null) {
                                Toast.makeText(context, "Take Attendance Error : response == null", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            String responseStr = new String(response, "UTF-8");

                            if(responseStr.equals("ok")) {
                                String attendanceTypeSummary = getTransitionString(context, geofenceTransition);

                                if(portal.equals("TP")) {
                                    MyFirebaseMessagingService.sendNotification(context,
                                            String.format(context.getString(R.string.attendance_take),locationSummary,distanceInM,accuracy),
                                            attendanceTypeSummary ,
                                            "StaffAttendances",portal,"/#/Request/check", null);
                                } else {
                                    MyFirebaseMessagingService.sendNotification(context,
                                            String.format(context.getString(R.string.attendance_take),locationSummary,distanceInM,accuracy),
                                            attendanceTypeSummary ,
                                            "MyAttendances",portal,"/#/check",null);
                                }

                            } else if(responseStr.equals("AttendanceTypeAttendNotFound")) {
                                MyFirebaseMessagingService.sendNotification(context,
                                        String.format(context.getString(R.string.attendance_type_attend_not_found),locationSummary) ,
                                        context.getString(R.string.error),
                                        "","*","",null);
                            } else if (!responseStr.equals("")) {
                                Toast.makeText(context, "K12net Geo Fence Error responseStr : " + responseStr, Toast.LENGTH_LONG).show();
                            }

                        } catch (UnsupportedEncodingException e) {
                            WebViewerActivity.Toast(e,context);
                        }

                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                        // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                        WebViewerActivity.Toast(e,context);
                        Toast.makeText(context, "Error Code 1923 : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception ex) {
                WebViewerActivity.Toast(ex,context);
            }
        } else {
            // Check the action code and determine what to do
            String action = intent.getAction();
            if (action != null) {
                Log.e(TAG, action);
                Toast.makeText(context, "K12net Geo Fence Error Action " + action, Toast.LENGTH_LONG).show();
               // MyFirebaseMessagingService.sendNotification(context, "onReceive 1 " + action,"Geo Fence Event","","","");
            }

            // Log the error.
            //Log.e(TAG, context.getString(R.string.geofence_transition_invalid_type, geofenceTransition));
            Toast.makeText(context, "K12net Geo Fence Error: " + context.getString(R.string.geofence_transition_invalid_type, geofenceTransition), Toast.LENGTH_LONG).show();
           // MyFirebaseMessagingService.sendNotification(context, "onReceive 2 " + context.getString(R.string.geofence_transition_invalid_type, geofenceTransition),"Geo Fence Event","","","");
        }

        wakeLock.release();
    }

    /**
     * Gets transition details and returns them as a formatted string.
     *
     * @param geofenceTransition    The ID of the geofence transition.
     * @param triggeringGeofencesIdsList   The geofence(s) triggered.
     * @return                      The transition details formatted as String.
     */
    private String getGeofenceTransitionDetails(
            Context context,
            int geofenceTransition,
            ArrayList<String> triggeringGeofencesIdsList) {

        String attendanceTypeSummary = getTransitionString(context, geofenceTransition);

        String triggeringGeofencesIdsString = TextUtils.join(", ",  triggeringGeofencesIdsList);

        return attendanceTypeSummary + ": " + triggeringGeofencesIdsString;
    }

    /**
     * Maps geofence transition types to their human-readable equivalents.
     *
     * @param transitionType    A transition type constant defined in Geofence
     * @return                  A String indicating the type of transition
     */
    private String getTransitionString(Context context, int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return context.getString(R.string.geofence_transition_entered);
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return context.getString(R.string.geofence_transition_exited);
            default:
                return context.getString(R.string.unknown_geofence_transition);
        }
    }
}