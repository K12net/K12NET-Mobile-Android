package com.k12nt.k12netframe;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.view.Window;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.location.LocationSettingsStates;
import com.google.firebase.messaging.FirebaseMessaging;
import com.k12nt.k12netframe.async_tasks.AsyncCompleteListener;
import com.k12nt.k12netframe.async_tasks.HTTPAsyncTask;
import com.k12nt.k12netframe.async_tasks.TaskHandler;
import com.k12nt.k12netframe.attendance.AttendanceManager;
import com.k12nt.k12netframe.fcm.SetUserStateTask;
import com.k12nt.k12netframe.utils.definition.K12NetStaticDefinition;
import com.k12nt.k12netframe.utils.helper.K12NetHelper;
import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;
import com.k12nt.k12netframe.utils.webConnection.K12NetHttpClient;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.HttpCookie;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
        isLogin = false;

        if(intent != null && intent.getExtras() != null) {
            super.onNewIntent(intent);
            this.setIntent(intent);

            if (chkRememberMe.isChecked()) {
                StartLoginOperation();
            } else {
                checkNotificationExist(intent);
            }
        }
    }

    private boolean checkNotificationExist(Intent intent) {
        if(intent == null || intent.getExtras() == null) return false;

        if (intent.getExtras().getInt("requestID",0) != 0) {
            WebViewerActivity.body = intent.getExtras().getString("body","");
            WebViewerActivity.title = intent.getExtras().getString("title","");

            final String body = WebViewerActivity.body;
            final String title = WebViewerActivity.title;
            final String query = intent.getStringExtra("query");
            final Intent intentOfLogin = this.getIntent();

            NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(intent.getExtras().getInt("requestID",0));

            Runnable confirmation = () -> {
                String url = K12NetUserReferences.getConnectionAddress();

                intentOfLogin.putExtra("intent","");
                intentOfLogin.putExtra("portal","");
                intentOfLogin.putExtra("query","");
                intentOfLogin.putExtra("body","");
                intentOfLogin.putExtra("title","");
                intentOfLogin.putExtra("requestID",0);

                new SetUserStateTask().execute(isConfirmed ? "1" : "0",query);
            };

            setConfirmDialog(title,body,confirmation);

            return true;
        }

        WebViewerActivity.intent = intent.getExtras().getString("intent","");

        if(!WebViewerActivity.intent.equals("")) {
            WebViewerActivity.portal = intent.getExtras().getString("portal","");
            WebViewerActivity.query = intent.getExtras().getString("query","");
            WebViewerActivity.body = intent.getExtras().getString("body","");
            WebViewerActivity.title = intent.getExtras().getString("title","");

            final String webPart = WebViewerActivity.intent;
            final String portal = WebViewerActivity.portal;
            final String query = WebViewerActivity.query;
            final String body = WebViewerActivity.body;
            final String title = WebViewerActivity.title;

            final Intent intentOfLogin = this.getIntent();

            Runnable confirmation = () -> {
                String url = K12NetUserReferences.getConnectionAddress();

                intentOfLogin.putExtra("intent","");
                intentOfLogin.putExtra("portal","");
                intentOfLogin.putExtra("query","");
                intentOfLogin.putExtra("body","");
                intentOfLogin.putExtra("title","");

                if (isConfirmed) {
                    url += String.format("/Default.aspx?intent=%1$s&portal=%2$s&query=%3$s",webPart,portal,query);
                    WebViewerActivity.previousUrl = WebViewerActivity.startUrl;

                    navigateTo(url);
                }
            };

            if (WebViewerActivity.startUrl != null && (WebViewerActivity.startUrl.equals(K12NetUserReferences.getConnectionAddress()) || WebViewerActivity.startUrl.contains("Login.aspx"))) {
                isConfirmed = true;
                confirmation.run();
            } else {
                setConfirmDialog(title,body+System.getProperty("line.separator")+System.getProperty("line.separator") + this.getString(R.string.navToNotify),confirmation);
            }

            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if(data != null) {
            final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
            switch (requestCode) {
                case AttendanceManager.REQUEST_CHECK_SETTINGS:
                    switch (resultCode) {
                        case Activity.RESULT_OK:
                            // All required changes were successfully made
                            break;
                        case Activity.RESULT_CANCELED:
                            // The user was asked to change settings, but chose not to
                            break;
                        default:
                            break;
                    }
                    break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume(){
        isLogin = false;
        K12NetSettingsDialogView.setLanguageToDefault(getBaseContext());

        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            String manufacturer = Build.MANUFACTURER;

            if(manufacturer != null && (manufacturer.toLowerCase().contains("huawe") || "huawe".equalsIgnoreCase(manufacturer))) {

            } else {
                FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                    if(task.isComplete() && task.isSuccessful()) K12NetUserReferences.setDeviceToken(task.getResult());
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

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

        login_button.setOnClickListener(arg0 -> {
            isLoginRetry = false;
            checkCurrentVersion();
        });

        Button settings_button = (Button) findViewById(R.id.btn_settings);
        settings_button.setOnClickListener(arg0 -> {
            K12NetSettingsDialogView dialogView = new K12NetSettingsDialogView(arg0.getContext(), this);
            dialogView.createContextView(null);

            dialogView.setOnDismissListener(dialog -> {

                K12NetSettingsDialogView.setLanguageToDefault(getBaseContext());

                recreate();

            });

            dialogView.show();
        });

        Button resetPassword = (Button) findViewById(R.id.btnResetPassword);
        resetPassword.setOnClickListener(arg0 -> {
            Intent webIntent = new Intent(arg0.getContext(), WebViewerActivity.class);
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            WebViewerActivity.startUrl = K12NetUserReferences.getConnectionAddress() + "/ResetPassword.aspx";
            arg0.getContext().startActivity(webIntent);
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
        currentVersion = pInfo == null ? "" : pInfo.versionName;
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

        K12NetHttpClient.setCookie("UICulture", K12NetUserReferences.getNormalizedLanguageCode(), strUTCDate);
        K12NetHttpClient.setCookie("Culture", K12NetUserReferences.getNormalizedLanguageCode(), strUTCDate);

        this.login();
    }

    public static Boolean isLogin = false;
    public static Boolean isLoginRetry = false;
    public void login() {
        try {
            isLogin = false;

            String connString = K12NetUserReferences.getConnectionAddress() + "/Authentication_JSON_AppService.axd/Login";
            HTTPAsyncTask loginTask = new HTTPAsyncTask(context, connString, "Login");

            loginTask.setHeader("Content-type", "application/json;charset=UTF-8");
            loginTask.setHeader("Atlas-DeviceID", K12NetUserReferences.getDeviceToken());
            loginTask.setHeader("Atlas-DeviceTypeID", K12NetStaticDefinition.ASISTO_ANDROID_APPLICATION_ID);
            loginTask.setHeader("Atlas-DeviceModel", GetDeviceModel());

            loginTask.setJson("userName", K12NetUserReferences.getUsername().trim());
            loginTask.setJson("password", K12NetUserReferences.getPassword());
            loginTask.setJson("createPersistentCookie", "false");

            loginTask.setOnCompleteListener(this);

            loginTask.execute();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String GetDeviceModel() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (Build.MODEL.startsWith(Build.MANUFACTURER)) {
            model =  Build.MODEL;
        } else {
            model = manufacturer + " [" + model + "] ";
        }
        String deviceName = "";

        try{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                deviceName = Settings.Global.getString(getContentResolver(), Settings.Global.DEVICE_NAME);
            }
        } catch(Exception e) {

        }

        if(deviceName != null && !deviceName.equals("")) {
            model += " [" + deviceName + "] ";
        }

        if(Build.SERIAL != null && !Build.SERIAL.equals("unknown")) {
            model += " [" + Build.SERIAL + "] ";
        }

        return  model;
    }

    @Override
    public void asyncTaskCompleted(HTTPAsyncTask completedTask){
        String taskName = completedTask.GetName();

        if(taskName != null && taskName.equals("Login")) {
            try {
                JSONObject responseJSON = new JSONObject(completedTask.GetResult());

                isLogin = responseJSON.optBoolean("d", false);

                if(!isLogin && !isLoginRetry) {
                    isLoginRetry = true;

                    String connString = K12NetUserReferences.getConnectionAddress();

                    if (connString.equals("https://okul.k12net.com")) {
                        K12NetUserReferences.setConnectionAddress("https://azure.k12net.com");
                    } else {
                        K12NetUserReferences.setConnectionAddress("https://okul.k12net.com");
                    }

                    this.login();

                    return;
                }

                if (isLogin == false) {
                    Toast.makeText(context, R.string.login_failed, Toast.LENGTH_SHORT).show();
                } else {
                    AttendanceManager.Instance().initialize(this, new TaskHandler() {
                        @Override
                        public void onTaskCompleted(String result) {
                            if (isLoginRetry) {

                                String connString = K12NetUserReferences.getConnectionAddress();

                                if (connString.equals("https://azure.k12net.com")) {
                                    if(K12NetUserReferences.getLanguageCode().equals("tr")) K12NetUserReferences.setLanguage("en");
                                } else {
                                    K12NetUserReferences.setLanguage("tr");
                                }

                            }

                            final LoginActivity currentActivity = LoginActivity.this;

                            String connString = K12NetUserReferences.getConnectionAddress() + "/Authentication_JSON_AppService.axd/SetLanguage";

                            try {
                                HTTPAsyncTask langTask = new HTTPAsyncTask(context, connString,"SetLanguage");

                                langTask.setHeader("LanguageCode", K12NetUserReferences.getNormalizedLanguageCode());

                                List<String> cookies = completedTask.GetConnection().getHeaderFields().get("Set-Cookie");

                                if(cookies != null) {
                                    for (String cookie : cookies) {
                                        if(cookie.startsWith(".")) {//Set Authentication cookie
                                            langTask.GetConnection().setRequestProperty("Cookie", cookie.replaceAll("; HttpOnly","").replaceAll("HttpOnly",""));
                                        }
                                    }
                                }

                                langTask.setOnCompleteListener(currentActivity);

                                langTask.execute();

                                K12NetUserReferences.LANG_UPDATED = false;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if (result.equals("PermissionRequested")) return;

                            Intent intentOfLogin = currentActivity.getIntent();
                            boolean hasNotification = checkNotificationExist(intentOfLogin);

                            if(!hasNotification) {
                                String startUrl = K12NetUserReferences.getConnectionAddress();

                                navigateTo(startUrl + "/Logon.aspx");
                            }
                        }
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            //Toast.makeText(context, completedTask.GetResult(), Toast.LENGTH_SHORT).show();
        }
    }

    public Boolean isConfirmed = false;
    private void LoginCompleted() {
    }

    private void navigateTo(String url) {
        WebViewerActivity.startUrl = url;
        Intent intent = new Intent(context, WebViewerActivity.class);
        context.startActivity(intent);
    }

    public synchronized void setConfirmDialog(String title, String message, final Runnable func) {
        isConfirmed = false;

        try {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (!isFinishing()){
                        final AlertDialog.Builder confirmDialog = new AlertDialog.Builder(LoginActivity.this);

                        confirmDialog.setTitle(title);
                        confirmDialog.setMessage(message);
                        confirmDialog.setCancelable(false);

                        confirmDialog.setPositiveButton(context.getString(R.string.yes), (dialog, which) -> {
                            isConfirmed = true;

                            func.run();
                        });

                        confirmDialog.setNegativeButton(context.getString(R.string.no), (dialog, which) -> {
                            isConfirmed = false;

                            func.run();
                        });

                        confirmDialog.show();
                    }
                }
            });


            /*if (!isFinishing()) {
                final Handler diloagResultWaitHandler = new Handler(Looper.getMainLooper())
                {
                    @Override
                    public void handleMessage(Message mesg)
                    {

                    }
                };

                final AlertDialog.Builder confirmDialog = new AlertDialog.Builder(this);

                confirmDialog.setTitle(title);
                confirmDialog.setMessage(message);
                confirmDialog.setCancelable(false);

                confirmDialog.setPositiveButton(context.getString(R.string.yes), (dialog, which) -> {
                    isConfirmed = true;

                    func.run();
                    diloagResultWaitHandler.sendMessage(diloagResultWaitHandler.obtainMessage());
                });

                confirmDialog.setNegativeButton(context.getString(R.string.no), (dialog, which) -> {
                    isConfirmed = false;

                    func.run();
                    diloagResultWaitHandler.sendMessage(diloagResultWaitHandler.obtainMessage());
                });

                confirmDialog.show();

                try{
                    Looper.loop();
                }
                catch(RuntimeException e){}
            }*/

        } catch (Exception e) {
            e.printStackTrace();
        }
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
                else if (currentVersionInt[2] < latestVersionInt[2] && currentVersionInt[1] <= latestVersionInt[1] && currentVersionInt[0] <= latestVersionInt[0]){
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
            builder.setPositiveButton(R.string.update, (dialog, which) -> {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse
                        (K12NetStaticDefinition.MARKET_APP_ADDRESS)));
                dialog.dismiss();
            });

            builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
                //background.start();
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
                builder.setPositiveButton(R.string.update, (dialog, which) -> {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse
                            (K12NetStaticDefinition.MARKET_APP_ADDRESS)));
                    dialog.dismiss();
                });

                builder.setNegativeButton(R.string.ok, (dialog, which) -> {
                    K12NetUserReferences.setWarnedVersionString(latestVersion);
                    StartLoginOperation();
                });

                builder.setCancelable(false);
                dialog = builder.show();
            }

        } catch (Exception e) {
            StartLoginOperation();
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == AttendanceManager.REQUEST_PERMISSIONS_REQUEST_CODE) {
            Runnable confirmation = () -> {
                if(!isConfirmed) {
                    final LoginActivity currentActivity = LoginActivity.this;
                    Intent intentOfLogin = currentActivity.getIntent();
                    boolean hasNotification = checkNotificationExist(intentOfLogin);

                    if(!hasNotification) {
                        String startUrl = K12NetUserReferences.getConnectionAddress();

                        navigateTo(startUrl + "/Logon.aspx");
                    }
                    return;
                }
                // Build intent that displays the App settings screen.

                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                intent.setData(uri);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
            };

            if (grantResults.length <= 0) {
                setConfirmDialog(getString(R.string.location_permission),getString(R.string.locationAccessAppSettings),confirmation);
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                AttendanceManager.Instance().initialize(this, new TaskHandler() {

                    @Override
                    public void onTaskCompleted(String result) {

                    }
                });
            } else {
                // Permission denied.
                // Notify the user via a SnackBar that they have rejected a core permission for the
                setConfirmDialog(getString(R.string.location_permission),getString(R.string.locationAccessAppSettings),confirmation);
            }
        }
    }
}
