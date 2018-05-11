package com.k12nt.k12netframe.utils.webConnection;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.k12nt.k12netframe.async_tasks.LoginAsyncTask;
import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class K12NetHttpClient {

    private static final int CONNECTION_TIMEOUT = 7000;
    private static final int WAIT_RESPONSE_TIMEOUT = 10000;
    public static int clientId = 0;

    private static HttpContext httpContext;

    private static CookieStore cookie_store;

    public static DefaultHttpClient client;

    private static Context loginContext;

    public static void setContext(HttpContext aHttpContext) {
        httpContext = aHttpContext;
    }

    public static String execute(String url, JSONObject changeSet) throws Exception {
        HttpClient client = getClient();
        HttpResponse response = null;

        HttpPost httpPost = new HttpPost(url);

        StringEntity entity = null;
        try {
            entity = new StringEntity(changeSet.toString(), HTTP.UTF_8);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        entity.setContentType("application/json");

        httpPost.setEntity(entity);

        return execute(httpPost);
    }

    public static String execute(String url, String changeSet) throws Exception {
        HttpClient client = getClient();
        HttpResponse response = null;

        HttpPost httpPost = new HttpPost(url);

        StringEntity entity = null;
        try {
            entity = new StringEntity(changeSet, HTTP.UTF_8);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        entity.setContentType("application/json");

        httpPost.setEntity(entity);

        return execute(httpPost);
    }

    public static String execute(HttpPost httpPost) throws Exception {
        HttpClient client = getClient();
        HttpResponse response = null;

        String line = "";
        if (client != null) {
            //Log.d("httpClient", httpPost.getURI().toJson());
            //Log.d("httpClient", "HttpClient post Başladı: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(System.currentTimeMillis()));
            try {
                response = client.execute(httpPost, httpContext);
                line = checkCookie(response);
                if(line != null && line.isEmpty()){
                    response = client.execute(httpPost, httpContext);
                    line = checkCookie(response);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return line;
    }

    public static String execute(HttpGet httpGet) throws Exception {
        HttpClient client = getClient();
        HttpResponse response = null;
        String line = "";
        if (client != null) {
            try {
                response = client.execute(httpGet, httpContext);
                line = checkCookie(response);
                if (line != null && line.isEmpty()) {
                    response = client.execute(httpGet, httpContext);
                    line = checkCookie(response);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return line;
    }

    private static String checkCookie(HttpResponse response) throws Exception {
        String line = "";
        if (response.getStatusLine().getStatusCode() / 100 == 5) {
            Log.d("PARSE_ERROR", response.getStatusLine().getReasonPhrase());
            Log.d("PARSE_ERROR", line);
            throw new Exception();
        }
        if (response != null) {
            try {



                InputStream in = response.getEntity().getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                line = reader.readLine();
                while (reader.ready()) {
                    line += reader.readLine();
                }
                if(line.length() > 100){
                    String subline = line.substring(0,99).toLowerCase();
                    if(subline.contains("authentication failed")) {
                        LoginAsyncTask.login();
                        line = "";
                    }
                }

            }catch (IOException e) {
                e.printStackTrace();
            }
        }
        return line;
    }


    private static DefaultHttpClient getClient() {

        ConnectivityManager cn = (ConnectivityManager) loginContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo nf = cn.getActiveNetworkInfo();
        if ((nf != null && nf.isConnected() == true) == false) {
            //todo bu kısımda hata geliyor, login contect null çünkü
            //Toast.makeText(loginContext, R.string.noNetwork, Toast.LENGTH_LONG).show();
            return null;
        }

        if (client == null) {

            HttpParams params = new BasicHttpParams();
            params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
            HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, WAIT_RESPONSE_TIMEOUT);
            HttpConnectionParams.setTcpNoDelay(params, true);


        /*    SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            final SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
            schemeRegistry.register(new Scheme("https", sslSocketFactory, 443));
            ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
            client = new DefaultHttpClient(cm, params);
            client.getCookieStore().getCookies();*/

            client = new DefaultHttpClient(params);

          //  CookieStore httpCookieStore = new BasicCookieStore();
          //  client.setCookieStore(httpCookieStore);

            clientId++;
        }
        return client;
    }

    public static void resetBrowser(Context ctx) {
        loginContext = ctx;

        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        HttpContext http_context = new BasicHttpContext();
        cookie_store = new BasicCookieStore();
        http_context.setAttribute(ClientContext.COOKIE_STORE, cookie_store);
        K12NetHttpClient.setContext(http_context);
    }

    public static void initContext(Context context) {
        loginContext = context;
    }

    public static List<Cookie> getCookieList(){
        return cookie_store.getCookies();
    }

    public static void setCookie(String name, String value, String expires){
        android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
        String cookieString = name + "=" + value + ";expires=" + expires + "; path=/;Domain=.k12net.com";
        cookieManager.setCookie(".k12net.com", cookieString);
    }
}
