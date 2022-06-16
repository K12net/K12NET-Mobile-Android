package com.k12nt.k12netframe.utils.webConnection;

import android.os.Looper;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.util.List;
import com.loopj.android.http.*;

public class K12NetHttpClient {
    // A SyncHttpClient is an AsyncHttpClient
    public static AsyncHttpClient syncHttpClient = new SyncHttpClient();
    public static AsyncHttpClient asyncHttpClient = new AsyncHttpClient();

    /**
     * @return an async client when calling from the main thread, otherwise a sync client.
     */
    public static AsyncHttpClient getClient()
    {
        // Return the synchronous HTTP client when the thread is not prepared
        if (Looper.myLooper() == null)
            return syncHttpClient;
        return asyncHttpClient;
    }

    public static void resetBrowser() {
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
    }

    public static List<HttpCookie> getCookieList(){
        CookieManager cookieManager = ((CookieManager) CookieManager.getDefault());

        if(cookieManager == null) {
            resetBrowser();
            cookieManager = ((CookieManager) CookieManager.getDefault());
        }

        List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();

        return  cookies;
    }

    public static void setCookie(String name, String value, String expires){
        android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
        String cookieString = name + "=" + value + ";expires=" + expires + "; path=/;Domain=.k12net.com";
        cookieManager.setCookie(".k12net.com", cookieString);
    }
}
