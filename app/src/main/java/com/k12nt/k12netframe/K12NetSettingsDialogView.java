package com.k12nt.k12netframe;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.k12nt.k12netframe.attendance.AttendanceManager;
import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;

import java.util.Locale;

public class K12NetSettingsDialogView extends K12NetDialogView {

    public static String TURKISH = "tr";
    public static String ENGLISH = "en";
    public static String ARABIC = "ar";
    public static String FRENCH = "fr";
    public static String RUSSIAN = "ru";
    private LoginActivity Activity;

	public K12NetSettingsDialogView(Context context,LoginActivity activity) {
		super(context);
		this.Activity = activity;
	}

    ToggleButton[] language_btn_list = new ToggleButton[5];
	
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
        final EditText fsAddress = (EditText) view.findViewById(R.id.txt_fs_address);

        appAddress.setText(K12NetUserReferences.getConnectionAddress());
        fsAddress.setText(K12NetUserReferences.getFileServerAddress());

        Switch swtGeoFenceMonitor = (Switch) view.findViewById(R.id.swtGeoFenceMonitor);

        if(K12NetUserReferences.isPermitBackgroundLocation() == null || K12NetUserReferences.isPermitBackgroundLocation() == true) {
            swtGeoFenceMonitor.setChecked(true);
        } else {
            swtGeoFenceMonitor.setChecked(false);
        }

        final LoginActivity activity = this.Activity;

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

        Button save_button = (Button) view.findViewById(R.id.btn_save);

        save_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                K12NetUserReferences.setConnectionAddress(appAddress.getText().toString());
                K12NetUserReferences.setFileServerAddress(fsAddress.getText().toString());
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

        ToggleButton btn_tr = (ToggleButton) view.findViewById(R.id.btn_tr);
        ToggleButton btn_en = (ToggleButton) view.findViewById(R.id.btn_en);
        ToggleButton btn_ar = (ToggleButton) view.findViewById(R.id.btn_ar);
        ToggleButton btn_fr = (ToggleButton) view.findViewById(R.id.btn_fr);
        ToggleButton btn_ru = (ToggleButton) view.findViewById(R.id.btn_ru);

        language_btn_list[0] = btn_tr;
        language_btn_list[1] = btn_en;
        language_btn_list[2] = btn_ar;
        language_btn_list[3] = btn_fr;
        language_btn_list[4] = btn_ru;

        btn_tr.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                updateLanguage(0, TURKISH);
            }
        });

        btn_en.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                updateLanguage(1, ENGLISH);
            }
        });

        btn_ar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                updateLanguage(2, ARABIC);
            }
        });

        btn_fr.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                updateLanguage(3, FRENCH);
            }
        });

        btn_ru.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                updateLanguage(4, RUSSIAN);
            }
        });

        if(K12NetUserReferences.getLanguageCode().equals(TURKISH))  {
            updateLanguage(0, TURKISH);
        }
        else if(K12NetUserReferences.getLanguageCode().equals(ENGLISH)) {
            updateLanguage(1, ENGLISH);
        }
        else if(K12NetUserReferences.getLanguageCode().equals(ARABIC)) {
            updateLanguage(2, ARABIC);
        }
        else if(K12NetUserReferences.getLanguageCode().equals(FRENCH)) {
            updateLanguage(3, FRENCH);
        }
        else if(K12NetUserReferences.getLanguageCode().equals(RUSSIAN)) {
            updateLanguage(4, RUSSIAN);
        }

		return view;
		
	}

    private void updateLanguage(int languageIndex, String languageCode) {
        K12NetUserReferences.setLanguage(languageCode);

        for(int i = 0; i < language_btn_list.length;i++) {
            language_btn_list[i].setChecked(i == languageIndex);
        }

        setLanguageToDefault(getContext());
    }

    public static void setLanguageToDefault(Context context){
        K12NetUserReferences.initUserReferences(context);

        Locale myLocale = new Locale(K12NetUserReferences.getLanguageCode());
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
