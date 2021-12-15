package com.k12nt.k12netframe.fcm;

import android.os.AsyncTask;

import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;

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
            String connString = K12NetUserReferences.getConnectionAddress() + "/Authentication_JSON_AppService.axd/NotificationResponse";
            URL url = new URL(connString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");

            Map<String, Object> headerParams = new LinkedHashMap<>();
            headerParams.put("result", confirm);
            headerParams.put("notificationID", query);

            for (Map.Entry<String, Object> param : headerParams.entrySet()) {
                conn.addRequestProperty(param.getKey(), String.valueOf(param.getValue()));
            }

            BufferedWriter bw = null;
            try {
                bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
                bw.write("");
                bw.flush();
                bw.close();
            } catch (IOException exception) {
                throw exception;
            } finally {
                try {
                    if (bw != null) {
                        bw.close();
                    }
                } catch (IOException ex) {

                }
            }

            BufferedReader in = null;
            String inputLine;
            StringBuilder body;
            try {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                body = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    body.append(inputLine);
                }
                in.close();
                String result = body.toString();

                return result;
            } catch (IOException ioe) {
                throw ioe;
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ex) {

                }
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
