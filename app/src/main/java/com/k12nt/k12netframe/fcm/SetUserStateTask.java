package com.k12nt.k12netframe.fcm;

import android.os.AsyncTask;

import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

public class SetUserStateTask
        extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... params) {
        String confirm = params[0];
        String[] queryParts = params[1].split(";");
        String query = queryParts[queryParts.length - 1];

        try {
            String connString = K12NetUserReferences.getConnectionAddress() + "/GWCore.Web/api/Portals/NotificationResponse/"+confirm+"/"+query;
            URL url = new URL(connString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        conn.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return response.toString();
            } else {
                System.out.println("GET request not worked");
                return null;
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        // do something with the result
    }
}
