package com.k12nt.k12netframe.fcm;

/**
 * Created by on 21/09/16.
 */

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent){
        String confirm = intent.getStringExtra("confirm");
        String query = intent.getStringExtra("query");

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(intent.getIntExtra("id", 0));

        new SetUserStateTask().execute(confirm,query);
    }

}

