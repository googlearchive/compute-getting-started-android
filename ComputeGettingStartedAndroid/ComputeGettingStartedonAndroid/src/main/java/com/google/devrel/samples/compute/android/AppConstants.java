package com.google.devrel.samples.compute.android;

/**
 * Application constants.
 *
 * @author paul.rashidi@google.com (Paul Rashidi)
 */
public final class AppConstants {
  /**
   * Application preferences name.
   */
  public static final String APP_PREF_NM = "ComputeEngineSamplePreferences";
  /**
   * Preference name for the last used Google account.
   */
  public static final String PREF_SELECTED_ACCOUNT_EMAIL = "AccountEmail";
  /**
   * Preference name prefix for the project ID last used with a Google account. The Google account
   * name will be appended to this preference value allowing a project ID to be stored for each
   * Google account that is used with the application.
   */
  public static final String PREF_SELECTED_PROJECT_ID_SUFFIX = "ProjectId";
  public static final String DATE_TIME_FORMAT_STRING = "EEE, MMM d yyyy @hh:mma z";
  public static final String COMPUTE_ENGINE_ANDROID_SAMPLE_APP_NAME = "ComputeEngineAndroidSampleApp/0.1";
  /**
   * Constant indicating the ID of the Google Play Services dialog.
   */
  public static final int PLAY_SERVICES_ERROR_DIALOG = 1234;

}
