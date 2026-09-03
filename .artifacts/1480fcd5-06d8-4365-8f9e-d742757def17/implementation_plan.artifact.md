# Fix Missing Internet Permission for Release Build

The application fails to fetch JSON data in the release version because the `INTERNET` permission is missing from the main Android manifest. Flutter includes this permission by default only for `debug` and `profile` build configurations to support development tools. Since the app performs network requests, it must explicitly declare this permission for production use.

## Proposed Changes

### Android Configuration

#### [MODIFY] [AndroidManifest.xml](file:///home/soilad/AndroidStudioProjects/vitap_menu_2/android/app/src/main/AndroidManifest.xml)
- Add `<uses-permission android:name="android.permission.INTERNET"/>` to allow the app to access the internet in release mode.

## Verification Plan

### Manual Verification
- Run the app in release mode using `flutter run --release` (or build a release APK/Bundle).
- Verify that the menu data is fetched and displayed correctly on the home screen.
