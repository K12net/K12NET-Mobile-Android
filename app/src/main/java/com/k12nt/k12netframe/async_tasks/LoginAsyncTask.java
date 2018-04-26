package com.k12nt.k12netframe.async_tasks;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.k12nt.k12netframe.R;
import com.k12nt.k12netframe.WebViewerActivity;
import com.k12nt.k12netframe.utils.definition.K12NetStaticDefinition;
import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;
import com.k12nt.k12netframe.utils.webConnection.K12NetHttpClient;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

public class LoginAsyncTask extends AsistoAsyncTask {

    Activity currentActivity;
    Context ctx;
    Dialog progress_dialog;
    String username;
    String password;

    private Boolean isLogin = false;

    public LoginAsyncTask(Context ctx, String username, String password, Activity currentActivity) {
        this.currentActivity = currentActivity;
        this.ctx = ctx;
        this.username = username;
        this.password = password;
    }

    @Override
    public void onPreExecute() {
        progress_dialog = new Dialog(ctx, R.style.K12NET_ModalLayout);
        progress_dialog.setContentView(R.layout.loading_view_layout);
        progress_dialog.show();
    }

    @Override
    protected Void doInBackground(Void... params) {

        isLogin = login();

        return null;
    }

    public static boolean login() {
        boolean success = false;
        HttpResponse response = null;
        try {
            JSONObject json = new JSONObject();
            json.put("userName", K12NetUserReferences.getUsername());
            json.put("password", K12NetUserReferences.getPassword());
            json.put("createPersistentCookie", "false");

            String connString = K12NetUserReferences.getConnectionAddress() + "/Authentication_JSON_AppService.axd/Login";
            HttpPost httpost = new HttpPost(connString);
            StringEntity entity = new StringEntity(json.toString(), HTTP.UTF_8);
            httpost.setEntity(entity);
            httpost.setHeader("Content-type", "application/json;charset=UTF-8");
            httpost.setHeader("Atlas-DeviceID", K12NetUserReferences.getDeviceToken());
            httpost.setHeader("Atlas-DeviceTypeID", K12NetStaticDefinition.ASISTO_ANDROID_APPLICATION_ID);

            String line = K12NetHttpClient.execute(httpost);

            JSONObject responseJSON = new JSONObject(line);

            success = responseJSON.optBoolean("d", false);

        } catch (Exception ex) {
            success = false;
        }

        return success;

    }


    @Override
    protected void onAsyncComplete() {
        if(progress_dialog.isShowing()) {
            try {
                progress_dialog.dismiss();
            }
            catch (Exception ex) {

            }
        }

        if (isLogin == false) {
            Toast.makeText(ctx, R.string.login_failed, Toast.LENGTH_SHORT).show();
        } else {
            WebViewerActivity.startUrl = K12NetUserReferences.getConnectionAddress();
            Intent intent = new Intent(ctx, WebViewerActivity.class);
            ctx.startActivity(intent);
        }
    }
}
