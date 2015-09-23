![status: inactive](https://img.shields.io/badge/status-inactive-red.svg)

This project is no longer actively developed or maintained.  

compute-getting-started-android
==========================================

This is sample code for an Android application that displays some Google Compute
Engine(GCE) resources using the GCE API. The sample was created using
Android Studio but should also work with Eclipse as well through maven pom.xml
importing. 

## Products
- [Google Compute Engine][1]

## Language
- [Android][2]

## APIs
- [Google Compute Engine][1]

## Setup Instructions
1. Clone the repo locally.
    `git clone https://github.com/GoogleCloudPlatform/compute-getting-started-android`
1. Import the project:
    1. Open Android Studio and choose "Import Project."
    1. Select the `ComputeGettingStartedAndroid/ComputeGettingStartedonAndroid/build.gradle`
to import and choose the gradle wrapper option. Use default values for the rest
of the import screens.
    1. NOTE: If you recieve an import error indicating that "play-services", a
required dependency, could not be found comment out the Google Play Services
dependency line of build.gradle. This is on line 54 of
`ComputeGettingStartedAndroid/ComputeGettingStartedonAndroid/build.gradle`.
Import the project again. Comment the dependency back in after the following step.

1. Check dependencies (even if the project compiles) using the Android SDK
Manager. Ensure the following packages are installed and up-to-date:
    1. Android Support Repository
    1. Android Support Library
    1. Google Play Services
    1. Google Repository

1. Close and reopen your project. Clean dependencies using the Build menu
by choosing "Rebuild Project."

1. Register an developer project with Google.
your project and application application with Play Services and allow you to retrieve OAuth2 tokens
from Android. Alternatively, it is possible to implement your own OAuth2 flow.
    1. Use the [Cloud Console][8] to create (or reuse) a project for tinkering.
    1. Use the Cloud console to create a new client ID underneath your project
representing Android app. This is done within the API Access section. The
client ID should be of type "Installed Application" type and subtype Android.
Use `com.google.devrel.samples.compute.android` as the package name. Retrieve
your Android debug keystore fingerprint from ADB. On a Mac this command would
typically look like this with an empty password:
    `keytool -list -v -keystore ~/.android/debug.keystore`

1. Deploy the Android App
    1. Deploy your app via normal Android deployment procedures.
    1. You will need to use a version 17 or later "Google APIs" enabled AVD
definition if you are using an emulator instead of a physical device. Physical
devices need only have Google Play installed to work.

[1]: http://developers.google.com/compute
[2]: http://developer.android.com/reference/packages.html
[8]: http://cloud.google.com/console
