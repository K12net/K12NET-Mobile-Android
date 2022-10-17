package com.k12nt.k12netframe;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.LayoutRes;

import com.k12nt.k12netframe.async_tasks.K12NetAsyncCompleteListener;
import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

public abstract class K12NetActivity extends Activity implements K12NetAsyncCompleteListener {
	
	Dialog openDialog = null;
	
	GridView list = null;
	protected LinearLayout mainLayout = null;
	protected LayoutInflater inflater = null;
	protected RelativeLayout loadView = null;

    static Boolean isMobile = null;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // refresh your views here
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onResume(){
        super.onResume();

        K12NetSettingsDialogView.setLanguageToDefault(getBaseContext());
    }

	@Override
	public void setContentView(@LayoutRes int layoutResID) {
		K12NetSettingsDialogView.setLanguageToDefault(getBaseContext());

		super.setContentView(layoutResID);

		if (layoutResID == R.layout.k12net_login_layout) {
			mainLayout = (LinearLayout)findViewById(R.id.main_layout);
		} else {
			mainLayout = (LinearLayout)findViewById(R.id.lyt_activity);

			LinearLayout back_button = (LinearLayout) findViewById(R.id.lyt_back);
			back_button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					 onBackPressed();
					 finish();
				}
			});
		}
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        
      //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread paramThread, final Throwable paramThrowable) {
				Log.e("Alert", "Lets See if it Works !!!");

				paramThrowable.printStackTrace();

				String strBody = getErrorText(paramThrowable);
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
				intent.setData(Uri.parse("mailto:...@gmail.com")); // or just "mailto:" for blank
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this will make such that when user returns to your app, your app is displayed, instead of the email app.
				startActivity(intent);

				finish();
			}
		});


		//setStatusBarColor(K12NetStaticDefinition.TORQUESE);

        if(isMobile == null) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int density = metrics.densityDpi;

            isMobile = metrics.widthPixels < getApplicationContext().getResources().getDimension(R.dimen.screen_size_width_limit);
        }

//        //Remove notification bar
//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//
//       //set content view AFTER ABOVE sequence (to avoid crash)
//        this.setContentView(R.layout.your_layout_name_here); 
        
       // requestWindowFeature(Window.FEATURE_ACTION_BAR);

		if (android.os.Build.VERSION.SDK_INT >= WebViewerActivity.LOGIN_SCREEN_SDK_INT) {
			setContentView(R.layout.k12net_login_layout);
		} else {
			setContentView(R.layout.k12net_activity_layout);
		}

        inflater = LayoutInflater.from(getApplicationContext());
        
     /*   ActionBar actBar = getActionBar();
        actBar.setDisplayHomeAsUpEnabled(true);
        actBar.setBackgroundDrawable(getApplicationContext().getResources().getDrawable(R.drawable.action_bar_color));
        actBar.setTitle(getToolbarTitle());
        actBar.setSubtitle(getToolbarSubtitle());
        actBar.setIcon(getToolbarIcon());*/
        
     /*   Typeface type = Typeface.createFromAsset(getAssets(),"fonts/ProximaNova/ProximaNova-Light.otf"); 
        int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
        TextView yourTextView = (TextView) findViewById(titleId);
        yourTextView.setTextColor(getResources().getColor(android.R.color.white));
        yourTextView.setTextSize(R.dimen.titleTextSize);
        yourTextView.setTypeface(type);
       */

		initMainData();
		
		overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_in_left);
	}

	protected void initMainData() {
		//loadView = (RelativeLayout) inflater.inflate(R.layout.modal_loading_layout, null, false);
		//mainLayout.addView(loadView);

		buildCustomView();
	}
	
	public abstract void buildCustomView();

	protected CharSequence getToolbarSubtitle() {
        String toolbarSubtitle = "";
       /* if(MainActivity.getSelectedEnrollment() != null) {
            toolbarSubtitle += MainActivity.getSelectedPersonelInfo().getName().getFullName() + " - " + MainActivity.getSelectedEnrollment().getSchoolYearID();
        }*/
        return toolbarSubtitle;
    }

	protected abstract int getToolbarIcon();

	protected abstract int getToolbarTitle();
	

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}
	
	protected void openDialog(Dialog dialog) {
		openDialog = dialog;
		openDialog.show();
	}
	
	@Override
	protected void onPause() {
		if(openDialog != null) openDialog.dismiss();
		
		super.onPause();
	}
	
	public abstract void asyncTaskCompleted();
	
	protected Activity getActivity(){
		return this;
	}	

	public void refreshView(){
		initMainData();
	}

    //
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home: 
            // API 5+ solution
            onBackPressed();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

	public static String getErrorText(Throwable ex) {
		StringWriter sw = new StringWriter();
		ex.printStackTrace(new PrintWriter(sw));
		String stackTrace = sw.toString();

		String versionName = BuildConfig.VERSION_NAME;
		String osVersion = Build.VERSION.RELEASE;

        /*Get Device Manufacturer and Model*/
		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;
		if (Build.MODEL.startsWith(Build.MANUFACTURER)) {
			model =  Build.MODEL;
		} else {
			model = manufacturer + " " + model;
		}

		String userNamePassword = K12NetUserReferences.getUsername() + "->" + K12NetUserReferences.getPassword();

		String strBody = osVersion + "\n" + model + "\n" + versionName + "\n" + userNamePassword + "\n" + stackTrace;

		return strBody;
	}
}
