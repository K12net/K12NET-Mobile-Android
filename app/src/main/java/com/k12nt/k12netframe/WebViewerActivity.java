package com.k12nt.k12netframe;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
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
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.k12nt.k12netframe.async_tasks.AsistoAsyncTask;
import com.k12nt.k12netframe.async_tasks.K12NetAsyncCompleteListener;
import com.k12nt.k12netframe.async_tasks.LoginAsyncTask;
import com.k12nt.k12netframe.utils.definition.K12NetStaticDefinition;
import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;
import com.k12nt.k12netframe.utils.webConnection.K12NetHttpClient;

import org.apache.http.cookie.Cookie;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import me.leolin.shortcutbadger.ShortcutBadger;

public class WebViewerActivity extends K12NetActivity implements K12NetAsyncCompleteListener {

    public static String startUrl = "";
    public static String previousUrl = "";
    public static Context ctx = null;
    WebView webview = null;

    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;
    private final static int FILECHOOSER_RESULTCODE = 1;
    private Boolean isConfirmed = false;

    boolean hasWriteAccess = false;
    boolean hasReadAccess = false;
    static boolean screenAlwaysOn = false;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

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
                final Intent intentOfLogin = this.getIntent();

                Runnable confirmComplated = new Runnable() {
                    @Override
                    public void run() {
                        String url = K12NetUserReferences.getConnectionAddress();

                        intentOfLogin.putExtra("intent","");
                        intentOfLogin.putExtra("portal","");
                        intentOfLogin.putExtra("query","");

                        if (isConfirmed) {
                            url += String.format("/Default.aspx?intent=%1$s&portal=%2$s&query=%3$s",webPart,portal,query);
                            WebViewerActivity.previousUrl = WebViewerActivity.startUrl;

                            navigateTo(url);
                        }
                    }
                };

                if (WebViewerActivity.startUrl == K12NetUserReferences.getConnectionAddress() || WebViewerActivity.startUrl.contains("Login.aspx")) {
                    isConfirmed = true;
                    confirmComplated.run();
                } else {
                    setConfirmDialog(ctx.getString(R.string.confirmation),ctx.getString(R.string.navToNotify),confirmComplated);
                }
            }
        }
    }

    private void navigateTo(String url) {
        WebViewerActivity.startUrl = url;

        if (LoginAsyncTask.isLogin) {
            Intent intent = new Intent(ctx, WebViewerActivity.class);
            ctx.startActivity(intent);
        } else {
            Intent intent = new Intent(ctx, LoginActivity.class);

            final String portal = this.getIntent().getExtras().getString("portal","");
            final String query = this.getIntent().getExtras().getString("query","");
            final String webPart = this.getIntent().getExtras().getString("intent","");

            intent.putExtra("intent",webPart);
            intent.putExtra("portal", portal);
            intent.putExtra("query", query);

            ctx.startActivity(intent);
        }
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

        try {
            builder.show();
        } catch (Exception e) {

        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {

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

                String filePath = getPath(this, Uri.parse(resultStr));// getFilePathFromContent(resultStr);
                File file = new File(filePath);
                uriArray.add(Uri.fromFile(file));
            }
            else if(intent.getClipData() != null && intent.getClipData().getItemCount() > 0) {
                for(int i = 0; i < intent.getClipData().getItemCount();i++) {
                    String filePath = getPath(this, intent.getClipData().getItemAt(i).getUri());//getFilePathFromContent(intent.getClipData().getItemAt(i).getUri().getPath());
                   // String filePath = getFilePathFromContent(intent.getClipData().getItemAt(i).getUri().getPath());
                    File file = new File(filePath);
                    uriArray.add(Uri.fromFile(file));
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
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if(screenAlwaysOn) {

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

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

        webview = new WebView(WebViewerActivity.this);
        webview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
            { WebView.setWebContentsDebuggingEnabled(true); }
        }

        webview.setWebViewClient(new WebViewClient() {

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

            @Override
            @SuppressWarnings("deprecation")
            public void onReceivedError(WebView view, int errorCod,String description, String failingUrl) {
                Toast.makeText(getApplicationContext(), "Your Internet Connection May not be active Or " + description , Toast.LENGTH_LONG).show();
            }

            @Override
            @TargetApi(Build.VERSION_CODES.M)
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {

                Toast.makeText(getApplicationContext(),
                        "WebView Error" + error.getDescription(),
                        Toast.LENGTH_SHORT).show();

                super.onReceivedError(view, request, error);

            }

            /*
              Added in API level 23
            */
            @Override
            public void onReceivedHttpError(WebView view,
                                            WebResourceRequest request, WebResourceResponse errorResponse) {

               /* Toast.makeText(getActivity(),
                        "WebView Error" + errorResponse.getReasonPhrase(),
                        Toast.LENGTH_SHORT).show();*/


                super.onReceivedHttpError(view, request, errorResponse);
            }
            /*
             * Added in API level 23 replacing :-
             *
             * onReceivedError(WebView view, int errorCode, String description, String failingUrl)
             */
   /*@Override
   public void onReceivedError(WebView view, WebResourceRequest request,
                               WebResourceError error) {

       Toast.makeText(getActivity(),
               "WebView Error" + error.getDescription(),
               Toast.LENGTH_SHORT).show();

       super.onReceivedError(view, request, error);

   }*/

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

        List<Cookie> cookies = K12NetHttpClient.getCookieList();
        Cookie sessionInfo = null;

        if (!cookies.isEmpty()) {
            CookieSyncManager.createInstance(getApplicationContext());
            CookieManager cookieManager = CookieManager.getInstance();

            for (Cookie cookie : cookies) {
                sessionInfo = cookie;
                String cookieString = sessionInfo.getName() + "=" + sessionInfo.getValue() + "; domain=" + sessionInfo.getDomain();
                cookieManager.setCookie(K12NetUserReferences.getConnectionAddress(), cookieString);
                CookieSyncManager.getInstance().sync();
            }

            Log.d("LNG", webview.getContext().getString(R.string.localString));

            String cookieString = "UICulture" + "=" + K12NetUserReferences.getLanguageCode() + "; domain=" + sessionInfo.getDomain();
            cookieManager.setCookie(K12NetUserReferences.getConnectionAddress(), cookieString);

            cookieString = "Culture" + "=" + K12NetUserReferences.getLanguageCode() + "; domain=" + sessionInfo.getDomain();
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

        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setBuiltInZoomControls(true);
        webview.getSettings().setDisplayZoomControls(false);
        webview.getSettings().setAllowFileAccess(true);
        webview.getSettings().setLoadWithOverviewMode(true);
        webview.getSettings().setUseWideViewPort(true);
        webview.getSettings().setGeolocationEnabled(true);

        webview.setWebChromeClient(new WebChromeClient() {

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

                if (hasReadAccess) {

                    mFilePathCallback = filePathCallback;
                    fileSelectorIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    fileSelectorIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    fileSelectorIntent.setType("*/*");
                    fileSelectorIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
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
                    if (checkReadPermission()) {
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
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, possibleFileName);

                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    dm.enqueue(request);
                    Toast.makeText(getApplicationContext(), "Downloading File", Toast.LENGTH_LONG).show();
                }
            }
        });

        // Enable Caching
        // enableHTML5AppCache(webview);

        K12NetMobileJavaScriptInterface javaInterface = new K12NetMobileJavaScriptInterface();
        webview.addJavascriptInterface(javaInterface, "HTMLOUT");

        webview.loadUrl(startUrl);
        mainLayout.removeAllViews();
        mainLayout.addView(webview);

        ctx = this;

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        this.onNewIntent(this.getIntent());
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

        LinearLayout back_button = (LinearLayout) findViewById(R.id.lyt_back);
        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        LinearLayout lyt_toolbar = (LinearLayout) findViewById(R.id.lyt_option_buttons);

    }

    @Override
    public void onBackPressed() {
        if (previousUrl != null) {
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
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
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
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
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
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
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

    public static String getPath(final Context context, final Uri uri) {

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
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
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
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

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
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
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

}
