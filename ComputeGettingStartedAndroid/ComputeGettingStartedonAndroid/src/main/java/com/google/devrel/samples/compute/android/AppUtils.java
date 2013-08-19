package com.google.devrel.samples.compute.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import com.google.common.collect.Lists;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import static com.google.devrel.samples.compute.android.BuildConfig.DEBUG;

/**
 * Utility methods simplifying sample code.
 *
 * @author paul.rashidi@google.com (Paul Rashidi)
 */
public class AppUtils {
  private static final String LOG_TAG = "AppUtils";
  public static final int RESOURCE_LISTING_TOTAL_LIMIT = 100;
  public static final int RESOURCE_LISTING_ITERATION_LIMIT = 25;

  /**
   * Static instance of the JSON factory.
   */
  private static final JsonFactory JSON_FACTORY = new AndroidJsonFactory();

  /**
   * Static instance of the HTTP transport.
   */
  private static HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();

   /**
    * Trim string if it is longer than the specified length.
    */
  public static String trimString(String string, int limit, @Nullable String prependString,
      @Nullable String appendString) {
    StringBuilder returnString = new StringBuilder();
    if (prependString != null) {
      returnString.append(prependString);
    }

    if (string.length() > limit) {
      // Append only the first part of string under the limit.
      returnString.append(string.substring(0, limit));
    } else {
      // Append whole string since it was under the limit.
      returnString.append(string);
    }

    if (appendString != null) {
      returnString.append(appendString);
    }

    return returnString.toString();
  }

  /**
   * Count the Google Accounts on the device.
   */
  public static int countGoogleAccounts(Context context) {
    AccountManager am = AccountManager.get(context);
    Account[] accounts = am.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
    if (accounts == null || accounts.length < 1) {
      return 0;
    } else {
      return accounts.length;
    }
  }

  /**
   * Generate the OAuth2 scope string accepted by the client libraries.
   *
   * @param scopes to be encoded in the OAuth2 string
   * @return OAuth2 scope string
   */
  public static String getOAuth2ScopeString(String[] scopes) {
    if (Array.getLength(scopes) < 1) {
      return null;
    }

    StringBuilder scopeString = null;
    for (String scope : scopes) {
      if (scopeString == null) {
        scopeString = new StringBuilder("oauth2: ").append(scope);
      } else {
        scopeString.append(" ")
                   .append(scope);
      }
    }

    return scopeString.toString();
  }

  /**
   * Generates an {@code Intent} that can invoke an account picker for Google Accounts.
   */
  public static Intent getAccountPickerIntent() {
    return AccountPicker.newChooseAccountIntent(null, null,
        new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, false,
        "Select the account to access Google Compute Engine API.", null, null, null);
  }

  /**
   * Retrieve the account stored for the application.
   *
   * @param context used to access the preferences (usually the Activity)
   * @return stored account name or empty string
   */
  public static String getStoredAccount(Context context) {
    return getStoredProperty(context, AppConstants.PREF_SELECTED_ACCOUNT_EMAIL);
  }

  /**
   * Set the account stored for the application.
   */
  public static void setStoredAccount(Context context, String account) {
    SharedPreferences preferences = context.getSharedPreferences(AppConstants.APP_PREF_NM,
        Context.MODE_PRIVATE);
    preferences.edit().putString(AppConstants.PREF_SELECTED_ACCOUNT_EMAIL, account).commit();
  }

  /**
   * Retrieve the project id stored for the account.
   */
  public static String getStoredProjectId(Context context, String account) {
    return getStoredProperty(context, account + ":" + AppConstants.PREF_SELECTED_PROJECT_ID);
  }

  /**
   * Utility method for retrieving stored properties.
   */
  private static String getStoredProperty(Context context, String propertyName) {
    SharedPreferences preferences = context.getSharedPreferences(AppConstants.APP_PREF_NM,
        Context.MODE_PRIVATE);
    if (preferences == null) {
      return "";
    }
    return preferences.getString(propertyName, "");
  }

  /**
   * Set the project id stored for the account.
   */
  public static void setStoredProjectId(Context context, String projectId, String account) {
    SharedPreferences preferences = context.getSharedPreferences(AppConstants.APP_PREF_NM,
        Context.MODE_PRIVATE);
    preferences.edit().putString(account + ":" + AppConstants.PREF_SELECTED_PROJECT_ID, projectId)
        .commit();
  }

  public static Date convertDateTime(String dateTimeString) {
    SimpleDateFormat simpleDateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    Date newDate = null;
    try {
      if (DEBUG) {
        newDate = simpleDateFormatter.parse(dateTimeString);
        Log.v(LOG_TAG, newDate + "");
      }
    } catch (ParseException parseException) {
      Log.e(LOG_TAG, "Date parsing exception", parseException);
    }
    return newDate;
  }

  public static Compute getComputeServiceObject(Context context, String emailAddress) {
    List scopes = Lists.newArrayList(ComputeScopes.COMPUTE, ComputeScopes.DEVSTORAGE_READ_ONLY);

    // Utilize the Android credential type. This will give you problems if you haven't
    // registered the android application within the developer console (see README file).
    GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(context, scopes);
    // Tell the credential which Google account(email) to use.
    credential.setSelectedAccountName(emailAddress);

    // Create Google Compute Engine API query object.
    return new Compute.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
        .setApplicationName(AppConstants.COMPUTE_ENGINE_ANDROID_SAMPLE_APP_NAME).build();
  }

  public static final String getNameFromSelfLink(String selfLink) {
    Pattern p = Pattern.compile("([^/]+$)");
    Matcher m = p.matcher(selfLink);
    String name = null;
    if (m.find()) {
      name = m.group(1);
    }
    return name;
  }
}
