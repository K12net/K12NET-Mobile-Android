package com.k12nt.k12netframe.async_tasks;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.k12nt.k12netframe.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

public class HTTPAsyncTask extends AsyncTask<String, Void, String> {

    private AsyncCompleteListener asyncListener = null;
    public Context ctx;
    public Dialog progress_dialog;
    public URL url;
    public String RequestMethod = "POST";
    private Map<String,Object> headerParams;
    private JSONObject jsonObject ;
    private String name ;
    private String result ;
    public Bitmap ResultAsBitmap;
    private HttpURLConnection conn = null;

    public HTTPAsyncTask(Context ctx, String connString, String name) throws MalformedURLException {
        this.ctx = ctx;
        this.name = name;
        this.url = new URL(connString);
    }

    public void setOnCompleteListener(AsyncCompleteListener listener) {
        this.asyncListener = listener;
    }

    public void setJson(String key, String value) throws JSONException {
        if(jsonObject == null) jsonObject = new JSONObject();
        this.jsonObject.put(key, value);
    }

    public  void setHeader(String key, String value) {
        if(headerParams == null) headerParams = new LinkedHashMap<>();
        this.headerParams.put(key, value);
    }

    @Override
    public void onPreExecute() {
        if(this.ctx == null) return;

        progress_dialog = new Dialog(ctx, R.style.K12NET_ModalLayout);
        progress_dialog.setContentView(R.layout.loading_view_layout);

        if(!((Activity) ctx).isFinishing())
        {
            progress_dialog.show();
        }
    }

    @Override
    protected String doInBackground(String... urls) {
        try {
            conn = this.GetConnection();
            conn.setRequestMethod(RequestMethod);

            if (RequestMethod.equals("GET")) {
                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) { // success

                    if(name != null && name.equals("SchoolLogo")) {
                        InputStream is = conn.getInputStream();
                        BufferedInputStream bis = new BufferedInputStream(is);
                        ResultAsBitmap = BitmapFactory.decodeStream(bis);
                        bis.close();
                        is.close();

                        return "SchoolLogo";
                    }

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
            }

            if(jsonObject != null) {

                if (headerParams != null){
                    for (Map.Entry<String,Object> param : headerParams.entrySet()) {
                        conn.addRequestProperty(param.getKey(), String.valueOf(param.getValue()));
                    }
                }

                this.sendData(conn, jsonObject.toString());

                /*conn.connect();

                String m = conn.getResponseMessage();
                int c = conn.getResponseCode();
                String e = this.read(conn.getErrorStream());*/

                if(asyncListener != null)
                    return this.read(conn.getInputStream());

                return null;
            } else if (headerParams != null) {
                for (Map.Entry<String,Object> param : headerParams.entrySet()) {
                    conn.addRequestProperty(param.getKey(), String.valueOf(param.getValue()));
                }

                this.sendData(conn, "");

                if(asyncListener != null)
                    return this.read(conn.getInputStream());

                return null;
            } else {
                conn.setDoOutput(true);

                conn.connect();

                if(asyncListener != null)
                    return this.read(conn.getInputStream());

                return null;

            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }finally {
            if(conn != null) conn.disconnect();
        }
    }

    private void sendData(HttpURLConnection con, String data) throws IOException {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(con.getOutputStream(), "UTF-8"));
            bw.write(data);
            bw.flush();
            bw.close();
        } catch(IOException exception) {
            throw exception;
        } finally {
            this.closeQuietly(bw);
        }
    }

    private String read(InputStream is) throws IOException {
        if(is == null) return null;

        BufferedReader in = null;
        String inputLine;
        StringBuilder body;
        try {
            in = new BufferedReader(new InputStreamReader(is));

            body = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                body.append(inputLine);
            }
            in.close();

            return body.toString();
        } catch(IOException ioe) {
            throw ioe;
        } finally {
            this.closeQuietly(in);
        }
    }

    protected void closeQuietly(Closeable closeable) {
        try {
            if( closeable != null ) {
                closeable.close();
            }
        } catch(IOException ex) {

        }
    }

    public String GetResult() {
        return  this.result;
    }

    public String GetName() {
        return  this.name;
    }

    public void Reset() {
        conn = null;
    }

    public HttpURLConnection GetConnection() throws IOException {
        if(conn == null) conn = (HttpURLConnection) url.openConnection();
        return  conn;
    }

    @Override
    protected void onPostExecute(String result) {
        this.result = result;

        if(progress_dialog != null && progress_dialog.isShowing()) {
            try {
                progress_dialog.dismiss();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if(asyncListener != null) asyncListener.asyncTaskCompleted(this);
    }

}
