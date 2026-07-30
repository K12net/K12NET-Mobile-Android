package com.k12nt.k12netframe.attendance;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;

/**
 * Geofence kayitlari cihaz yeniden baslatildiginda Android tarafindan silinir; uygulamanin
 * bunlari yeniden kaydetmesi gerekir. Ayni durum uygulama guncellendiginde de olusabilir.
 * <p>
 * Bu receiver olmadan yoklama, kullanici uygulamayi tekrar acip bir portala girene kadar
 * ({@code WebViewerActivity.initLocationServices}) calismaz.
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "BootCompletedReceiver";

    /** Bazi ureticiler (HTC/Xiaomi) hizli acilista standart broadcast yerine bunu gonderir. */
    private static final String ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();

        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                && !ACTION_QUICKBOOT_POWERON.equals(action)) {
            return;
        }

        // Acilis sirasinda firlatilan bir istisna kullaniciya "uygulama durduruldu" olarak
        // yansir; bu yuzden tum yol savunmaci sekilde sarmalandi.
        try {
            K12NetUserReferences.initUserReferences(context);

            // Kullanici konum takibini ayarlardan kapatmissa yeniden baslatma.
            Boolean permitted = K12NetUserReferences.isPermitBackgroundLocation();
            if (permitted != null && !permitted) {
                Log.i(TAG, "Background location disabled by user; skipping restart");
                return;
            }

            // Daha once giris yapilmamissa sunucuya yoklama gonderilemez.
            String deviceToken = K12NetUserReferences.getDeviceToken();
            if (deviceToken == null || deviceToken.isEmpty()) {
                Log.i(TAG, "No device token; skipping restart");
                return;
            }

            if (!hasLocationPermissions(context)) {
                // Izin verilmeden location tipli foreground service baslatmak
                // Android 14+ da SecurityException firlatir.
                Log.i(TAG, "Location permissions missing; skipping restart");
                return;
            }

            // AttendanceManager.onCreate() geofence listesini sunucudan cekip
            // startMonitorLocations() ile yeniden kaydeder.
            ContextCompat.startForegroundService(context, new Intent(context, AttendanceManager.class));

            Log.i(TAG, "AttendanceManager restarted after " + action);
        } catch (Exception e) {
            Log.e(TAG, "Failed to restart AttendanceManager after " + action, e);
        }
    }

    private static boolean hasLocationPermissions(Context context) {
        boolean hasForeground = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!hasForeground) return false;

        // ACCESS_BACKGROUND_LOCATION API 29 (Q) ile geldi ve arka planda geofence
        // tetiklemesi icin sarttir.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }
}
