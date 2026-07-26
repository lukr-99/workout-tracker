# Android release signing

Release builds are unsigned when no signing configuration is present, which keeps CI safe. To
create a local upload key, run this yourself and keep the generated file outside version control:

```powershell
keytool -genkeypair -v -keystore workout-tracker-upload.jks -alias workout-tracker `
  -keyalg RSA -keysize 2048 -validity 10000
```

Create a git-ignored `keystore.properties` at the repository root:

```properties
storeFile=C:/absolute/path/to/workout-tracker-upload.jks
storePassword=...
keyAlias=workout-tracker
keyPassword=...
```

CI may instead supply `KEYSTORE_STORE_FILE`, `KEYSTORE_STORE_PASSWORD`, `KEYSTORE_KEY_ALIAS`, and
`KEYSTORE_KEY_PASSWORD`. Never commit the keystore, this properties file, or any passwords.
