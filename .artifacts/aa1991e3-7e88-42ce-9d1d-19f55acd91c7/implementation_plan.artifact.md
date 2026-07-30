# Final Review and Stability Enhancements

This plan reviews the latest changes from the other agent and implements the remaining stability fixes for background location and UI consistency.

## Review of Latest Changes
The recent updates by the other agent are **excellent**:
- **`goAsync()`:** Correctly ensures that the network request for attendance completes in the background.
- **`BootCompletedReceiver`:** Correctly restarts the attendance monitor after a device reboot.
- **Wakelock Management:** Proper `try-finally` blocks prevent battery drain.

## Proposed Changes

### 1. Manifest & Boot Integration
#### [MODIFY] [AndroidManifest.xml](file:///D:/TFS/Mobile/GitHub/K12NET/K12NET-Mobile-Android/app/src/main/AndroidManifest.xml)
- Add `RECEIVE_BOOT_COMPLETED` permission.
- Register `BootCompletedReceiver` to handle system boot and app updates.

### 2. Background Location Fixes (Android 10 & 11+)
#### [MODIFY] [AttendanceManager.java](file:///D:/TFS/Mobile/GitHub/K12NET/K12NET-Mobile-Android/app/src/main/java/com/k12nt/k12netframe/attendance/AttendanceManager.java)
- Update version checks for `ACCESS_BACKGROUND_LOCATION` from API 30 (`R`) to **API 29 (`Q`)**.
- Implement two-stage permission flow for Android 11+: Request foreground location first, then request background location in `onRequestPermissionsResult`.

#### [MODIFY] [WebViewerActivity.java](file:///D:/TFS/Mobile/GitHub/K12NET/K12NET-Mobile-Android/app/src/main/java/com/k12nt/k12netframe/WebViewerActivity.java)
- Update permission request logic to handle the two-stage flow for Android 11+.

### 3. UI Consistency
#### [MODIFY] [res/values/styles.xml](file:///D:/TFS/Mobile/GitHub/K12NET/K12NET-Mobile-Android/app/src/main/res/values/styles.xml)
- Define `Asisto_PopupDialog` for phones (not just tablets) to ensure the settings dialog looks consistent across all devices.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to verify no regressions in build or manifest merging.

### Manual Verification
- **Boot Test:** Simulate a boot broadcast using `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED`.
- **Permission Test:** On Android 11+, verify that the app correctly prompts for "Always Allow" location access after the initial "While using app" prompt.
- **UI Test:** Open settings on a phone-sized device and verify the dialog uses the correct theme.
