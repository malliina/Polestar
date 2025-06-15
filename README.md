# Polestar

This Android Automotive app:

- Sends the location of your car to www.car-map.com.
- Finds nearby parking places in the Helsinki area.

## Development

Decent logcat filter:

     (level:warn & package:com.google.android.apps.automotive.templates.host |  level:info & package:mine)

## Releasing

To release a new version of the app to the Google Play Store internal testing track:

    ./gradlew release

Check [.github/workflows/fastlane.yml](.github/workflows/fastlane.yml) for details.

## SHA1 fingerprints

To avoid API error 10 DEVELOPER_ERROR:

1. Go to Play Console -> Setup -> App integrity -> App signing.
2. Copy the SHA1 from *App signing key certificate*.
3. Go to Firebase Console -> Project settings -> Select your Android app.
4. Press *Add fingerprint* and paste the SHA1 from step 2.
5. Run Gradle task Tasks -> android -> signingReport.
6. Observe all printed SHA1 fingerprints and add them to the Firebase Console as in step 4.
