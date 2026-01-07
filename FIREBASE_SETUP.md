# Firebase Setup Instructions

## 1. Enable Anonymous Authentication

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Click **Authentication** in the left menu
4. Click the **Sign-in method** tab
5. Find **Anonymous** in the list and click on it
6. Toggle **Enable** to ON
7. Click **Save**

## 2. Update Firestore Security Rules

1. In Firebase Console, click **Firestore Database** in the left menu
2. Click the **Rules** tab
3. Replace the existing rules with the following:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Allow authenticated users (including anonymous) to create and manage their own alerts
    match /alerts/{alertId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && request.resource.data.initiatorId == request.auth.uid;
      allow update: if request.auth != null && resource.data.initiatorId == request.auth.uid;
      allow delete: if request.auth != null && resource.data.initiatorId == request.auth.uid;
    }
    
    // Safety zones - allow authenticated users to read and write
    match /safety_zones_pin/{document=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
    
    // Safety resources - allow authenticated users to read
    match /safety_resource/{document=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null; // Adjust based on who should be able to add resources
    }
    
    // User-specific collections (e.g., trusted contacts)
    match /user/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Community reports - allow authenticated users to read and write
    match /community_reports/{document=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

4. Click **Publish** to apply the rules

## 3. How It Works

### Authentication Flow

1. **Email/Password Login**: Users sign in normally via LogInPage
2. **Anonymous Fallback**: If no user is logged in when HomeFragment loads, the app automatically signs in anonymously
3. **SOS Feature**: Works with both regular authenticated users and anonymous users

### What Anonymous Auth Provides

- Allows the SOS feature to work even if there's a temporary authentication issue
- Creates a unique user ID that can be used to write to Firestore
- Can be upgraded to a full account later if needed

### When Users Are Authenticated

- ✅ **Logged in via email/password**: Uses their email account
- ✅ **Anonymous**: Uses temporary anonymous account
- ❌ **Not authenticated**: Auto-signs in anonymously as fallback

## 4. Testing

After enabling anonymous auth and updating Firestore rules:

1. Restart the app
2. Log in with email/password
3. Press and hold the SOS button for 3 seconds
4. You should see:
   - ✅ Phone call initiated to trusted contact
   - ✅ "SOS alert created in Firestore" toast message
   - ✅ SMS messages sent with location
   - ✅ No "permission denied" or "error saving alert" errors

## 5. Troubleshooting

### Still Getting "Error Saving Alert"?

1. Check the Logcat output for specific error messages
2. Verify anonymous authentication is enabled in Firebase Console
3. Ensure Firestore rules are published
4. Check that the app has location permissions granted

### Need to See Firestore Rules Errors?

Look for logs with these tags:
- `HomeFragment`: Shows authentication and alert saving status
- `FirebaseFirestore`: Shows Firestore permission denied errors

### Common Issues

**Issue**: "User not signed in, cannot save alert"
- **Solution**: Enable anonymous authentication in Firebase Console

**Issue**: "PERMISSION_DENIED: Missing or insufficient permissions"
- **Solution**: Update Firestore security rules as shown above

**Issue**: Location is null when saving alert
- **Solution**: Ensure location permissions are granted and GPS is enabled on device
