# AGP Upgrade Walkthrough

The Android Gradle Plugin (AGP) has been manually upgraded to version `8.13.2` to resolve the block in the Android Studio Upgrade Assistant.

## Changes Made

### Build Configuration

#### [MODIFY] [build.gradle (Project)](file:///D:/TFS/Mobile/GitHub/K12NET/K12NET-Mobile-Android/build.gradle)
- Updated the AGP version from `8.11.1` to `8.13.2`.

## Verification Results

### Automated Build
- Executed `./gradlew :app:assembleDebug` and the build finished **SUCCESSFULLY**.
- The project is now aligned with the latest stable requirements for Android 16 (API 36).

## Summary
| Component | Old Version | New Version |
| :--- | :--- | :--- |
| AGP | 8.11.1 | 8.13.2 |
| Gradle | 8.14.5 | 8.14.5 (No change) |
