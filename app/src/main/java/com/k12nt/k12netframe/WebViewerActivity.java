package com.k12nt.k12netframe;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.LocationSettingsStates;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.builders.Actions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.k12nt.k12netframe.async_tasks.AsistoAsyncTask;
import com.k12nt.k12netframe.async_tasks.AsyncCompleteListener;
import com.k12nt.k12netframe.async_tasks.HTTPAsyncTask;
import com.k12nt.k12netframe.async_tasks.K12NetAsyncCompleteListener;
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpCookie;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import me.leolin.shortcutbadger.ShortcutBadger;

public class WebViewerActivity extends K12NetActivity implements K12NetAsyncCompleteListener {

    public static String currentState = "init";
    public static Boolean isLogin = false;
    public static String loginState = "";
    public static boolean logoutIsProgress = false;
    
    public static String startUrl = "";
    public static String previousUrl = "";

    WebView webview = null;
    WebView main_webview = null;
    WebView popup_webview = null;

    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;
    private final static int FILECHOOSER_RESULTCODE = 1;

    public Boolean isConfirmed = false;
    private Dialog progress_dialog;

    private static final String TAG = "WebViewerActivity";
    boolean hasWriteAccess = false;
    boolean hasReadAccess = false;
    static boolean screenAlwaysOn = false;

    private Intent fileSelectorIntent = null;
    private String contentStr = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        initUserReferences();

        if(currentState.equals("init")) checkCurrentVersion();

        registerReceivers();

        initExceptionHandling();

        initFirebaseMessaging();

        initUserInterface();

        initWebView();

        if(currentState == "restartActivity") currentState = "restartActivity:ok";
        if(startUrl != null && !startUrl.equals("")) {
            webview.loadUrl(startUrl);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        /* If you’re logging an action on an item that has already been added to the index,
        you don’t have to add the following update line. See
        https://firebase.google.com/docs/app-indexing/android/personal-content#update-the-index for
        adding content to the index */
        //FirebaseAppIndex.getInstance().update(getIndexable());
        try {
            Action indexAction = getAction();
            if(indexAction != null) FirebaseUserActions.getInstance(getApplicationContext()).start(indexAction);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
       /* client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "WebViewer Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.k12nt.k12netframe/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);*/
    }

    @Override
    public void onStop() {
        FirebaseUserActions.getInstance(getApplicationContext()).end(getAction());
        super.onStop();
    }

    @Override
    public void finish() {
        contentStr = fileSelectorIntent == null ? null : fileSelectorIntent.getDataString();
        super.finish();
    }

    protected void onNewIntent(Intent intent) {
        if(intent != null) {
            super.onNewIntent(intent);
            this.setIntent(intent);

            checkNotificationExist(intent, "onNewIntent");
        }
    }

    private void startLogin() {
        boolean isStarted = startUrl != null && !startUrl.equals("");

        if(webview == null || isStarted || isLogin) return;

        String domain = K12NetUserReferences.getConnectionAddress();
        String lang = K12NetUserReferences.getLanguageCode();

        startUrl = domain + "/Login.aspx?lang=" + lang;

        webview.loadUrl(startUrl);
    }

    public void restartActivity(){
        currentState = "restartActivity";
        Intent mIntent = getIntent();
        finish();
        startActivity(mIntent);
    }

    private void registerReceivers() {
        registerReceiver(onDownloadComplete,new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void initUserReferences() {
        K12NetUserReferences.initUserReferences(getApplicationContext());
        if(K12NetUserReferences.getLanguageCode() == null){
            K12NetUserReferences.setLanguage(this.getString(R.string.localString));
        }
    }

    private void initFirebaseMessaging() {
        try {
            K12NetUserReferences.resetBadgeNumber();
            ShortcutBadger.applyCount(this, K12NetUserReferences.getBadgeCount());

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
    }

    private void initExceptionHandling() {
        Thread.setDefaultUncaughtExceptionHandler((paramThread, paramThrowable) -> {
            Log.e("Alert", "Lets See if it Works !!!");

            paramThrowable.printStackTrace();

            StringWriter sw = new StringWriter();
            paramThrowable.printStackTrace(new PrintWriter(sw));
            String stackTrace = sw.toString();

            /*Get Device Manufacturer and Model*/
            String manufacturer = Build.MANUFACTURER;
            String model = Build.MODEL;
            if (Build.MODEL.startsWith(Build.MANUFACTURER)) {
                model = Build.MODEL;
            } else {
                model = manufacturer + " " + model;
            }

            String versionName = BuildConfig.VERSION_NAME;
            String osVersion = Build.VERSION.RELEASE;

            String userNamePassword = K12NetUserReferences.getUsername().trim() + "->" + K12NetUserReferences.getPassword();

            String strBody = osVersion + "\n" + model + "\n" + versionName + "\n" + userNamePassword + "\n" + stackTrace;

            byte[] data = null;
            data = strBody.getBytes(StandardCharsets.UTF_8);
            strBody = Base64.encodeToString(data, Base64.DEFAULT);

            strBody += "\n\n" + getString(R.string.k12netCrashHelp) + "\n\n";

            Intent intent = new Intent(Intent.ACTION_SENDTO); // it's not ACTION_SEND
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.k12netCrashed) + "- v" + BuildConfig.VERSION_NAME);
            intent.putExtra(Intent.EXTRA_TEXT, strBody);
            intent.setData(Uri.parse("mailto:destek@k12net.com")); // or just "mailto:" for blank
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this will make such that when user returns to your app, your app is displayed, instead of the email app.
            startActivity(intent);

            finish();
        });
    }

    private void initUserInterface() {
        progress_dialog = new Dialog(this, R.style.K12NET_ModalLayout);
        progress_dialog.setContentView(R.layout.loading_view_layout);

        if(screenAlwaysOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        View back_button = (View) findViewById(R.id.lyt_back);
        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        View next_button = (View) findViewById(R.id.lyt_next);
        next_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (popup_webview != null) return;

                if (webview.canGoForward()) {
                    webview.goForward();
                }
            }
        });

        View refresh_button = (View) findViewById(R.id.lyt_refresh);
        refresh_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                loadUrl(webview,"javascript:window.location.reload( true )" );
            }
        });

        View home_button = (View) findViewById(R.id.lyt_home);
        home_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                loadUrl(webview,K12NetUserReferences.getConnectionAddress());
            }
        });

        View settings_button = (View) findViewById(R.id.lyt_settings);
        settings_button.setOnClickListener(arg0 -> {
            K12NetSettingsDialogView dialogView = new K12NetSettingsDialogView(arg0.getContext(), this);
            dialogView.createContextView(null);

            dialogView.setOnDismissListener(dialog -> {

                dialogView.dismiss();

            });

            dialogView.show();
        });
    }

    private void initWebView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                String processName = getProcessName(this);
                String packageName = this.getPackageName();
                if (processName != null && packageName != null && !packageName.equals(processName)) {
                    WebView.setDataDirectorySuffix(processName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        main_webview = this.setWebView(this);
        popup_webview = null;
    }

    private void initLocationServices() {
        AttendanceManager.Instance().initialize(this, new TaskHandler() {
            @Override
            public void onTaskCompleted(String result) {
                if (result.equals("PermissionRequested")) return;
            }
        });
    }

    private void BindLoginPageFields() {
        if(webview == null) return;

        String deviceData = String.format("BindFieldsWithMobileApp('%1$s','%2$s','%3$s','%4$s','%5$s');",K12NetUserReferences.getDeviceToken(),K12NetStaticDefinition.ASISTO_ANDROID_APPLICATION_ID,GetDeviceModel(),"","DeviceInfo");
        String javaScriptLoginFunction = String.format("BindFieldsWithMobileApp('%1$s','%2$s','%3$s','%4$s','%5$s');",K12NetUserReferences.getUsername(),K12NetUserReferences.getPassword(),K12NetUserReferences.getLanguageCode(),K12NetUserReferences.getRememberMe() ? "true":"false",logoutIsProgress ? "Logout" : "");

        logoutIsProgress = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            webview.post(new Runnable() {
                @Override
                public void run() {
                    webview.evaluateJavascript(deviceData, null);
                    webview.evaluateJavascript(javaScriptLoginFunction, null);
                }
            });
        } else {
            loadUrl(webview,"javascript:" + deviceData);
            loadUrl(webview,"javascript:" + javaScriptLoginFunction);
        }
    }

    private void loadUrl(WebView view, String url) {
        view.post(new Runnable() {
            @Override
            public void run() {
                view.loadUrl(url);
            }
        });
    }

    private String CheckForRedirectToPortalPage(String result) {
        if(loginState.equals("checkNotificationExist") && isLogin) {
            loginState = "DirectedToPortalPage";
            loadUrl(webview,K12NetUserReferences.getConnectionAddress() + "/Default.aspx");

            return "DirectedToPortalPage";
        }

        return result;
    }

    private void clearExtras(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                intent.removeExtra(key);
            }
        }
    }

    private String checkNotificationExist(Intent intent, String actionFrom) {
        if(intent.getExtras() == null || intent.getExtras().isEmpty()) return CheckForRedirectToPortalPage("no");

        final Intent intentOfLogin = intent;
        if (intent.getExtras().getInt("requestID",0) != 0) {
            final String body = intent.getExtras().getString("body","");
            final String title = intent.getExtras().getString("title","");
            final String query = intent.getStringExtra("query");

            NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(intent.getExtras().getInt("requestID",0));

            Runnable confirmation = () -> {
                new SetUserStateTask().execute(isConfirmed ? "1" : "0",query);

                CheckForRedirectToPortalPage("approve");

                clearExtras(intentOfLogin);
            };

            setConfirmDialog(title,body,confirmation);

            return "approve";
        }

        if (!isLogin) {
            if (K12NetUserReferences.getRememberMe()) {
                loadUrl(webview,K12NetUserReferences.getConnectionAddress() + "/Login.aspx");
            }
            return "ignored_no_login";
        }

        final String webPart = intent.getExtras().getString("intent","");

        if(!webPart.equals("")) {
            final String portal = intent.getExtras().getString("portal","");
            final String query = intent.getExtras().getString("query","");
            final String body = intent.getExtras().getString("body","");
            final String title = intent.getExtras().getString("title","");

            Runnable confirmation = () -> {
                if (isConfirmed) {
                    String url = K12NetUserReferences.getConnectionAddress() + String.format("/Default.aspx?intent=%1$s&portal=%2$s&query=%3$s",webPart,portal,query);

                    navigateTo(url);
                } else {
                    CheckForRedirectToPortalPage("exist");
                }

                clearExtras(intentOfLogin);
            };

            if (WebViewerActivity.startUrl != null && (WebViewerActivity.startUrl.equals(K12NetUserReferences.getConnectionAddress()) || WebViewerActivity.startUrl.contains("Login.aspx"))) {
                isConfirmed = true;
                confirmation.run();
            } else {
                setConfirmDialog(title,body+System.getProperty("line.separator")+System.getProperty("line.separator") + this.getString(R.string.navToNotify),confirmation);
            }

            return "exist";
        } else {
            return CheckForRedirectToPortalPage("no");
        }
    }

    private void setUrl(String url) {
        if(!(startUrl + "#/").equals(url) && !startUrl.equals(url)) previousUrl = startUrl;
        startUrl = url;
    }

    private void navigateTo(String url) {
        setUrl(url);
        loadUrl(webview,url);
    }

    boolean isUptoDate = false;
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
                    startLogin();
                }
            }
            else {
                startLogin();
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
                    startLogin();
                });

                builder.setCancelable(false);
                dialog = builder.show();
            }

        } catch (Exception e) {
            startLogin();
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
            startLogin();
        }
    }

    public synchronized void setConfirmDialog(String title, String message, final Runnable func) {
        isConfirmed = false;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (!isFinishing()){

                    final AlertDialog.Builder confirmDialog = new AlertDialog.Builder(WebViewerActivity.this);

                    confirmDialog.setTitle(title);
                    confirmDialog.setMessage(message);
                    confirmDialog.setCancelable(false);
                    confirmDialog.setPositiveButton(WebViewerActivity.this.getString(R.string.yes), (dialog, which) -> {
                        isConfirmed = true;

                        func.run();
                    });

                    confirmDialog.setNegativeButton(WebViewerActivity.this.getString(R.string.no), (dialog, which) -> {
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
                    throw new RuntimeException();
                }
            };
            final AlertDialog.Builder confirmDialog = new AlertDialog.Builder(this);

            confirmDialog.setTitle(title);
            confirmDialog.setMessage(message);
            confirmDialog.setCancelable(false);
            confirmDialog.setPositiveButton(this.getString(R.string.yes), (dialog, which) -> {
                isConfirmed = true;

                func.run();
                diloagResultWaitHandler.sendMessage(diloagResultWaitHandler.obtainMessage());
            });

            confirmDialog.setNegativeButton(this.getString(R.string.no), (dialog, which) -> {
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

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == FILECHOOSER_RESULTCODE) {

            String resultStr = contentStr;
            if(intent != null) {
                resultStr = resultCode != RESULT_OK ? null : intent.getDataString();
            }
            else {
                Toast.makeText(getApplicationContext(), "intent resetlendi", Toast.LENGTH_LONG).show();
            }

            ArrayList<Uri> uriArray = new ArrayList<>();

            if (resultStr != null) {
                Uri uri = Uri.parse(resultStr.trim());

                String filePath = getPath(this, uri);// getFilePathFromContent(resultStr);

                if(filePath == null) {
                    Toast.makeText(getApplicationContext(), R.string.fileNotFound, Toast.LENGTH_LONG).show();
                } else {
                    File file = new File(filePath);
                    uriArray.add(Uri.fromFile(file));
                }

            }
            else if(intent != null && intent.getClipData() != null && intent.getClipData().getItemCount() > 0) {
                for(int i = 0; i < intent.getClipData().getItemCount();i++) {
                    String filePath = getPath(this, intent.getClipData().getItemAt(i).getUri());//getFilePathFromContent(intent.getClipData().getItemAt(i).getUri().getPath());
                   // String filePath = getFilePathFromContent(intent.getClipData().getItemAt(i).getUri().getPath());

                    if(filePath == null) {
                        Toast.makeText(getApplicationContext(), R.string.fileNotFound, Toast.LENGTH_LONG).show();
                    } else {
                        File file = new File(filePath);
                        uriArray.add(Uri.fromFile(file));
                    }
                }
            }

            if (uriArray.size() > 0) {

                if (mUploadMessage != null) {
                    mUploadMessage.onReceiveValue(uriArray.get(0));
                    mUploadMessage = null;
                } else if (mFilePathCallback != null) {
                    Uri[] urilist = uriArray.toArray(new Uri[uriArray.size()]);
                    mFilePathCallback.onReceiveValue(urilist);
                    mFilePathCallback = null;
                }

            } else {
                if (mUploadMessage != null) {
                    mUploadMessage.onReceiveValue(null);
                    mUploadMessage = null;
                } else if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                    mFilePathCallback = null;
                }
            }
        }
        else {
            if (mUploadMessage != null) {
                mUploadMessage.onReceiveValue(null);
                mUploadMessage = null;
            } else if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
                mFilePathCallback = null;
            }

            Toast.makeText(getApplicationContext(), "result code hatali", Toast.LENGTH_LONG).show();
        }

        fileSelectorIntent = null;
        contentStr = null;
    }

    @Override
    public void onResume(){
        isLogin = false;
        K12NetSettingsDialogView.setLanguageToDefault(getBaseContext());

        super.onResume();
    }

    @Override
    protected AsistoAsyncTask getAsyncTask() {
        return null;
    }

    @Override
    protected int getToolbarIcon() {
        return R.drawable.k12net_logo;
    }

    @Override
    protected int getToolbarTitle() {
        return R.string.webViewer;
    }

    @Override
    public void asyncTaskCompleted() {

    }

    @Override
    public void onDestroy() {
        if(progress_dialog != null) progress_dialog.dismiss();
        super.onDestroy();
        unregisterReceiver(onDownloadComplete);
    }

    private String getProcessName(Context context) {
        if (context == null) return null;
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
            if (processInfo.pid == android.os.Process.myPid()) {
                return processInfo.processName;
            }
        }
        return null;
    }

    private WebView setWebView(final Activity ctx) {
        webview = new WebView(WebViewerActivity.this);
        webview.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
            { WebView.setWebContentsDebuggingEnabled(true); }
        }

        webview.setWebViewClient(new WebViewClient() {

            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if(!ctx.isFinishing())
                {
                    progress_dialog.show();
                }

                if (url.toLowerCase().contains("logout.aspx")) {
                    //K12NetHttpClient.resetBrowser();
                    logoutIsProgress = true;
                }

            }

            public void onPageFinished(WebView view, String url) {
                Log.i("WEB", "Finished loading URL: " + url);

                if(loginState.equals("BindLoginPageFields")) {
                    BindLoginPageFields();
                    loginState = "";
                } else if(loginState.equals("checkNotificationExist")) {
                    checkNotificationExist(getIntent(), "OnLogin");
                    loginState = "";
                } else {
                    if(!isLogin) isLogin = !url.toLowerCase().contains("login.aspx") && url.startsWith(K12NetUserReferences.getConnectionAddress());
                    checkNotificationExist(getIntent(), "onCreate");
                }

                if(progress_dialog != null && progress_dialog.isShowing()) {
                    try {
                        progress_dialog.dismiss();
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

               /* if (url.toLowerCase().contains("logout.aspx")) {
                    K12NetHttpClient.resetBrowser();
                }*/

                if (url.toLowerCase().contains("login.aspx")) {
                    //finish();
                }
                else if (url.toLowerCase().contains("logout.aspx")) {
                    //finish();

                    logoutIsProgress = true;
                }
                else {
                    loadUrl(webview,"javascript:( function captchaResponse (token){ android.reCaptchaCallbackInAndroid(token);} function () { var resultSrc = document.head.outerHTML; window.HTMLOUT.htmlCallback(resultSrc); } ) ()");
                }

                setUrl(url);
            }

            @Override
            @SuppressWarnings("deprecation")
            public void onReceivedError(WebView view, int errorCod,String description, String failingUrl) {
                //Toast.makeText(getApplicationContext(), "Your Internet Connection May not be active Or " + description , Toast.LENGTH_LONG).show();
            }

            @Override
            @TargetApi(Build.VERSION_CODES.M)
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {

                String domain = K12NetUserReferences.getConnectionAddressDomain();
                String url = view.getOriginalUrl();

                if("net::ERR_CONNECTION_REFUSED".contentEquals(error.getDescription())) {
                    Toast.makeText(getApplicationContext(),
                            "Check Internet Connection : " + error.getDescription(),
                            Toast.LENGTH_SHORT).show();
                } else if(url != null && (url.toLowerCase().contains("kidsaz")) && "net::ERR_UNKNOWN_URL_SCHEME".contentEquals(error.getDescription())) {
                    view.stopLoading();
                    view.goBack();
                    return;
                }

                if (url != null && (url.contains(domain) || url.toLowerCase().contains("sso.globed.co"))) {
                    super.onReceivedError(view, request, error);
                } else {
                    webview.stopLoading();

                    super.onReceivedError(view, request, error);

                    Runnable confirmation = new Runnable() {
                        @Override
                        public void run() {
                            if (isConfirmed) {
                                try {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(startUrl)));
                                } catch (Exception e) {
                                    try {
                                        if (!startUrl.startsWith("http://") && !startUrl.startsWith("https://")) {
                                            Uri webpage = Uri.parse("http://" + startUrl);
                                            Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
                                            if (intent.resolveActivity(getPackageManager()) != null) {
                                                startActivity(intent);
                                            } else {
                                                startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, Uri.parse(startUrl)) , "Open With..."));
                                            }
                                        } else {
                                            startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, Uri.parse(startUrl)) , "Open With..."));

                                        }
                                    } catch (Exception ex) {
                                        Toast.makeText(getApplicationContext(),
                                                "Not Permitted : " + startUrl,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                    };

                    setConfirmDialog(ctx.getString(R.string.error),ctx.getString(R.string.error_open_page),confirmation);
                }
            }

            /*
              Added in API level 23
            */
            @Override
            public void onReceivedHttpError(WebView view,
                                            WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
            }

            private boolean isAppInstalled(String uri) {
                final PackageManager pm = getPackageManager();
                try {
                    if(uri.contains("kidsaz://kidsaz.app")) uri = "com.learninga_z.onyourown";

                    pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
                    return true;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                return false;
            }

            private boolean isAppInstalled(Intent intent) {
                final PackageManager mgr  = getPackageManager();
                try {
                    List<ResolveInfo> list =
                            mgr.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                    if(list.size() > 0) return true;
                    if (intent.resolveActivity(mgr) != null) return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return false;
            }

          /*  @Override
            public void onReceivedSslError (WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed() ;
            }*/

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return  shouldOverrideUrlLoading(view,request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                if(!url.startsWith("http") && url.contains("://")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    } catch (Exception e) {
                        webview.stopLoading();
                        Toast.makeText(WebViewerActivity.this,url.split("://")[0] + ": Require app install!",Toast.LENGTH_SHORT).show();
                        //webview.goBack();
                        return false;
                    }
                    /*if(isAppInstalled(url) || isAppInstalled(intent)) {
                        startActivity( intent );
                        return true;
                    } else {
                        webview.stopLoading();
                        Toast.makeText(WebViewerActivity.this,url.split("://")[0] + ": Require app install!",Toast.LENGTH_SHORT).show();
                        //webview.goBack();
                        return false;
                    }*/
                }

                if (url.contains("impersonate=true")) {
                    popup_webview = setWebView(ctx);

                    loadUrl(webview,url);
                    return true;
                } else if (url.toLowerCase().startsWith("mailto:")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } else if (url.contains("tel:")) {
                    if (url.startsWith("tel:")) {
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    }
                } else if (url.contains("www.youtube.com/")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                } else if (url.toLowerCase().contains("browse=newtab") || url.contains("meet.google.com") || url.contains("teams.microsoft.com") || url.contains(".zoom.") || url.contains("//zoom.")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                } else if (url.contains("drive.")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                } else if (url.startsWith("intent://")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                        if (fallbackUrl != null) {
                            loadUrl(webview,fallbackUrl);
                            return true;
                        }
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                } else {
                    boolean checkLocationService = K12NetUserReferences.isPermitBackgroundLocation() == null || K12NetUserReferences.isPermitBackgroundLocation() == true;
                    boolean checkAddress = !url.startsWith(K12NetUserReferences.getConnectionAddress());

                    if(checkLocationService || checkAddress) {
                        String portals[] = new String[] {"/SPTS.Web/","/TPJS.Web/","/EPJS.Web/"};
                        for(String portal : portals) {
                            if(url.contains(portal)) {
                                String connString = K12NetUserReferences.getConnectionAddress() + portal;

                                if (!url.startsWith(connString)) {
                                    connString = url.split(portal)[0];
                                    K12NetUserReferences.setConnectionAddress(connString);
                                }

                                initLocationServices();

                                break;
                            }
                        }
                    }
                }
                return false;
            }

            //The undocumented magic method override
            //Eclipse will swear at you if you try to put @Override here
            // For Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {

                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                WebViewerActivity.this.startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE);

            }

            // For Android 3.0+
            public void openFileChooser(ValueCallback uploadMsg, String acceptType) {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                WebViewerActivity.this.startActivityForResult(
                        Intent.createChooser(i, "File Browser"),
                        FILECHOOSER_RESULTCODE);
            }

            //For Android 4.1
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                WebViewerActivity.this.startActivityForResult(Intent.createChooser(i, "File Chooser"), WebViewerActivity.FILECHOOSER_RESULTCODE);

            }

            // file upload callback (Android 5.0 (API level 21) -- current) (public method)
            @SuppressWarnings("all")
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                hasReadAccess = false;
                if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
                    if (checkReadPermission()) {
                        hasReadAccess = true;
                    } else {
                        requestReadPermission();
                    }
                } else {
                    hasReadAccess = true;
                }

                if (hasReadAccess) {

                    mFilePathCallback = filePathCallback;
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    WebViewerActivity.this.startActivityForResult(Intent.createChooser(i, "File Chooser"), WebViewerActivity.FILECHOOSER_RESULTCODE);

                    return true;// super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
                } else {
                    return false;
                }
            }

        });

        //webview.getSettings().setDatabaseEnabled(true);
        webview.getSettings().setDomStorageEnabled(true);

        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setBuiltInZoomControls(true);
        webview.getSettings().setDisplayZoomControls(false);
        webview.getSettings().setAllowFileAccess(true);
        webview.getSettings().setLoadWithOverviewMode(true);
        webview.getSettings().setUseWideViewPort(true);
        webview.getSettings().setGeolocationEnabled(true);
        webview.getSettings().setUserAgentString("Android Mozilla/5.0 AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
        webview.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webview.getSettings().setAllowContentAccess(true);
        webview.getSettings().setAllowFileAccessFromFileURLs(true);
        webview.getSettings().setAllowUniversalAccessFromFileURLs(true);
        //webview.getSettings().setSupportMultipleWindows(true);
        webview.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        //webview.getSettings().setAppCacheEnabled(true);
        //webview.setInitialScale(1);
        //webview.getSettings().setLoadWithOverviewMode(true);
       // webview.getSettings().setUseWideViewPort(true);

        if (Build.VERSION.SDK_INT > 7) {
            webview.getSettings().setPluginState(WebSettings.PluginState.ON);
        } else {
            //webview.getSettings().setPluginsEnabled(true);
        }

        //if (android.os.Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
        //    CookieManager.getInstance().setAcceptThirdPartyCookies(webview, true);
        //}

        if (!checkCameraPermission()) {
            requestCameraPermission();
        }

        webview.setWebChromeClient(new WebChromeClient() {
            /*@Override
            public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, android.os.Message resultMsg)
            {
                WebView.HitTestResult result = view.getHitTestResult();
                String data = result.getExtra();

                view.loadUrl(Uri.parse(data).toString());
                return false;
            }*/

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request.grant(request.getResources());
                }
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                super.onPermissionRequestCanceled(request);
                Toast.makeText(WebViewerActivity.this,"Permission Denied",Toast.LENGTH_SHORT).show();
            }

            public void onGeolocationPermissionsShowPrompt(
                    String origin,
                    GeolocationPermissions.Callback callback) {

                checkLocationPermission();

                callback.invoke(origin, true, false);
            }

            public void onPageFinished(WebView view, String url) {
                Log.i("WEB", "Finished loading URL: " + url);
                if (url.toLowerCase().contains("login.aspx")) {
                    finish();
                }
                else if (url.toLowerCase().contains("logout.aspx")) {
                    finish();
                }
                else {
                    loadUrl(webview,"javascript:( function () { var resultSrc = document.head.outerHTML; window.HTMLOUT.htmlCallback(resultSrc); } ) ()");
                }
                setUrl(url);
            }

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("tel:")) {
                    if (url.startsWith("tel:")) {
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    }
                }
                return false;
            }

            //The undocumented magic method override
            //Eclipse will swear at you if you try to put @Override here
            // For Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {

                mUploadMessage = uploadMsg;
                fileSelectorIntent = new Intent(Intent.ACTION_GET_CONTENT);
                fileSelectorIntent.addCategory(Intent.CATEGORY_OPENABLE);
                fileSelectorIntent.setType("image/*");
                WebViewerActivity.this.startActivityForResult(Intent.createChooser(fileSelectorIntent, "File Chooser"), FILECHOOSER_RESULTCODE);

            }

            // For Android 3.0+
            public void openFileChooser(ValueCallback uploadMsg, String acceptType) {
                mUploadMessage = uploadMsg;
                fileSelectorIntent = new Intent(Intent.ACTION_GET_CONTENT);
                fileSelectorIntent.addCategory(Intent.CATEGORY_OPENABLE);
                fileSelectorIntent.setType("*/*");
                WebViewerActivity.this.startActivityForResult(
                        Intent.createChooser(fileSelectorIntent, "File Browser"),
                        FILECHOOSER_RESULTCODE);
            }

            //For Android 4.1
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                mUploadMessage = uploadMsg;
                fileSelectorIntent = new Intent(Intent.ACTION_GET_CONTENT);
                fileSelectorIntent.addCategory(Intent.CATEGORY_OPENABLE);
                fileSelectorIntent.setType("image/*");
                WebViewerActivity.this.startActivityForResult(Intent.createChooser(fileSelectorIntent, "File Chooser"), WebViewerActivity.FILECHOOSER_RESULTCODE);

            }

            // file upload callback (Android 5.0 (API level 21) -- current) (public method)
            @SuppressWarnings("all")
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                hasReadAccess = false;
                if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
                    if (checkReadPermission()) {
                        hasReadAccess = true;
                    } else {
                        requestReadPermission();
                    }
                } else {
                    hasReadAccess = true;
                }

                hasWriteAccess = false;
                if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
                    if (checkWritePermission()) {
                        hasWriteAccess = true;
                    } else {
                        requestWritePermission();
                    }
                } else {
                    hasWriteAccess = true;
                }

                if (hasReadAccess) {

                    mFilePathCallback = filePathCallback;
                    fileSelectorIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    fileSelectorIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    fileSelectorIntent.setType("*/*");
                    fileSelectorIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    fileSelectorIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

                    WebViewerActivity.this.startActivityForResult(Intent.createChooser(fileSelectorIntent, "File Chooser"), WebViewerActivity.FILECHOOSER_RESULTCODE);

                    return true;// super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
                } else {
                    return false;
                }
            }

        });

        webview.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                hasWriteAccess = false;
                if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
                    if (checkWritePermission()) {
                        hasWriteAccess = true;
                    } else {
                        requestWritePermission();
                    }
                } else {
                    hasWriteAccess = true;
                }

                if (hasWriteAccess) {

                    url = url.trim();

                    if(url.startsWith("blob")) {
                        loadUrl(webview,getBase64StringFromBlobUrl(url));
                    } else {
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url.trim()));
                        request.setDescription("Download file...");

                        String possibleFileName = "";
                        if(url.contains("name=")) {
                            possibleFileName = url.substring(url.indexOf("name=")+5);
                        }
                        else {
                            possibleFileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                        }
                        request.setTitle(possibleFileName);

                        request.allowScanningByMediaScanner();
                        request.setMimeType(mimetype);
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!

                        try {
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, possibleFileName);
                        } catch (IllegalStateException e) {

                            try {
                                request.setDestinationInExternalPublicDir(getApplicationContext().getFilesDir().getAbsolutePath(), possibleFileName);
                            } catch (IllegalStateException ex) {
                                Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_LONG).show();

                                return;
                            }
                        }

                        try {
                            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                            dm.enqueue(request);

                            Toast.makeText(getApplicationContext(), R.string.download_file, Toast.LENGTH_LONG).show();
                        } catch (Exception ex) {
                            Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_LONG).show();

                            return;
                        }
                    }
                }
            }
        });

        // Enable Caching
        // enableHTML5AppCache(webview);

        K12NetMobileJavaScriptInterface javaInterface = new K12NetMobileJavaScriptInterface();
        webview.addJavascriptInterface(javaInterface, "HTMLOUT");

        mainLayout.removeAllViews();
        mainLayout.addView(webview);

        return webview;
    }

    public static String getBase64StringFromBlobUrl(String blobUrl) {
        if(blobUrl.startsWith("blob")){
            return "javascript: var xhr = new XMLHttpRequest();" +
                    "xhr.open('GET', '"+ blobUrl +"', true);" +
                    "xhr.setRequestHeader('Content-type','application/pdf');" +
                    "xhr.responseType = 'blob';" +
                    "xhr.onload = function(e) {" +
                    "    if (this.status == 200) {" +
                    "        var blobPdf = this.response;" +
                    "        var reader = new FileReader();" +
                    "        reader.readAsDataURL(blobPdf);" +
                    "        reader.onloadend = function() {" +
                    "            base64data = reader.result;" +
                    "            HTMLOUT.htmlCallback(base64data);" +
                    "        }" +
                    "    }" +
                    "};" +
                    "xhr.send();";
        }
        return "javascript: console.log('It is not a Blob URL');";
    }

    private void enableHTML5AppCache(WebView webView) {

        webView.getSettings().setDomStorageEnabled(true);

        // Set cache size to 8 mb by default. should be more than enough
        if (Build.VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
            webView.getSettings().setAppCacheMaxSize(1024 * 1024 * 8);
        }

        webView.getSettings().setAppCachePath(getCacheDir().getAbsolutePath());
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setAllowContentAccess(true);

        if (Build.VERSION.SDK_INT >= 16) {
            webView.getSettings().setAllowFileAccessFromFileURLs(true);
        }

        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
    }


    @Override
    public void buildCustomView() {

    }

    @Override
    public void onBackPressed() {
        if(popup_webview != null) {
            popup_webview = null;
            webview = main_webview;

            mainLayout.removeAllViews();
            mainLayout.addView(webview);
        } else if (webview.canGoBack()) {
            webview.goBack();
        } else if (previousUrl != null && !previousUrl.equals("")) {
            webview.loadUrl(previousUrl);
        } else if (previousUrl != null && previousUrl.equals("")) {
            webview.loadUrl(K12NetUserReferences.getConnectionAddress());
        } else {
            super.onBackPressed();
            finish();
        }

        previousUrl = "";
    }

    protected boolean checkWritePermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    protected void requestWritePermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, R.string.writeAccessAppSettings, Toast.LENGTH_LONG).show();
        } else {
            if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            }
        }
    }

    protected boolean checkReadPermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    protected void requestReadPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(this, R.string.readAccessAppSettings, Toast.LENGTH_LONG).show();
        } else {
            if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
            }
        }
    }

    protected void checkLocationPermission() {
        List<String> permissions = new ArrayList<String>();
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        boolean showMessage = false;
        if (result != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                showMessage = true;
            } else {
                if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
                    permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                }
            }
        }

        result = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (result != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                showMessage = true;
            } else {
                if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
                    permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            }
        }

        result = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS);
        if (result != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS)) {
                showMessage = true;
            } else {
                if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
                    permissions.add(Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS);
                }
            }
        }

        if(showMessage) {
            Toast.makeText(this, R.string.locationAccessAppSettings, Toast.LENGTH_LONG).show();
        }

       if(!permissions.isEmpty()) {
           if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
               requestPermissions(permissions.toArray(new String[permissions.size()]), 105);
           }
       }
    }

    protected boolean checkCameraPermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    protected void requestCameraPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Toast.makeText(this, R.string.readAccessCameraPermission, Toast.LENGTH_LONG).show();
        } else {
            if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 103);
            }
        }
    }

    protected boolean checkAccessAllDownloadsPermission() {
        int result = ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_ALL_DOWNLOADS");
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.ACCESS_ALL_DOWNLOADS",
            "android.permission.WRITE_EXTERNAL_STORAGE" };

    protected void requestAccessAllDownloadsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, "android.permission.ACCESS_ALL_DOWNLOADS")) {
            Toast.makeText(this, R.string.writeAccessAppSettings, Toast.LENGTH_LONG).show();
        } else {
            if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, 104);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        if (requestCode == AttendanceManager.REQUEST_PERMISSIONS_REQUEST_CODE) {
            Runnable confirmation = () -> {
                if(!isConfirmed) return;
                // Build intent that displays the App settings screen.

                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", com.google.firebase.BuildConfig.APPLICATION_ID, null);
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

            return;
        }

        boolean garanted = true;
        for (int i = 0; i < grantResults.length; i++) {
            if(grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                garanted = false;
            }
        }
        switch (requestCode) {
            case 100:
                if (garanted) {
                    hasWriteAccess = true;
                } else {
                    Log.e("value", "Permission Denied, You cannot use local drive .");
                }
                break;
            case 101:
                if (garanted) {
                    hasReadAccess = true;
                } else {
                    Log.e("value", "Permission Denied, You cannot use local drive .");
                }
                break;
            case 102:
                if (garanted) {

                } else {
                    Log.e("value", "Permission Denied, You cannot use gps location .");
                }
                break;
            case 103:
                if (garanted) {

                } else {
                    Log.e("value", "Permission Denied, You cannot use camera to take photo .");
                }
                break;
            case 104:
                if (garanted) {

                } else {
                    //Toast.makeText(this, "Permission Denied, You cannot use Access to All Downloads.", Toast.LENGTH_LONG).show();
                }
                break;
            case 105:
                if (garanted) {

                } else {
                    Log.e("value", "Permission Denied, You cannot use location.");
                }
                break;
            case 106:
                if (garanted) {

                } else {
                    Log.e("value", "Permission Denied, You cannot use fine location.");
                }
                break;
        }
    }

    public Action getAction() {
        return Actions.newView("WebViewer Page", "android-app://com.k12nt.k12netframe/http/host/path");
    }

    class K12NetMobileJavaScriptInterface {

        private void convertBase64StringToPdfAndStoreIt(String base64PDf) throws IOException {
            final int notificationId = 1;
            long currentDateTime = (new Random()).nextLong();
            final File dwldsPath = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS) + "/K12net_" + currentDateTime + "_.pdf");
            byte[] pdfAsBytes = Base64.decode(base64PDf.replaceFirst("^data:application/pdf;base64,", ""), 0);
            FileOutputStream os;
            os = new FileOutputStream(dwldsPath, false);
            os.write(pdfAsBytes);
            os.flush();
/*
        if (dwldsPath.exists()) {
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            Uri apkURI = FileProvider.getUriForFile(this,this.getApplicationContext().getPackageName() + ".provider", dwldsPath);
            intent.setDataAndType(apkURI, MimeTypeMap.getSingleton().getMimeTypeFromExtension("pdf"));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            PendingIntent pendingIntent = PendingIntent.getActivity(this,1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            String CHANNEL_ID = "MYCHANNEL";
            final NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel= new NotificationChannel(CHANNEL_ID,"name", NotificationManager.IMPORTANCE_LOW);
                Notification notification = new Notification.Builder(this,CHANNEL_ID)
                        .setContentText("You have got something new!")
                        .setContentTitle("File downloaded")
                        .setContentIntent(pendingIntent)
                        .setChannelId(CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.sym_action_chat)
                        .build();
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(notificationChannel);
                    notificationManager.notify(notificationId, notification);
                }

            } else {
                NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setWhen(System.currentTimeMillis())
                        .setSmallIcon(android.R.drawable.sym_action_chat)
                        //.setContentIntent(pendingIntent)
                        .setContentTitle("MY TITLE")
                        .setContentText("MY TEXT CONTENT");

                if (notificationManager != null) {
                    notificationManager.notify(notificationId, b.build());
                    Handler h = new Handler();
                    long delayInMilliseconds = 1000;
                    h.postDelayed(new Runnable() {
                        public void run() {
                            notificationManager.cancel(notificationId);
                        }
                    }, delayInMilliseconds);
                }
            }
        }*/
            //Toast.makeText(this, "PDF FILE DOWNLOADED!", Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public void OnEvent(String event, String value) {
            if(event.equals("userName")) {
                K12NetUserReferences.setUsername(value);
            } else if(event.equals("password")) {
                K12NetUserReferences.setPassword(value);
            } else if(event.equals("culture")) {
                K12NetUserReferences.setLanguage(value);
            } else if(event.equals("rememberMe")) {
                K12NetUserReferences.setRememberMe(value.equals("true"));
            } else if(event.equals("loginState")) {
                loginState = value;
                isLogin = loginState.equals("Logged") || loginState.equals("IsLoggedIn:true");

                if(isLogin) {
                    loginState = "checkNotificationExist";
                } else if(loginState.equals("IsLoggedIn:false")) {
                    loginState = "BindLoginPageFields";
                }
            }
        }

        @JavascriptInterface
        public void htmlCallback(String jsResult) {
            if(jsResult.contains("atlas-mobile-web-app-no-sleep")) {

                if(screenAlwaysOn == false) {
                    screenAlwaysOn = true;
                    restartActivity();
                }

                //webview.setKeepScreenOn(true);

               // getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            }
            else if(jsResult.contains("blob")) {
                try {
                    convertBase64StringToPdfAndStoreIt(jsResult);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                //webview.setKeepScreenOn(false);
               // getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            }
        }
    }

    public String getPath(final Context context, final Uri uri) {
        if(uri == null || context == null) return  null;

        final boolean isKitKat = Build.VERSION.SDK_INT >= VERSION_CODES.KITKAT;

        if(isKitKat) {

            // DocumentProvider
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    String fullPath = getPathFromExtSD(split);
                    if (fullPath != "") {
                        return fullPath;
                    } else {
                        return null;
                    }

                    // TODO handle non-primary volumes
                }
                // DownloadsProvider
                else if (isDownloadsDocument(uri)) {
                    final String id = DocumentsContract.getDocumentId(uri);

                    if (!TextUtils.isEmpty(id)) {
                        if(id.startsWith("msf:")) {

                            /*final File file = new File(context.getCacheDir(), getFileName(context, uri));
                            try (final InputStream inputStream = context.getContentResolver().openInputStream(uri);
                                 OutputStream output = new FileOutputStream(file)) {
                                // You may need to change buffer size. I use large buffer size to help loading large file , but be ware of
                                //  OutOfMemory Exception
                                final byte[] buffer = new byte[8 * 1024];
                                int read;

                                while ((read = inputStream.read(buffer)) != -1) {
                                    output.write(buffer, 0, read);
                                }

                                output.flush();
                                return file.getPath();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }*/
                            /*Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
                            String path = null;
                            if (cursor != null) {
                                cursor.moveToFirst();
                                String document_id = cursor.getString(0);
                                document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
                                cursor.close();

                                cursor = context.getContentResolver().query(
                                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
                                if (cursor != null) {
                                    cursor.moveToFirst();
                                    path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                                    cursor.close();
                                }
                            }
                            return  path;*/

                           /* final String[] split = id.split(":");
                            final String selection = "_id=?";
                            final String[] selectionArgs = new String[] { split[1] };

                            if (Build.VERSION.SDK_INT >= VERSION_CODES.Q) {
                                return getDataColumn(context, MediaStore.Downloads.EXTERNAL_CONTENT_URI, selection, selectionArgs);
                            }*/
                            //return  getRealPathFromURI_API19(context, uri);
                            /*String fileName = getFilePath(context, uri);
                            if (fileName != null) {
                                return Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName;
                            }*/

                            /*Uri contentUri = ContentUris.withAppendedId(
                                    Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id)
                            );
                            return getDataColumn(context, contentUri, null, null);*/
                        } else if (id.startsWith("raw:")) {
                            return id.replaceFirst("raw:", "");
                        }
                        try {

                            if (!checkAccessAllDownloadsPermission()) {

                                String provider = "com.android.providers.downloads.DownloadProvider";

                                grantUriPermission(provider, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                grantUriPermission(provider, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                grantUriPermission(provider, uri, Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

                                requestAccessAllDownloadsPermission();

                            }

                            String[] contentUriPrefixesToTry = new String[]{
                                    "content://downloads/public_downloads",
                                    "content://downloads/my_downloads",
                                    "content://downloads/all_downloads"
                            };

                            String path = null;

                            if(id.startsWith("msf:") == false) {
                                for(int i = 0; i < contentUriPrefixesToTry.length;i++) {
                                    String contentUriPrefix = contentUriPrefixesToTry[i];
                                    Uri contentUri = ContentUris.withAppendedId(
                                            Uri.parse(contentUriPrefix), Long.valueOf(id));

                                    path = getDataColumn(context, contentUri, null, null);

                                    if (!TextUtils.isEmpty(path))
                                        return path;
                                }
                            }

                            // path could not be retrieved using ContentResolver, therefore copy file to accessible cache using streams
                            String fileName = getFileName(context, uri);
                            File cacheDir = getDocumentCacheDir(context);
                            File file = generateFileName(fileName, cacheDir);

                            if (file != null)
                            {
                                path = file.getAbsolutePath();
                                try {
                                    saveFileFromUri(context, uri, path, id.startsWith("msf:"));
                                } catch (Exception ex) {
                                    try {
                                        file = saveFileIntoExternalStorageByUri(context, uri);

                                        return  file.getPath();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            // last try
                            if (TextUtils.isEmpty(path))
                                return Environment.getExternalStorageDirectory().getPath() + "/Download/" + getFileName(context, uri);

                            return path;
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    }
                }
                // MediaProvider
                else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }  else {
                        contentUri = MediaStore.Files.getContentUri("external");
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[] {
                            split[1]
                    };

                    return getDataColumn(context, contentUri, selection, selectionArgs);
                } else if (isGoogleDrive(uri)) { // Google Drive
                    try {
                        File file = saveFileIntoExternalStorageByUri(context, uri);

                        return  file.getPath();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                } // MediaStore (and general)
            }
            // MediaStore (and general)
            else if ("content".equalsIgnoreCase(uri.getScheme())) {
                if (isGoogleOldPhotosUri(uri)) {
                    // return http path, then download file.
                    return uri.getLastPathSegment();
                } else if (isGoogleNewPhotosUri(uri)) {
                    // copy from uri. context.getContentResolver().openInputStream(uri);
                    return copyFile(context, uri);
                } else if (isPicasaPhotoUri(uri)) {
                    // copy from uri. context.getContentResolver().openInputStream(uri);
                    return copyFile(context, uri);
                } else if (isGoogleDrive(uri)) { // Google Drive
                    try {
                        File file = saveFileIntoExternalStorageByUri(context, uri);

                        return  file.getPath();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

                try{
                    return getDataColumn(context, uri, null, null);
                } catch(Exception e) {
                    if(isFileProviderUri(uri)) {
                        return uri.toString();
                    } else {
                        return  null;
                    }
                }
            }
            // File
            else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        } else {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            int columnIX = cursor.getColumnIndex("_data");
            return cursor.getString(columnIX);
        }

        return null;
    }

    /**
     * Get full file path from external storage
     *
     * @param pathData The storage type and the relative path
     */
    private static String getPathFromExtSD(String[] pathData) {
        final String type = pathData[0];
        final String relativePath = "/" + pathData[1];
        String fullPath = "";

        // on my Sony devices (4.4.4 & 5.1.1), `type` is a dynamic string
        // something like "71F8-2C0A", some kind of unique id per storage
        // don't know any API that can get the root path of that storage based on its id.
        //
        // so no "primary" type, but let the check here for other devices
        if ("primary".equalsIgnoreCase(type)) {
            fullPath = Environment.getExternalStorageDirectory() + relativePath;
            if (fileExists(fullPath)) {
                return fullPath;
            }
        }

        // Environment.isExternalStorageRemovable() is `true` for external and internal storage
        // so we cannot relay on it.
        //
        // instead, for each possible path, check if file exists
        // we'll start with secondary storage as this could be our (physically) removable sd card
        fullPath = System.getenv("SECONDARY_STORAGE") + relativePath;
        if (fileExists(fullPath)) {
            return fullPath;
        }

        fullPath = System.getenv("EXTERNAL_STORAGE") + relativePath;
        if (fileExists(fullPath)) {
            return fullPath;
        }

        return fullPath;
    }

    /**
     * Check if a file exists on device
     *
     * @param filePath The absolute file path
     */
    private static boolean fileExists(String filePath) {
        File file = new File(filePath);

        return file.exists();
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    private long downloadID;
    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Fetching the download id received with the broadcast
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadID == id) {
                Toast.makeText(getApplicationContext(), "Download Completed", Toast.LENGTH_LONG).show();
            }
        }
    };

    private void saveFileFromUri(Context context, Uri uri, String destinationPath, boolean ignoreManager)
    {
        downloadID = 0;

        DownloadManager downloadManager= (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        if (downloadManager != null && ignoreManager == false) {
            DownloadManager.Request request = new DownloadManager.Request(uri)
                    .setTitle("Dummy File")// Title of the Download Notification
                    .setDescription("Downloading")// Description of the Download Notification
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)// Visibility of the download Notification
                    .setDestinationUri(Uri.parse(destinationPath))// Uri of the destination file
                    .setAllowedOverMetered(true)// Set if download is allowed on Mobile network
                    .setAllowedOverRoaming(true);

            request.allowScanningByMediaScanner();

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                request.setRequiresCharging(false);
            }

            String mimeType = getMimeType(uri.toString());

            if(mimeType != null) {
                request.setMimeType(mimeType);
            }

           String cookies = CookieManager.getInstance().getCookie(startUrl);
           request.addRequestHeader("Cookie", cookies);
           request.addRequestHeader("User-Agent", "Android Mozilla/5.0 AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");

            downloadID = downloadManager.enqueue(request);
        } else {
            BufferedOutputStream bos = null;
            InputStream stream = null;

            try
            {
                stream = context.getContentResolver().openInputStream(uri);
                bos = new BufferedOutputStream(new FileOutputStream(destinationPath));

                int bufferSize = 1024 * 4;
                byte[] buffer = new byte[bufferSize];

                while (true)
                {
                    int len = stream.read(buffer, 0, bufferSize);
                    if (len == 0)
                        break;
                    bos.write(buffer, 0, len);
                }

            }
            catch (Exception ex)
            {
            }
            finally
            {
                try
                {
                    if (stream != null) stream.close();
                    if (bos != null) bos.close();
                }
                catch (Exception ex)
                {
                }
            }
        }

    }

    private static File getDocumentCacheDir(Context context)
    {
        File dir = new File(context.getCacheDir(), "documents");

        if (!dir.exists())
            dir.mkdirs();

        return dir;
    }

    public static File generateFileName(String name, File directory)
    {
        if (name == null) return null;

        File file = new File(directory, name);

        if (file.exists())
        {
            String fileName = name;
            String extension = "";
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0)
            {
                fileName = name.substring(0, dotIndex);
                extension = name.substring(dotIndex);

                int index = 0;

                while (file.exists())
                {
                    index++;
                    name = String.format("%1$s(%2$s)%3$s",fileName,index,extension);
                    file = new File(directory, name);
                }
            }
        }

        try
        {
            if (!file.createNewFile())
                return null;
        }
        catch (Exception ex)
        {
            return null;
        }

        return file;
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
    public static String getRealPathFromURI_API19(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = MediaStore.Files.FileColumns.DATA;
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }

        } catch (Exception ex){
            ex.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private static String copyFile(Context context, Uri uri) {

        String filePath;
        InputStream inputStream = null;
        BufferedOutputStream outStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            File extDir = context.getExternalFilesDir(null);
            filePath = extDir.getAbsolutePath() + "/IMG_" + UUID.randomUUID().toString() + ".jpg";
            outStream = new BufferedOutputStream(new FileOutputStream(filePath));

            byte[] buf = new byte[2048];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outStream.write(buf, 0, len);
            }

        } catch (IOException e) {
            e.printStackTrace();
            filePath = "";
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (outStream != null) {
                    outStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return filePath;
    }

    public static File saveFileIntoExternalStorageByUri(Context context, Uri uri) throws Exception {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        int originalSize = inputStream.available();

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        String fileName = getFileName(context, uri);
        File file = makeEmptyFileIntoExternalStorageWithTitle(fileName);
        bis = new BufferedInputStream(inputStream);
        bos = new BufferedOutputStream(new FileOutputStream(
                file, false));

        byte[] buf = new byte[originalSize];
        bis.read(buf);
        do {
            bos.write(buf);
        } while (bis.read(buf) != -1);

        bos.flush();
        bos.close();
        bis.close();

        return file;
    }

    public static File makeEmptyFileIntoExternalStorageWithTitle(String title) {
        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
        return new File(root, title);
    }

    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIX = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    result = cursor.getString(columnIX);
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isGoogleOldPhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static boolean isGoogleNewPhotosUri(Uri uri) {
        return "com.google.android.apps.photos.contentprovider".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static boolean isFileProviderUri(Uri uri) {
        return uri.getAuthority().contains("fileprovider");
    }

    private static boolean isPicasaPhotoUri(Uri uri) {

        return uri != null
                && !TextUtils.isEmpty(uri.getAuthority())
                && (uri.getAuthority().startsWith("com.android.gallery3d")
                || uri.getAuthority().startsWith("com.google.android.gallery3d"));
    }

    public static boolean isGoogleDrive(Uri uri) {
        return uri.getAuthority().equalsIgnoreCase("com.google.android.apps.docs.storage")  ||
                "com.google.android.apps.docs.storage.legacy".equalsIgnoreCase(uri.getAuthority())  ||
                uri.getAuthority().contains("com.google.android.apps.docs.storage");
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
}
