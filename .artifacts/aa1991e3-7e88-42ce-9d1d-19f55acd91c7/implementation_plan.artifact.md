# Modern Android Compatibility Fixes (API 21-36)

This plan addresses critical code areas that may cause crashes or errors on modern Android versions (up to API 36) while maintaining support for `minSdkVersion 21`.

## User Review Required

> [!IMPORTANT]
> - **Scoped Storage:** I am migrating data storage from the root external directory to the app's external files directory (`/Android/data/...`). This is required for Android 10+ compatibility.
> - **Foreground Services:** On Android 14+, foreground services *must* declare a type. I'm adding `FOREGROUND_SERVICE_TYPE_LOCATION` to the attendance service call.
> - **Package Visibility:** On Android 11+, apps cannot "see" other apps unless declared. I'm adding `<queries>` to the manifest to ensure the app can still open browsers and email clients.

## Proposed Changes

### 1. Build & Manifest Updates
#### [MODIFY] [AndroidManifest.xml](file:///D:/TFS/Mobile/GitHub/K12NET/K12NET-Mobile-Android/app/src/main/AndroidManifest.xml)
- Add `<queries>` block for browser and email intents.
- Ensure all exported components have intent filters (already done in previous steps).

### 2. Core Logic & Crash Prevention
#### [MODIFY] [WebViewerActivity.java](file:///D:/TFS/Mobile/GitHub/K12NET/K12NET-Mobile-Android/app/src/main/java/com/k12nt/k12netframe/WebViewerActivity.java)
- Fix `PendingIntent` mutability flags (add `FLAG_IMMUTABLE` or `FLAG_MUTABLE`) where missing.
- Update `getPackageInfo` usage for API 33+.
- Replace `Uri.fromFile` with `FileProvider.getUriForFile` for sharing files with external apps.

#### [MODIFY] [AttendanceManager.java](file:///D:/TFS/Mobile/GitHub/K12NET/K12NET-Mobile-Android/app/src/main/java/com/k12nt/k12netframe/attendance/AttendanceManager.java)
- Pass `ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION` to `startForeground`.

### 3. Data & Storage
#### [MODIFY] [K12NetUserReferences.java](file:///D:/TFS/Mobile/GitHub/K12NET/K12NET-Mobile-Android/app/src/main/java/com/k12nt/k12netframe/utils/userSelection/K12NetUserReferences.java)
- Migrate `DATA_FILE_PATH` from `Environment.getExternalStorageDirectory()` to `context.getExternalFilesDir(null)`.

## Verification Plan

### Automated Tests
- Run `./gradlew clean assembleDebug`.

### Manual Verification
- **Android 14/15/16 Testing:** Launch the attendance service and verify it doesn't crash on `startForeground`.
- **File Chooser:** Test picking a file and sharing it; verify `FileProvider` works correctly.
- **Intent Resolution:** Verify that "Open with Browser" or "Send Email" buttons still work on Android 11+.
