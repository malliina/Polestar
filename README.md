# Polestar

This Android Automotive app:

- Sends the location of your car to www.car-map.com.
- Finds nearby parking places in the Helsinki area.

## Releasing

1. Increment the version name and build number in [build.gradle.kts](automotive/build.gradle.kts).
2. Create a tag vx.y.z where x.y.z matches the version name: `git tag vx.y.z`.
3. Push the code GitHub.
4. Check [.github/workflows/fastlane.yml](.github/workflows/fastlane.yml) for details.
5. Create a new release of the latest build for internal testing in Google Play Console.

## SHA1 fingerprints

To avoid API error 10 DEVELOPER_ERROR:

1. Go to Play Console -> Setup -> App integrity -> App signing.
2. Copy the SHA1 from *App signing key certificate*.
3. Go to Firebase Console -> Project settings -> Select your Android app.
4. Press *Add fingerprint* and paste the SHA1 from step 2.
5. Run Gradle task Tasks -> android -> signingReport.
6. Observe all printed SHA1 fingerprints and add them to the Firebase Console as in step 4.
