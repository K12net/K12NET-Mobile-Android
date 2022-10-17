package com.k12nt.k12netframe.utils.webConnection;

import android.os.Looper;

import java.net.HttpCookie;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;
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

    public static void initCookies() {
        K12NetHttpClient.resetBrowser();

        android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();

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
    }

    public static void initLoginCookies() {
        CookieManager cookieManager = resetBrowser();
        String cookie = "UICulture" + "=" + K12NetUserReferences.getLanguageCode() + "; domain=k12net.com";
        cookieManager.getCookieStore().add(null,HttpCookie.parse(cookie).get(0));
        cookie = "Culture" + "=" + K12NetUserReferences.getLanguageCode() + "; domain=k12net.com";
        cookieManager.getCookieStore().add(null,HttpCookie.parse(cookie).get(0));
    }

    public static CookieManager resetBrowser() {
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        return cookieManager;
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

    public static String getCookie(String siteName,String cookieName){
        String cookieValue = null;

        android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
        String cookies = cookieManager.getCookie(siteName);
        String[] temp=cookies.split(";");
        for (String ar1 : temp ){
            if(ar1.contains(cookieName)){
                String[] temp1=ar1.split("=");
                cookieValue = temp1[1];
                break;
            }
        }
        return cookieValue;
    }

    public static void setCookie(String name, String value, String expires){
        android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
        String cookieString = name + "=" + value + ";expires=" + expires + "; path=/;Domain=.k12net.com";
        cookieManager.setCookie(".k12net.com", cookieString);
    }
}
