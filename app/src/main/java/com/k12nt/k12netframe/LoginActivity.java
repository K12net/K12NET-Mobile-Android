package com.k12nt.k12netframe;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Configuration;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import android.webkit.CookieManager;
import android.widget.Toast;

import java.net.HttpCookie;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import com.google.firebase.iid.FirebaseInstanceId;
import com.k12nt.k12netframe.async_tasks.AsyncCompleteListener;
import com.k12nt.k12netframe.async_tasks.HTTPAsyncTask;
import com.k12nt.k12netframe.utils.definition.K12NetStaticDefinition;
import com.k12nt.k12netframe.utils.helper.K12NetHelper;
import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;
import com.k12nt.k12netframe.utils.webConnection.K12NetHttpClient;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class LoginActivity extends Activity implements AsyncCompleteListener {

    final Context context = this;

    boolean isUptoDate = false;

    EditText username;
    EditText password;
    CheckBox chkRememberMe;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // refresh your views here
        super.onConfigurationChanged(newConfig);
    }

    protected void onNewIntent(Intent intent) {

        if(intent != null && intent.getExtras() != null) {
            super.onNewIntent(intent);
            this.setIntent(intent);

            if (chkRememberMe.isChecked()) {
                StartLoginOperation();
            }
        }
    }

    @Override
    public void onResume(){
        K12NetSettingsDialogView.setLanguageToDefault(getBaseContext());

        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(LoginActivity.this, instanceIdResult -> {
            String newToken = instanceIdResult.getToken();

            K12NetUserReferences.setDeviceToken(newToken);
        });

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        K12NetUserReferences.initUserReferences(getApplicationContext());

        if(K12NetUserReferences.getLanguageCode() == null){
            K12NetUserReferences.setLanguage(this.getString(R.string.localString));
        }

        K12NetHttpClient.resetBrowser();
        K12NetSettingsDialogView.setLanguageToDefault(getBaseContext());

        setContentView(R.layout.k12net_login_layout);

        username = (EditText) findViewById(R.id.txt_login_username);
        password = (EditText) findViewById(R.id.txt_login_password);
        chkRememberMe = (CheckBox) findViewById(R.id.chk_remember_me);

        chkRememberMe.setChecked(K12NetUserReferences.getRememberMe());

        username.setText(K12NetUserReferences.getUsername());
        if (chkRememberMe.isChecked()) {
            password.setText(K12NetUserReferences.getPassword());
        }

        Button login_button = (Button) findViewById(R.id.btn_login_submit);

        login_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                checkCurrentVersion();
            }
        });

        Button settings_button = (Button) findViewById(R.id.btn_settings);
        settings_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                K12NetSettingsDialogView dialogView = new K12NetSettingsDialogView(arg0.getContext());
                dialogView.createContextView(null);

                dialogView.setOnDismissListener(new OnDismissListener() {

                    @Override
                    public void onDismiss(final DialogInterface dialog) {

                        K12NetSettingsDialogView.setLanguageToDefault(getBaseContext());

                        recreate();

                    }
                });

                dialogView.show();
            }
        });

        Button resetPassword = (Button) findViewById(R.id.btnResetPassword);
        resetPassword.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent webIntent = new Intent(arg0.getContext(), WebViewerActivity.class);
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                WebViewerActivity.startUrl = "https://okul.k12net.com/ResetPassword.aspx";
                arg0.getContext().startActivity(webIntent);
            }
        });

        if (chkRememberMe.isChecked()) {
            checkCurrentVersion();
        }
    }

    String currentVersion, latestVersion;
    Dialog dialog;
    private void checkCurrentVersion(){
        PackageManager pm = this.getPackageManager();
        PackageInfo pInfo = null;

        try {
            pInfo =  pm.getPackageInfo(this.getPackageName(),0);

        } catch (PackageManager.NameNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        currentVersion = pInfo.versionName;
        //check if version number only has 2 segment
        if(K12NetHelper.findPattermCount(currentVersion, ".") < 2){
            currentVersion += ".0";
        }

        K12NetUserReferences.setWarnedVersionString(currentVersion);

        new GetLatestVersion(this).execute();

    }

    private void StartLoginOperation() {
        K12NetUserReferences.setUsername(username.getText().toString());
        K12NetUserReferences.setPassword(password.getText().toString());
        K12NetUserReferences.setRememberMe(chkRememberMe.isChecked());

        K12NetHttpClient.resetBrowser();

        CookieManager cookieManager = CookieManager.getInstance();

        List<HttpCookie> cookies = K12NetHttpClient.getCookieList();
        if (cookies != null) {
            for (HttpCookie cookie : cookies) {
                if (cookie.getName().contains("NotCompletedPollCount")){
                    String cookieString = cookie.getName() + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT" + "; Domain=" + cookie.getDomain();
                    cookieManager.setCookie(cookie.getDomain(), cookieString);
                }
            }
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 1);
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String strUTCDate = dateFormatter.format(cal.getTime());

        K12NetHttpClient.setCookie("UICulture", K12NetUserReferences.getLanguageCode(), strUTCDate);
        K12NetHttpClient.setCookie("Culture", K12NetUserReferences.getLanguageCode(), strUTCDate);

        this.login();
    }

    public static Boolean isLogin = false;
    public void login() {
        try {
            isLogin = false;

            String connString = K12NetUserReferences.getConnectionAddress() + "/Authentication_JSON_AppService.axd/Login";
            HTTPAsyncTask loginTask = new HTTPAsyncTask(context, connString, "Login");

            loginTask.setHeader("Content-type", "application/json;charset=UTF-8");
            loginTask.setHeader("Atlas-DeviceID", K12NetUserReferences.getDeviceToken());
            loginTask.setHeader("Atlas-DeviceTypeID", K12NetStaticDefinition.ASISTO_ANDROID_APPLICATION_ID);

            loginTask.setJson("userName", K12NetUserReferences.getUsername());
            loginTask.setJson("password", K12NetUserReferences.getPassword());
            loginTask.setJson("createPersistentCookie", "false");

            loginTask.setOnCompleteListener(this);

            loginTask.execute();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void asyncTaskCompleted(HTTPAsyncTask completedTask){
        String taskName = completedTask.GetName();

        if(taskName == "Login") {
            try {
                JSONObject responseJSON = new JSONObject(completedTask.GetResult());

                isLogin = responseJSON.optBoolean("d", false);

                this.LoginCompleted();

                if(isLogin && K12NetUserReferences.LANG_UPDATED) {
                    String connString = K12NetUserReferences.getConnectionAddress() + "/Authentication_JSON_AppService.axd/SetLanguage";

                    HTTPAsyncTask langTask = new HTTPAsyncTask(context, connString,"SetLanguage");

                    langTask.setHeader("LanguageCode", K12NetUserReferences.getLanguageCode());

                    List<String> cookies = completedTask.GetConnection().getHeaderFields().get("Set-Cookie");

                    if(cookies != null) {
                        for (String cookie : cookies) {
                            if(cookie.startsWith(".")) {//Set Authentication cookie
                                langTask.GetConnection().setRequestProperty("Cookie", cookie.replaceAll("; HttpOnly","").replaceAll("HttpOnly",""));
                            }
                        }
                    }

                    langTask.setOnCompleteListener(this);

                    langTask.execute();

                    K12NetUserReferences.LANG_UPDATED = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            //Toast.makeText(context, completedTask.GetResult(), Toast.LENGTH_SHORT).show();
        }
    }

    private Boolean isConfirmed = false;
    private void LoginCompleted() {
        if (isLogin == false) {
            Toast.makeText(context, R.string.login_failed, Toast.LENGTH_SHORT).show();
        } else {
            final Activity currentActivity = LoginActivity.this;
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

                    Runnable confirmCompleted = new Runnable() {
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

                                navigateTo(url + "/Logon.aspx");
                            }
                        }
                    };

                    if (WebViewerActivity.startUrl == startUrl) {
                        isConfirmed = true;
                        confirmCompleted.run();
                    } else {
                        setConfirmDialog(title,body+System.getProperty("line.separator")+System.getProperty("line.separator") + context.getString(R.string.navToNotify),confirmCompleted);
                    }

                    return;
                }
            }

            navigateTo(startUrl + "/Logon.aspx");
        }
    }

    private void navigateTo(String url) {
        WebViewerActivity.startUrl = url;
        Intent intent = new Intent(context, WebViewerActivity.class);
        context.startActivity(intent);
    }

    private synchronized void setConfirmDialog(String title, String message, final Runnable func) {
        isConfirmed = false;

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setPositiveButton(context.getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isConfirmed = true;

                func.run();
            }
        });

        builder.setNegativeButton(context.getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isConfirmed = false;

                func.run();
            }
        });

        builder.show();
    }

    private class GetLatestVersion extends AsyncTask<String, String, JSONObject> {

        private Dialog progress_dialog;
        Context ctx;

        public GetLatestVersion(Context ctx) {
            this.ctx = ctx;
        }

        @Override
        protected void onPreExecute() {

            progress_dialog = new Dialog(ctx, R.style.K12NET_ModalLayout);
            progress_dialog.setContentView(R.layout.loading_view_layout);
            progress_dialog.show();

            super.onPreExecute();
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            try {
//It retrieves the latest version by scraping the content of current version from play store at runtime

                Document doc = Jsoup.connect("http://fs.k12net.com/mobile/files/versions.k12net.txt").userAgent("Mozilla").header("Cache-control", "no-cache").header("Cache-store", "no-store").timeout(4000).get();

                if(doc.getElementsByTag("android").size() > 0) {
                    latestVersion = doc.getElementsByTag("android").first().attr("version");
                }

                //check if version number only has 2 segment25*
                if(latestVersion != null && K12NetHelper.findPattermCount(latestVersion, ".") < 2){
                    latestVersion += ".0";
                }

            }catch (Exception e){
                e.printStackTrace();

            }

            return new JSONObject();
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            if(progress_dialog.isShowing()) {
                try {
                    progress_dialog.dismiss();
                }
                catch (Exception ex) {

                }
            }

            isUptoDate = true;
            if(latestVersion!=null) {
                int[] currentVersionInt = getVersionList(currentVersion);
                int[] latestVersionInt = getVersionList(latestVersion);

                if (currentVersionInt[1] < latestVersionInt[1] || currentVersionInt[0] < latestVersionInt[0]){
                    isUptoDate = false;

                    showUpdateDialog();
                }
                else if (currentVersionInt[2] < latestVersionInt[2]){
                    isUptoDate = true;

                    showWarningDialog(latestVersion);
                }
                else {
                    StartLoginOperation();
                }
            }
            else {
                StartLoginOperation();
            }

            super.onPostExecute(jsonObject);
        }

        private int[] getVersionList(String version) {
            String[] versionSList = version.split("\\.");
            int[] versionList = new int[versionSList.length];
            for(int i = 0; i < versionList.length;i++){
                versionList[i] = K12NetHelper.getInt(versionSList[i], 0);
            }
            return versionList;
        }
    }

    private void showUpdateDialog(){
        try {

            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.appName);
            builder.setMessage(R.string.newUpdateAvailable);
            builder.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse
                            (K12NetStaticDefinition.MARKET_APP_ADDRESS)));
                    dialog.dismiss();
                }
            });

            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //background.start();
                }
            });

            builder.setCancelable(false);
            dialog = builder.show();

        } catch (Exception e) {
            StartLoginOperation();
        }
    }

    private void showWarningDialog(final String latestVersion){
        try {

            if(K12NetUserReferences.getWarnedVersionString().compareTo(latestVersion) != 1) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.appName);
                builder.setMessage(R.string.newUpdateAvailableWarning);
                builder.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse
                                (K12NetStaticDefinition.MARKET_APP_ADDRESS)));
                        dialog.dismiss();
                    }
                });

                builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        K12NetUserReferences.setWarnedVersionString(latestVersion);
                        StartLoginOperation();
                    }
                });

                builder.setCancelable(false);
                dialog = builder.show();
            }

        } catch (Exception e) {
            StartLoginOperation();
        }
    }

}
