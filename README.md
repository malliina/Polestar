# Polestar

Exploring Android Automotive development.

## SHA1 fingerprints

To avoid API error 10 DEVELOPER_ERROR:

1. Go to Play Console -> Setup -> App integrity -> App signing.
2. Copy the SHA1 from *App signing key certificate*.
3. Go to Firebase Console -> Project settings -> Select your Android app.
4. Press *Add fingerprint* and paste the SHA1 from step 2.
5. Run Gradle task Tasks -> android -> signingReport.
6. Observe all printed SHA1 fingerprints and add them to the Firebase Console as in step 4.
