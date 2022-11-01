package com.k12nt.k12netframe;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.k12nt.k12netframe.attendance.AttendanceManager;
import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;

import java.util.Locale;

public class K12NetSettingsDialogView extends K12NetDialogView {

    private WebViewerActivity Activity;

	public K12NetSettingsDialogView(Context context,WebViewerActivity activity) {
		super(context);
		this.Activity = activity;
	}
	
	protected int getToolbarIcon() {
		return R.drawable.k12net_logo;
	}

	@Override
	protected int getToolbarTitle() {
		return R.string.action_settings;
	}
	
	@Override
	public View getDialogView(Object objView) { 

        View view = inflater.inflate(R.layout.k12net_setting_layout, null);

        final EditText appAddress = (EditText) view.findViewById(R.id.txt_connection_address);

        appAddress.setText(K12NetUserReferences.getConnectionAddress());

        Switch swtGeoFenceMonitor = (Switch) view.findViewById(R.id.swtGeoFenceMonitor);

        if(K12NetUserReferences.isPermitBackgroundLocation() == null || K12NetUserReferences.isPermitBackgroundLocation() == true) {
            swtGeoFenceMonitor.setChecked(true);
        } else {
            swtGeoFenceMonitor.setChecked(false);
        }

        final WebViewerActivity activity = this.Activity;

        swtGeoFenceMonitor.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton var1, boolean isChecked) {
                if(isChecked) {
                    Toast.makeText(activity, activity.getString(R.string.login_required), Toast.LENGTH_LONG).show();
                } else {
                    AttendanceManager.Instance().stopAttendanceService(activity);
                }

                K12NetUserReferences.setPermitBackgroundLocation(isChecked);
            }
        });

        swtGeoFenceMonitor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShapeDrawable shapedrawable = new ShapeDrawable();
                shapedrawable.setShape(new RectShape());
                shapedrawable.getPaint().setColor(Color.RED);
                shapedrawable.getPaint().setStrokeWidth(10f);
                shapedrawable.getPaint().setStyle(Paint.Style.STROKE);
                swtGeoFenceMonitor.setBackground(shapedrawable);
            }
        });

        Button save_button = (Button) view.findViewById(R.id.btn_save);

        save_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                K12NetUserReferences.setConnectionAddress(appAddress.getText().toString());
                dismiss();
            }
        });

        Button btn_policy = (Button) view.findViewById(R.id.btn_policy);

        btn_policy.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);

                //todo: App provider must include their own PrivacyPolicy by changing below url
                String lang = K12NetUserReferences.getLanguageCode();
                if (!lang.equals("tr") && !lang.equals("ar")) lang = "en";
                intent.setData(Uri.parse(String.format("http://fs.k12net.com/mobile/files/PrivacyPolicy_%s.html",lang)));
                getContext().startActivity(intent);
            }
        });

		return view;
		
	}

    public static void setLanguageToDefault(Context context){
        K12NetUserReferences.initUserReferences(context);

        Locale myLocale = new Locale(K12NetUserReferences.getLanguageCode().split("-")[0]);
        Resources res = context.getResources();
        Configuration configuration = res.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
            configuration.setLocale(myLocale);
        }

        configuration.locale = myLocale;

        Locale.setDefault(myLocale);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            context.createConfigurationContext(configuration);
        }

        res.updateConfiguration(configuration,res.getDisplayMetrics());
    }
}
