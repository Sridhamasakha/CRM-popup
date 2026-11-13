# HKM Caller

Small Android companion app for the HKM CRM. When a call starts or rings, it asks the CRM
whether the number belongs to a donor or a lead, and shows a heads-up notification with the
name, category, last interaction and any open opportunity. When the call ends, matched calls
are logged back as an interaction. Numbers that are not in the CRM are ignored and never logged.

## Build

Option A, GitHub Actions, no Android Studio needed:
1. Create an empty repository on github.com.
2. Upload the contents of this folder to it, keeping the folders intact.
3. Open the Actions tab, run Build HKM Caller APK.
4. Download the artifact, unzip it, and install the apk on the phone, allowing install from unknown sources.

Option B, Android Studio:
1. Open this folder as an existing project.
2. Build, Build Bundle or APK, Build APK.
3. Install on the phone.

## First run on the phone

1. Open the app, allow phone state, call log and notification permissions.
2. Paste the device key from the CRM, and your CRM login email.
3. Tap Save and test. It should say the test number matched a contact.
4. Leave Watch calls switched on, and allow the app to run in the background, exempt it from battery optimisation.

## What it never does

It does not record calls, it does not read messages, and it sends a number to the CRM only to ask who it is.
Numbers not found in the CRM are discarded.
