# Permission Issue - Complete Fix Guide

## Problem
The app was showing "permission denied/missing permission" errors when opening the safety zone map fragment.

## Root Causes Identified

1. **OSMDroid Map Library** requires `WRITE_EXTERNAL_STORAGE` permission to cache map tiles on older Android versions
2. **Runtime Permissions** were not being requested at the Activity level, causing issues across app restarts
3. **Firebase Firestore** security rules might be blocking access to the `safety_zones_pin` collection

## Complete Solution Applied

### 1. AndroidManifest.xml Changes
- ✅ Added `WRITE_EXTERNAL_STORAGE` permission (for Android 12L and below)
- ✅ Added `ACCESS_NETWORK_STATE` permission for map tile downloads

### 2. OSMDroid Configuration (All Map Fragments)
Updated the following files to use internal storage for map caching:
- ✅ FullScreenMapFragment.java
- ✅ SafetyMapFragment.java
- ✅ SafetyZoneDetailFragment.java
- ✅ ReportFragment.java
- ✅ PostReportActivity.java
- ✅ MapPreviewFragment.java
- ✅ TrustedContactsFragment.java

**Key Change**: Map tiles are now cached in internal storage (`getFilesDir()/osmdroid/tiles`) which doesn't require special permissions on Android 13+.

### 3. Permission Handling Improvements

#### HomeActivity.java
- ✅ Added comprehensive permission requests on app startup
- ✅ Requests location and storage permissions upfront
- ✅ Ensures permissions persist across app sessions

#### All Map Fragments
- ✅ Added fallback permission checks
- ✅ Improved error messaging
- ✅ Better handling of permission denial

### 4. Error Logging & Debugging
- ✅ Added detailed logging to identify if errors are from Android permissions or Firestore
- ✅ Better error messages to distinguish between different failure types
- ✅ Logs help identify if the issue is Firestore security rules

## Installation Steps (CRITICAL)

### Step 1: Uninstall Old Version
```bash
# On your device/emulator, completely uninstall the app
adb uninstall com.example.grpassignment
# OR manually uninstall from device settings
```

### Step 2: Install New Version
```bash
# Install the new APK
adb install app/build/outputs/apk/debug/app-debug.apk
# OR use Android Studio's Run button
```

### Step 3: Grant Permissions
When you open the app:
1. You'll be prompted for **Location** permission - **GRANT IT**
2. On Android 12 and below, you may see **Storage** permission - **GRANT IT**
3. The app will remember these permissions for future launches

## If Issue Persists - Firestore Security Rules

If you still see "permission denied" errors after following all steps above, the issue is likely **Firebase Firestore security rules**.

### Check Your Firestore Rules:

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to **Firestore Database** → **Rules**
4. Ensure you have read access to `safety_zones_pin` collection

### Temporary Test Rules (Development Only):
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /safety_zones_pin/{document=**} {
      allow read: if true;  // Allow anyone to read
      allow write: if request.auth != null;
    }
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### Production Rules (Recommended):
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /safety_zones_pin/{document=**} {
      allow read: if request.auth != null;  // Only authenticated users
      allow write: if request.auth != null && request.auth.token.admin == true;
    }
  }
}
```

## Verification Checklist

- [ ] App completely uninstalled before installing new version
- [ ] New APK installed successfully
- [ ] Location permission granted when prompted
- [ ] Storage permission granted (if on Android 12 or below)
- [ ] App opens without crashes
- [ ] Navigate to Safety Map fragment
- [ ] Map tiles load properly
- [ ] Safety zone markers appear on map
- [ ] No "permission denied" errors in logs

## Debugging Commands

If issues persist, collect logs:

```bash
# Clear logcat
adb logcat -c

# Start logging
adb logcat | findstr "FullScreenMapFragment SafetyMapFragment PERMISSION_DENIED"

# Then open the Safety Map fragment and check for errors
```

## What Was Fixed

### Before:
- ❌ Map cache used external storage (required permissions)
- ❌ Permissions not requested at Activity level
- ❌ No distinction between Android and Firestore permission errors
- ❌ Permissions lost after app restart

### After:
- ✅ Map cache uses internal storage (no special permissions needed on Android 13+)
- ✅ Permissions requested upfront in HomeActivity
- ✅ Clear error messages distinguish between different error types
- ✅ Permissions persist across app restarts
- ✅ Better error logging for debugging

## Support

If you continue to see permission errors after following all steps:
1. Check logcat output for specific error messages
2. Verify Firebase Firestore security rules
3. Ensure you granted all requested permissions
4. Try rebooting the device/emulator

---
**Last Updated**: January 6, 2026
**Status**: All fixes applied and tested
