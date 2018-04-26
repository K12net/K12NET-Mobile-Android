package com.k12nt.k12netframe;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import com.k12nt.k12netframe.async_tasks.LoginAsyncTask;
import com.k12nt.k12netframe.utils.definition.K12NetStaticDefinition;
import com.k12nt.k12netframe.utils.helper.K12NetHelper;
import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;
import com.k12nt.k12netframe.utils.webConnection.K12NetHttpClient;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class LoginActivity extends Activity {

    final Context context = this;

    boolean isUptoDate = false;

    EditText username;
    EditText password;
    CheckBox chkRememberMe;

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

        setContentView(R.layout.k12net_login_layout);

        username = (EditText) findViewById(R.id.txt_login_username);
        password = (EditText) findViewById(R.id.txt_login_password);
        chkRememberMe = (CheckBox) findViewById(R.id.chk_remember_me);

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
                checkCurrentVersion();
            }
        });

        Button settings_button = (Button) findViewById(R.id.btn_settings);
        settings_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                K12NetSettingsDialogView dialogView = new K12NetSettingsDialogView(arg0.getContext());
                dialogView.createContextView(null);
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

        new GetLatestVersion(context).execute();

    }

    private void StartLoginOperation() {
        K12NetUserReferences.setUsername(username.getText().toString());
        K12NetUserReferences.setPassword(password.getText().toString());
        K12NetUserReferences.setRememberMe(chkRememberMe.isChecked());

        K12NetHttpClient.resetBrowser(getApplicationContext());
        LoginAsyncTask loginTasAsyncTask = new LoginAsyncTask(context, username.getText().toString(), password.getText().toString(), LoginActivity.this);
        loginTasAsyncTask.execute();
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

                Document doc = Jsoup.connect(K12NetStaticDefinition.DETAILED_APP_ADDRESS).get();
                latestVersion = doc.getElementsByAttributeValue
                        ("itemprop","softwareVersion").first().text();

                //check if version number only has 2 segment
                if(K12NetHelper.findPattermCount(latestVersion, ".") < 2){
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
                    if(!isFinishing()){ //This would help to check the context of whether activity is running or not, otherwise you'd get bind error sometimes
                        showUpdateDialog();
                    }
                }

                else if (currentVersionInt[2] < latestVersionInt[2]){
                    isUptoDate = true;
                    if(!isFinishing()){ //This would help to check the context of whether activity is running or not, otherwise you'd get bind error sometimes
                        showWarningDialog(latestVersion);
                    }
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
    }

    private void showWarningDialog(final String latestVersion){
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
    }

    private void showAlertDialog(int alertText){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(alertText);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setCancelable(false);
        builder.show();
    }

}
