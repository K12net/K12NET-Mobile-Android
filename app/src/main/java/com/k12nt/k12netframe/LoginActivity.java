package com.k12nt.k12netframe;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import org.apache.http.cookie.Cookie;
import android.webkit.CookieManager;

import com.k12nt.k12netframe.async_tasks.LoginAsyncTask;
import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;
import com.k12nt.k12netframe.utils.webConnection.K12NetHttpClient;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class LoginActivity extends Activity {

    public static String providerId;
    final Context context = this;
   // final int anim_len = 75 * 50;
     final int anim_wait_len = 1200;

    Handler handler = new Handler();

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.setIntent(intent);

        if(intent != null && intent.getExtras() != null) {
            String uri = intent.getExtras().getString("intent",null);

            if(uri != null) {
                final CheckBox chkRememberMe = (CheckBox) findViewById(R.id.chk_remember_me);

                if (chkRememberMe.isChecked()) {
                    Button login_button = (Button) findViewById(R.id.btn_login_submit);
                    login_button.performClick();
                }
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // refresh your views here
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onResume(){
        K12NetSettingsDialogView.setLanguageToDefault(getBaseContext());

        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        K12NetUserReferences.initUserReferences(getApplicationContext());

        if(K12NetUserReferences.getLanguageCode() == null){
            K12NetUserReferences.setLanguage(this.getString(R.string.localString));
        }

        K12NetHttpClient.resetBrowser(getApplicationContext());

        K12NetSettingsDialogView.setLanguageToDefault(getBaseContext());

        setContentView(R.layout.k12net_login_layout);

        final EditText username = (EditText) findViewById(R.id.txt_login_username);
        final EditText password = (EditText) findViewById(R.id.txt_login_password);
        final CheckBox chkRememberMe = (CheckBox) findViewById(R.id.chk_remember_me);

        chkRememberMe.setChecked(K12NetUserReferences.getRememberMe());

        username.setText(K12NetUserReferences.getUsername());
        if (chkRememberMe.isChecked()) {
            password.setText(K12NetUserReferences.getPassword());
        }

        ImageView img_logo = (ImageView) findViewById(R.id.img_login_icon);

        Button login_button = (Button) findViewById(R.id.btn_login_submit);

        login_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                K12NetUserReferences.setUsername(username.getText().toString());
                K12NetUserReferences.setPassword(password.getText().toString());
                K12NetUserReferences.setRememberMe(chkRememberMe.isChecked());

            /*    Mint.addExtraData("enteredUserName", username.getText().toString());
                Mint.addExtraData("enteredPassword", password.getText().toString());
                Mint.addExtraData("enteredUrl", K12NetUserReferences.getConnectionAddress());
                Mint.addExtraData("enteredFS", K12NetUserReferences.getFileServerAddress());*/

                K12NetHttpClient.resetBrowser(getApplicationContext());

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

                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, 1);
                SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                String strUTCDate = dateFormatter.format(cal.getTime());

                K12NetHttpClient.setCookie("UICulture", K12NetUserReferences.getLanguageCode(), strUTCDate);
                K12NetHttpClient.setCookie("Culture", K12NetUserReferences.getLanguageCode(), strUTCDate);

                LoginAsyncTask loginTasAsyncTask = new LoginAsyncTask(context, username.getText().toString(), password.getText().toString(), LoginActivity.this);
                loginTasAsyncTask.execute();

                //Log.d("time", "login basildi: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS").format(System.currentTimeMillis())) ;

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
            login_button.performClick();
        }
    }
}
