package com.k12nt.k12netframe.fcm;

/**
 * Created by on 21/09/16.
 */

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.k12nt.k12netframe.LoginActivity;
import com.k12nt.k12netframe.R;
import com.k12nt.k12netframe.WebViewerActivity;
import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;

import me.leolin.shortcutbadger.ShortcutBadger;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String ChannelID = "M_CH_ID_K12net";
    private static final String ChannelID_Confirm = "M_CH_ID_K12net_Confirm";

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        K12NetUserReferences.setDeviceToken(s);
    }

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

        String msg = "not found";
        String intent = "";
        String portal = "";
        String query = "";
        String title = "";

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
            title = remoteMessage.getData().get("title");

            if (msg == null) {
                msg = remoteMessage.getData().get("body");
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            msg =  remoteMessage.getNotification().getBody();

            if (title == null) {
                title = remoteMessage.getNotification().getTitle();
            }
        }

        if (title == null) {
            title = this.getString(R.string.app_name);
        }

        K12NetUserReferences.initUserReferences(this);
        K12NetUserReferences.increaseBadgeNumber();

        sendNotification(msg,title,intent,portal,query);

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    // [END receive_message]

    private void confirmNotification(String messageBody,String title,String intentStr,String portal,String query) {
        Intent intent = new Intent(this, WebViewerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

        title += " Confirm?";
        intent.putExtra("intent",intentStr);
        intent.putExtra("portal", portal);
        intent.putExtra("query", query);
        intent.putExtra("body", messageBody);
        intent.putExtra("title", title);

        int requestID = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestID, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Notification.Builder builder = null;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Intent yesIntent = new Intent(this, LoginActivity.class);
            yesIntent.putExtra("confirm","yes");
            yesIntent.putExtra("query",query);
            PendingIntent yesPendingAction = PendingIntent.getActivity(this, 0 , yesIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent noIntent = new Intent(this, LoginActivity.class);
            noIntent.putExtra("confirm","no");
            noIntent.putExtra("query",query);
            PendingIntent noPendingAction = PendingIntent.getActivity(this, 0 , noIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Notification.Action yesAction = null;
            Notification.Action noAction = null;

            if("auth".equals(portal)) {
                yesAction = new Notification.Action.Builder(Icon.createWithResource("", R.drawable.yes), this.getString(R.string.yes), yesPendingAction).build();
                noAction = new Notification.Action.Builder(Icon.createWithResource("", R.drawable.no), this.getString(R.string.no), noPendingAction).build();
            } else {
                yesAction = new Notification.Action.Builder(Icon.createWithResource("", R.drawable.confirm), this.getString(R.string.yes), yesPendingAction).build();
                noAction = new Notification.Action.Builder(Icon.createWithResource("", R.drawable.cancel), this.getString(R.string.no), noPendingAction).build();
            }

            builder = new Notification.Builder(this, ChannelID_Confirm)
                    .setSmallIcon(R.drawable.k12net_logo)
                    .setStyle(new Notification.BigTextStyle().bigText(messageBody))
                    .setContentTitle(title)
                    .setChannelId(ChannelID_Confirm)
                    .setContentText(messageBody)
                    .setContentIntent(pendingIntent)
                    .setNumber(K12NetUserReferences.getBadgeCount())
                    .setContentIntent(pendingIntent)
                    .setWhen(System.currentTimeMillis())
                    .setOngoing(true).setAutoCancel(false)
                    .addAction(yesAction).addAction(noAction);
        }

        if(builder == null) return;

        Notification notification = builder.build();

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(ChannelID_Confirm) == null) {
                createChannel(title,notificationManager,ChannelID_Confirm);
            }
        }

        notificationManager.notify(K12NetUserReferences.getBadgeCount(), notification);

        ShortcutBadger.applyCount(getApplicationContext(), K12NetUserReferences.getBadgeCount());
    }
    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(String messageBody,String title,String intentStr,String portal,String query) {
        int requestID = (int) System.currentTimeMillis();
        Intent intent = new Intent(this, WebViewerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.putExtra("intent",intentStr);
        intent.putExtra("portal", portal);
        intent.putExtra("query", query);
        intent.putExtra("body", messageBody);
        intent.putExtra("title", title);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestID, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ChannelID)
                .setDefaults(Notification.DEFAULT_ALL)
                .setSmallIcon(R.drawable.k12net_logo)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody))
                .setContentTitle(title)
                .setChannelId(ChannelID)
                .setColor(0xfd7e14)
                .setContentText(messageBody)
                .setAutoCancel(true).setContentIntent(pendingIntent)
                .setSound(defaultSoundUri)
                .setNumber(K12NetUserReferences.getBadgeCount())
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis());

        if("confirm".equals(intentStr)) {
            Intent yesAction = new Intent(this, NotificationReceiver.class);
            yesAction.putExtra("confirm","1");
            yesAction.putExtra("portal",portal);
            yesAction.putExtra("query",query);
            yesAction.putExtra("id", requestID);
            PendingIntent yesPendingAction = PendingIntent.getBroadcast(this, 1 , yesAction, PendingIntent.FLAG_ONE_SHOT);

            Intent noAction = new Intent(this, NotificationReceiver.class);
            noAction.putExtra("confirm","0");
            noAction.putExtra("query",query);
            noAction.putExtra("portal",portal);
            noAction.putExtra("id", requestID);
            PendingIntent noPendingAction = PendingIntent.getBroadcast(this, 0 , noAction, PendingIntent.FLAG_ONE_SHOT);

            if("auth".equals(portal)) {
                builder = builder.addAction(0,"✅ " + this.getString(R.string.yes),yesPendingAction);
                builder = builder.addAction(0,"⛔ " + this.getString(R.string.no),noPendingAction);
            } else {
                builder = builder.addAction(R.drawable.confirm,this.getString(R.string.yes),yesPendingAction);
                builder = builder.addAction(R.drawable.cancel,this.getString(R.string.no),noPendingAction);
            }
            builder = builder.setWhen(0).setOngoing(true).setAutoCancel(false);
                    //.setStyle(new NotificationCompat.BigTextStyle().bigText(this.getString(R.string.yes)))
                    //.setStyle(new NotificationCompat.BigTextStyle().bigText(this.getString(R.string.no)));
        }

        Notification notification = null;

        if (Build.VERSION.SDK_INT > 15) {// for some reason Notification.PRIORITY_DEFAULT doesn't show the counter
            builder.setPriority(Notification.PRIORITY_MAX);
            notification= builder.build();
        } else {
            notification = builder.getNotification();
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(ChannelID) == null) {
                createChannel(title,notificationManager,ChannelID);
            }
        }

        notificationManager.notify(requestID, notification);

        ShortcutBadger.applyCount(getApplicationContext(), K12NetUserReferences.getBadgeCount());
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel(String channelTitle, NotificationManager notificationManager,String channelID ) {
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel notificationChannel = new NotificationChannel(channelID, channelTitle, importance);
        notificationChannel.setShowBadge(true);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        notificationChannel.enableVibration(true);
        notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

        notificationManager.createNotificationChannel(notificationChannel);
    }

}
