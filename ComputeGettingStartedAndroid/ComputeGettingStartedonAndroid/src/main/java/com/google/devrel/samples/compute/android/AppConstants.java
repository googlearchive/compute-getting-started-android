/* Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
