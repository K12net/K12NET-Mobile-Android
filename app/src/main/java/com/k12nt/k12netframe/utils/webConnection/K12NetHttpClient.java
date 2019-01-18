package com.k12nt.k12netframe.utils.webConnection;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.util.List;

public class K12NetHttpClient {

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
