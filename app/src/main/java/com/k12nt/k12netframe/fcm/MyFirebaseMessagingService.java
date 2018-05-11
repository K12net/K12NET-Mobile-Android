package com.k12nt.k12netframe.fcm;

/**
 * Created by tarikcanturk on 21/09/16.
 */
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.k12nt.k12netframe.LoginActivity;
import com.k12nt.k12netframe.R;
import com.k12nt.k12netframe.async_tasks.LoginAsyncTask;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;

import java.util.Locale;

import me.leolin.shortcutbadger.ShortcutBadger;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    public static int NOTIFICATION_ID = 1;

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        NOTIFICATION_ID++;

        String msg = "not found";
        String intent = "";
        String portal = "";
        String query = "";

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            msg = remoteMessage.getData().get("message");

            intent = remoteMessage.getData().get("intent");
            portal = remoteMessage.getData().get("portal");
            query = remoteMessage.getData().get("query");
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            msg =  remoteMessage.getNotification().getBody();
        }

        K12NetUserReferences.initUserReferences(this);
        K12NetUserReferences.increaseBadgeNumber();
        ShortcutBadger.applyCount(getApplicationContext(), K12NetUserReferences.getBadgeCount());

        sendNotification(msg,intent,portal,query);

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    // [END receive_message]
    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(String messageBody,String intentStr,String portal,String query) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        /*intent.setData(url);*/
        intent.putExtra("intent",intentStr);
        intent.putExtra("portal", portal);
        intent.putExtra("query", query);

        int requestID = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestID, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.k12net_logo)
                .setContentTitle(this.getString(R.string.app_name))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(messageBody))
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

      /*  if(WebViewerActivity.ctx != null) {
            Toast.makeText(WebViewerActivity.ctx, messageBody, Toast.LENGTH_SHORT).show();
        }*/

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }
}
