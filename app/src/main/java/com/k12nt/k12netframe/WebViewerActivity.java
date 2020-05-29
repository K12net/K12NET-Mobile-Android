package com.k12nt.k12netframe;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
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

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.builders.Actions;
import com.k12nt.k12netframe.async_tasks.AsistoAsyncTask;
import com.k12nt.k12netframe.async_tasks.K12NetAsyncCompleteListener;
import com.k12nt.k12netframe.utils.definition.K12NetStaticDefinition;
import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;
import com.k12nt.k12netframe.utils.webConnection.K12NetHttpClient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import me.leolin.shortcutbadger.ShortcutBadger;

public class WebViewerActivity extends K12NetActivity implements K12NetAsyncCompleteListener {

    public static String startUrl = "";
    public static String previousUrl = "";
    WebView webview = null;
    WebView main_webview = null;
    WebView popup_webview = null;

    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;
    private final static int FILECHOOSER_RESULTCODE = 1;
    private Boolean isConfirmed = false;
    private Dialog progress_dialog;

    private static final String TAG = "WebViewerActivity";
    boolean hasWriteAccess = false;
    boolean hasReadAccess = false;
    static boolean screenAlwaysOn = false;

    private Intent fileSelectorIntent = null;
    private String contentStr = null;

    protected void onNewIntent(Intent intent) {

        if(intent != null && intent.getExtras() != null) {
            super.onNewIntent(intent);
            this.setIntent(intent);

            final String uri = intent.getExtras().getString("intent","");

            if(uri != "") {
                final String portal = intent.getExtras().getString("portal","");
                final String query = intent.getExtras().getString("query","");
                final String webPart = intent.getExtras().getString("intent","");
                final String body = intent.getExtras().getString("body","");
                final String title = intent.getExtras().getString("title","");

                final Intent intentOfLogin = this.getIntent();

                Runnable confirmation = new Runnable() {
                    @Override
                    public void run() {
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
                    }
                };

                if (WebViewerActivity.startUrl == K12NetUserReferences.getConnectionAddress() || WebViewerActivity.startUrl.contains("Login.aspx")) {
                    isConfirmed = true;
                    confirmation.run();
                } else {
                    setConfirmDialog(title,body+System.getProperty("line.separator")+System.getProperty("line.separator") + this.getString(R.string.navToNotify),confirmation);
                }
            }
        }
    }

    private void navigateTo(String url) {
        WebViewerActivity.startUrl = url;

        if (LoginActivity.isLogin) {
            Intent intent = new Intent(this, WebViewerActivity.class);
            this.startActivity(intent);
        } else {
            Intent intent = new Intent(this, LoginActivity.class);

            final String portal = this.getIntent().getExtras().getString("portal","");
            final String query = this.getIntent().getExtras().getString("query","");
            final String webPart = this.getIntent().getExtras().getString("intent","");

            intent.putExtra("intent",webPart);
            intent.putExtra("portal", portal);
            intent.putExtra("query", query);

            this.startActivity(intent);
        }
    }

    private synchronized void setConfirmDialog(String title, String message, final Runnable func) {
        isConfirmed = false;

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setPositiveButton(this.getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isConfirmed = true;

                func.run();
            }
        });

        builder.setNegativeButton(this.getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isConfirmed = false;

                func.run();
            }
        });

        try {
            builder.show();
        } catch (Exception e) {

        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == FILECHOOSER_RESULTCODE) {

            String resultStr = contentStr;
            if(intent != null) {
                resultStr = intent == null || resultCode != RESULT_OK ? null
                        : intent.getDataString();
            }
            else {
                Toast.makeText(getApplicationContext(), "intent resetlendi", Toast.LENGTH_LONG).show();
            }

            ArrayList<Uri> uriArray = new ArrayList<>();

            if (resultStr != null) {
                Uri uri = Uri.parse(resultStr);

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
        super.onDestroy();
        unregisterReceiver(onDownloadComplete);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        progress_dialog = new Dialog(this, R.style.K12NET_ModalLayout);
        progress_dialog.setContentView(R.layout.loading_view_layout);

        if(screenAlwaysOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        registerReceiver(onDownloadComplete,new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread paramThread, final Throwable paramThrowable) {
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


                String userNamePassword = K12NetUserReferences.getUsername() + "->" + K12NetUserReferences.getPassword();

                String strBody = osVersion + "\n" + model + "\n" + versionName + "\n" + userNamePassword + "\n" + stackTrace;

                byte[] data = null;
                try {
                    data = strBody.getBytes("UTF-8");
                    strBody = Base64.encodeToString(data, Base64.DEFAULT);
                } catch (UnsupportedEncodingException e1) {

                }

                strBody += "\n\n" + getString(R.string.k12netCrashHelp) + "\n\n";

                Intent intent = new Intent(Intent.ACTION_SENDTO); // it's not ACTION_SEND
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.k12netCrashed) + "- v" + BuildConfig.VERSION_NAME);
                intent.putExtra(Intent.EXTRA_TEXT, strBody);
                intent.setData(Uri.parse("mailto:destek@k12net.com")); // or just "mailto:" for blank
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this will make such that when user returns to your app, your app is displayed, instead of the email app.
                startActivity(intent);

                finish();
            }
        });

        K12NetUserReferences.initUserReferences(getApplicationContext());
        K12NetUserReferences.resetBadgeNumber();
        ShortcutBadger.applyCount(this, K12NetUserReferences.getBadgeCount());

        List<HttpCookie> cookies = K12NetHttpClient.getCookieList();
        HttpCookie sessionInfo = null;

        if (cookies != null && !cookies.isEmpty()) {
            CookieManager cookieManager = CookieManager.getInstance();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                CookieSyncManager.createInstance(getApplicationContext());
            }
            cookieManager.setAcceptCookie(true);

            for (HttpCookie cookie : cookies) {
                sessionInfo = cookie;
                String cookieString = sessionInfo.getName() + "=" + sessionInfo.getValue() + "; domain=" + sessionInfo.getDomain();
                cookieManager.setCookie(K12NetUserReferences.getConnectionAddress(), cookieString);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    CookieSyncManager.getInstance().sync();
                }
            }

            String cookieString = "UICulture" + "=" + K12NetUserReferences.getNormalizedLanguageCode() + "; domain=" + sessionInfo.getDomain();
            cookieManager.setCookie(K12NetUserReferences.getConnectionAddress(), cookieString);

            cookieString = "Culture" + "=" + K12NetUserReferences.getNormalizedLanguageCode() + "; domain=" + sessionInfo.getDomain();
            cookieManager.setCookie(K12NetUserReferences.getConnectionAddress(), cookieString);

            cookieString = "AppID" + "=" + K12NetStaticDefinition.ASISTO_ANDROID_APPLICATION_ID + "; domain=" + sessionInfo.getDomain();
            cookieManager.setCookie(K12NetUserReferences.getConnectionAddress(), cookieString);
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

                webview.reload();
            }
        });

        View home_button = (View) findViewById(R.id.lyt_home);
        home_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                webview.loadUrl(K12NetUserReferences.getConnectionAddress());
            }
        });

        main_webview = this.setWebView(this);
        popup_webview = null;
        webview.loadUrl(startUrl);

        this.onNewIntent(this.getIntent());
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

            }

            public void onPageFinished(WebView view, String url) {
                Log.i("WEB", "Finished loading URL: " + url);

                if(progress_dialog != null && progress_dialog.isShowing()) {
                    try {
                        progress_dialog.dismiss();
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                if (url.toLowerCase().contains("login.aspx")) {
                    finish();
                }
                else if (url.toLowerCase().contains("logout.aspx")) {
                    finish();
                }
                else {
                    webview.loadUrl("javascript:( function () { var resultSrc = document.head.outerHTML; window.HTMLOUT.htmlCallback(resultSrc); } ) ()");
                }

                startUrl = url;
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

                if(error.getDescription() == "net::ERR_CONNECTION_REFUSED") {
                    Toast.makeText(getApplicationContext(),
                            "Check Internet Connection : " + error.getDescription(),
                            Toast.LENGTH_SHORT).show();
                }

                String domain = K12NetUserReferences.getConnectionAddressDomain();
                String url = view.getOriginalUrl();

                if (url != null && url.contains(domain)) {
                    super.onReceivedError(view, request, error);
                } else {
                    webview.stopLoading();

                    super.onReceivedError(view, request, error);

                    Runnable confirmation = new Runnable() {
                        @Override
                        public void run() {
                            if (isConfirmed) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(startUrl)));
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

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("impersonate=true")) {
                    popup_webview = setWebView(ctx);

                    webview.loadUrl(url);
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
                } else if (url.contains("meet.google.com") || url.contains("teams.microsoft.com") || url.contains(".zoom.") || url.contains("//zoom.")) {
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
                            webview.loadUrl(fallbackUrl);
                            return true;
                        }
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
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

        //webview.getSettings().setDomStorageEnabled(true);

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

                if (checkGPSPermission() == false) {
                    requestGPSPermission();
                }


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
                    webview.loadUrl("javascript:( function () { var resultSrc = document.head.outerHTML; window.HTMLOUT.htmlCallback(resultSrc); } ) ()");
                }
                startUrl = url;
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

                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
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
        });

        // Enable Caching
        // enableHTML5AppCache(webview);

        K12NetMobileJavaScriptInterface javaInterface = new K12NetMobileJavaScriptInterface();
        webview.addJavascriptInterface(javaInterface, "HTMLOUT");

        mainLayout.removeAllViews();
        mainLayout.addView(webview);

        return webview;
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
        } else if (previousUrl != null) {
            webview.loadUrl(previousUrl);
        } else if (webview.canGoBack()) {
            webview.goBack();
        } else {
            super.onBackPressed();
            finish();
        }

        previousUrl = null;
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

    protected boolean checkGPSPermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    protected void requestGPSPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS)) {
            Toast.makeText(this, R.string.locationAccessAppSettings, Toast.LENGTH_LONG).show();
        } else {
            if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS}, 102);
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
        switch (requestCode) {
            case 100:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasWriteAccess = true;
                } else {
                    Log.e("value", "Permission Denied, You cannot use local drive .");
                }
                break;
            case 101:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasReadAccess = true;
                } else {
                    Log.e("value", "Permission Denied, You cannot use local drive .");
                }
                break;
            case 102:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Log.e("value", "Permission Denied, You cannot use gps location .");
                }
                break;
            case 103:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Log.e("value", "Permission Denied, You cannot use camera to take photo .");
                }
                break;
            case 104:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(this, "Permission Denied, You cannot use Access to All Downloads.", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    public Action getAction() {
        return Actions.newView("WebViewer Page", "android-app://com.k12nt.k12netframe/http/host/path");
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
            if(indexAction != null) FirebaseUserActions.getInstance().start(indexAction);
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
        FirebaseUserActions.getInstance().end(getAction());
        super.onStop();
    }

    @Override
    public void finish() {

        contentStr = fileSelectorIntent == null ? null : fileSelectorIntent.getDataString();

        super.finish();

    }

    public void restartActivity(){
        Intent mIntent = getIntent();
        finish();
        startActivity(mIntent);
    }

    class K12NetMobileJavaScriptInterface {
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
            else {

                screenAlwaysOn = false;
                //webview.setKeepScreenOn(false);
               // getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            }
        }
    }

    public String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

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
                        if (id.startsWith("raw:")) {
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

                            for(int i = 0; i < contentUriPrefixesToTry.length;i++) {
                                String contentUriPrefix = contentUriPrefixesToTry[i];
                                Uri contentUri = ContentUris.withAppendedId(
                                        Uri.parse(contentUriPrefix), Long.valueOf(id));

                                path = getDataColumn(context, contentUri, null, null);

                                if (!TextUtils.isEmpty(path))
                                    return path;
                            }

                            // path could not be retrieved using ContentResolver, therefore copy file to accessible cache using streams
                            String fileName = getFileName(context, uri);
                            File cacheDir = getDocumentCacheDir(context);
                            File file = generateFileName(fileName, cacheDir);

                            if (file != null)
                            {
                                path = file.getAbsolutePath();
                                try {
                                    saveFileFromUri(context, uri, path);
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
            return cursor.getString(cursor.getColumnIndex("_data"));
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

    private void saveFileFromUri(Context context, Uri uri, String destinationPath)
    {
        downloadID = 0;

        DownloadManager downloadManager= (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        if (downloadManager != null) {
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
        final String column = "_data";
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
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
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
}
