package com.k12nt.k12netframe.utils.userSelection;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import com.google.android.gms.location.Geofence;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.k12nt.k12netframe.K12NetSettingsDialogView;
import com.k12nt.k12netframe.attendance.GeoFenceData;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;

public class K12NetUserReferences {
	
	public static String DATA_FILE_PATH = Environment.getExternalStorageDirectory() + "/MobiDers/context/";
	public static final String IMG_FILE_PATH = DATA_FILE_PATH + "temp_img/";
	public static String FILE_ENCODING_CHARSET = "UTF-8";

    //public static boolean LANG_UPDATED = true;

    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";

	private static final String SETTINGS_FILE_NAME = "userSettings";
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
    private static final String LANGAUGE = "languageCode";
	private static final String CONNECTION_ADDRESS = "connectionAddress";
    private static final String REMEMBER_PASSWORD = "rememberPassword";
    private static final String PERMIT_LOCATION_SERVICES = "permitLocationService";
	private static final String LIGHT_OPTION = "lightOption";
    private static final String CALENDAR_PROVIDER_ID = "calendarProviderId";
    private static final String BADGENUMBER = "badgeNumber";
    private static final String WARNVERSION = "warnVersion";
    private static final String TOKEN = "token";

	private static K12NetUserReferences references = null;

	private SharedPreferences settings;
	private String username;
	private String password;
	private String connectionString;
    private boolean rememberPassword;
    private Boolean permitBackgroundLocation = null;
    private int badgeNumber;
    private String languageCode;
    private String warnedVersion;
    private String token;
	
	public K12NetUserReferences(Context context) {
		settings = context.getSharedPreferences(SETTINGS_FILE_NAME, Context.MODE_PRIVATE);
		username = settings.getString(USERNAME, null);
		password = settings.getString(PASSWORD, null);
        languageCode = settings.getString(LANGAUGE, null);
        connectionString = settings.getString(CONNECTION_ADDRESS, null);

        if (connectionString == null) connectionString = "https://okul.k12net.com";

        if(languageCode == null) languageCode = Locale.getDefault().getLanguage().split("_")[0].split("-")[0].toLowerCase();

		rememberPassword = settings.getBoolean(REMEMBER_PASSWORD, false);
        permitBackgroundLocation = settings.contains(PERMIT_LOCATION_SERVICES) ?
                settings.getBoolean(PERMIT_LOCATION_SERVICES, false) : null;
        badgeNumber = settings.getInt(BADGENUMBER, 0);
        warnedVersion = settings.getString(WARNVERSION, null);
        token = settings.getString(TOKEN, "");
	}

	public static void initUserReferences(Context context) {
        if(references == null){
	    	references = new K12NetUserReferences(context);
	    }
    }

    private void storeString(String key, String value) {
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(key, value);
		editor.commit();
	}

	private void storeBoolean(String key, boolean value) {
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(key, value);
		editor.commit();
	}

    private void storeInt(String key, int value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        editor.commit();
    }
	
	public static String getConnectionAddress(){
		String http_address = references.connectionString;
		if(!http_address.startsWith("http")) {
			http_address = "http://" + http_address;
		}
		return http_address;
	}

    public static String getConnectionAddressDomain(){
        String[] parts = getConnectionAddress().split("://");
        String domain = getConnectionAddress();

        if(parts.length == 2) {
            domain = parts[1].split("/")[0];
        }

        return domain;
    }
	
	public static String getUsername(){
        if(references.username == null) return "";
		return references.username;
	}
	
	public static String getPassword(){
		return references.password;
	}
	public static void setConnectionAddress(String conAddress){
		references.connectionString = conAddress;
		references.storeString(CONNECTION_ADDRESS, references.connectionString);
	}

	public static void setUsername(String username) {
		references.username = username;
		references.storeString(USERNAME, references.username);
	}

	public static void setPassword(String password) {
		references.password = password;
		references.storeString(PASSWORD, references.password);
	}

    public static void increaseBadgeNumber() {
        references.badgeNumber++;
        references.storeInt(BADGENUMBER, references.badgeNumber);
    }

    public static void resetBadgeNumber() {
        references.badgeNumber = 0;
        references.storeInt(BADGENUMBER, references.badgeNumber);
    }

	public static boolean getRememberMe() {
		return references.rememberPassword;
	}

    public static Boolean isPermitBackgroundLocation() {
        return references.permitBackgroundLocation;
    }

    public static int getBadgeCount() {
        return references.badgeNumber;
    }

    public static void setRememberMe(boolean rememberMe) {
        references.rememberPassword = rememberMe;
        references.storeBoolean(REMEMBER_PASSWORD, references.rememberPassword);
    }

    public static void setPermitBackgroundLocation(boolean permitBackgroundLocation) {
        references.permitBackgroundLocation = permitBackgroundLocation;
        references.storeBoolean(PERMIT_LOCATION_SERVICES, references.permitBackgroundLocation);
    }

    public static void setLanguage(String languageCode) {
        references.languageCode = languageCode;
        references.storeString(LANGAUGE, references.languageCode);
    }

    public static String getLanguageCode(){
	    if (references.languageCode == null) {
            setLanguage("en");

            String language = Locale.getDefault().getLanguage();
            language = language.split("_")[0].split("-")[0].toLowerCase();
            setLanguage(language);
        }
        return references.languageCode;
    }

    public static void setWarnedVersionString(String newWarnedVersionStr) {
        references.warnedVersion = newWarnedVersionStr;
        references.storeString(WARNVERSION, references.warnedVersion);
    }

    public static String getWarnedVersionString(){
        return references.warnedVersion;
    }

    public static void setDeviceToken(String newDeviceToken) {
        references.token = newDeviceToken;
        references.storeString(TOKEN, references.token);
    }

    public static String getDeviceToken(){
        return references.token;
    }

}
