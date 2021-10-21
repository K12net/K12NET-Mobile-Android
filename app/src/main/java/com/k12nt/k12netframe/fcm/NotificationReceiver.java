package com.k12nt.k12netframe.fcm;

/**
 * Created by on 21/09/16.
 */

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent){
        String confirm = intent.getStringExtra("confirm");
        String query = intent.getStringExtra("query");

        new SetUserStateTask().execute(confirm,query);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(intent.getIntExtra("id", 0));
    }

    private class SetUserStateTask
            extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params)
        {
            String confirm = params[0];
            String query = params[1];

            try {
                String connString = K12NetUserReferences.getConnectionAddress() + "/Authentication_JSON_AppService.axd/NotificationResponse";
                URL url = new URL(connString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");

                Map<String,Object> headerParams = new LinkedHashMap<>();
                headerParams.put("result", confirm);
                headerParams.put("notificationID", query);

                for (Map.Entry<String,Object> param : headerParams.entrySet()) {
                    conn.addRequestProperty(param.getKey(), String.valueOf(param.getValue()));
                }

                BufferedWriter bw = null;
                try {
                    bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
                    bw.write("");
                    bw.flush();
                    bw.close();
                } catch(IOException exception) {
                    throw exception;
                } finally {
                    try {
                        if( bw != null ) {
                            bw.close();
                        }
                    } catch(IOException ex) {

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }

        @Override
        protected void onPostExecute(String result)
        {
            super.onPostExecute(result);
            // do something with the result
        }
    }

}
