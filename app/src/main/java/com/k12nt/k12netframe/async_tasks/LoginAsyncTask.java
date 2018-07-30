package com.k12nt.k12netframe.async_tasks;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.webkit.CookieManager;
import android.widget.Toast;
import com.k12nt.k12netframe.R;
import com.k12nt.k12netframe.WebViewerActivity;
import com.k12nt.k12netframe.fcm.MyFirebaseInstanceIDService;
import com.k12nt.k12netframe.utils.definition.K12NetStaticDefinition;
import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;
import com.k12nt.k12netframe.utils.webConnection.K12NetHttpClient;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.util.List;

public class LoginAsyncTask extends AsistoAsyncTask {

    Activity currentActivity;
    Context ctx;
    Dialog progress_dialog;
    String username;
    String password;
    Boolean isConfirmed = false;

    public static Boolean isLogin = false;

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
        if(!((Activity) ctx).isFinishing())
        {
            progress_dialog.show();
        }
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
            CookieManager cookieManager = CookieManager.getInstance();
            List<Cookie> cookies = K12NetHttpClient.getCookieList();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().contains("NotCompletedPollCount")){
                        String cookieString = cookie.getName() + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT" + "; Domain=" + cookie.getDomain();
                        cookieManager.setCookie(cookie.getDomain(), cookieString);
                    }
                }
            }

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
            MyFirebaseInstanceIDService firebaseInstanceIDService = new MyFirebaseInstanceIDService();
            firebaseInstanceIDService.onTokenRefresh();

            Intent intentOfLogin = currentActivity.getIntent();
            String startUrl = K12NetUserReferences.getConnectionAddress();

            WebViewerActivity.previousUrl = null;
            if(intentOfLogin != null && intentOfLogin.getExtras() != null) {
                final String intent = intentOfLogin.getExtras().getString("intent","");

                if(intent != "") {
                    final String portal = intentOfLogin.getExtras().getString("portal","");
                    final String query = intentOfLogin.getExtras().getString("query","");
                    final String body = intentOfLogin.getExtras().getString("body","");
                    final String title = intentOfLogin.getExtras().getString("title","");

                    Runnable confirmComplated = new Runnable() {
                        @Override
                        public void run() {
                            Intent intentOfLogin = currentActivity.getIntent();
                            String url = K12NetUserReferences.getConnectionAddress();

                            intentOfLogin.putExtra("intent","");
                            intentOfLogin.putExtra("portal","");
                            intentOfLogin.putExtra("query","");
                            intentOfLogin.putExtra("body","");
                            intentOfLogin.putExtra("title","");

                            if (isConfirmed) {
                                url += String.format("/Default.aspx?intent=%1$s&portal=%2$s&query=%3$s",intent,portal,query);
                                WebViewerActivity.previousUrl = WebViewerActivity.startUrl;

                                navigateTo(url);
                            }
                        }
                    };

                    if (WebViewerActivity.startUrl == startUrl) {
                        isConfirmed = true;
                        confirmComplated.run();
                    } else {
                        setConfirmDialog(title,body+System.getProperty("line.separator")+System.getProperty("line.separator") + ctx.getString(R.string.navToNotify),confirmComplated);
                    }

                    return;
                }
            }

            navigateTo(startUrl);
        }

    }

    private void navigateTo(String url) {
        WebViewerActivity.startUrl = url;
        Intent intent = new Intent(ctx, WebViewerActivity.class);
        ctx.startActivity(intent);
    }

    private synchronized void setConfirmDialog(String title, String message, final Runnable func) {
        isConfirmed = false;

        final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);

        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setPositiveButton(ctx.getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isConfirmed = true;

                func.run();
            }
        });

        builder.setNegativeButton(ctx.getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isConfirmed = false;

                func.run();
            }
        });

        builder.show();
    }
}
