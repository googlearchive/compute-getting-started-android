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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.compute.ComputeScopes;

import java.io.IOException;

import static com.google.devrel.samples.compute.android.BuildConfig.DEBUG;

/**
 * Display a login screen to select a Google account registered with the Android device. The
 * {@code Activity} ensures the application is authorized for the required OAuth2 scopes prior to
 * forwarding to the next screen.
 *
 * This Android sample code was added to the sample project from the Android Studio examples found
 * in the "File -> New Class -> Android Activity" menu flow. It has been modified to present a login
 * experience that makes use of Google Accounts already registered with the Android device.
 *
 * @author paul.rashidi@google.com (Paul Rashidi)
 */
public class LoginActivity extends Activity {
  private final static String LOG_TAG = "LoginActivity";

  /**
   * Activity result indicating a return from the authorization approval intent.
   */
  private static final int ACTIVITY_RESULT_FROM_REQUEST_AUTH = 1111;

  /**
   * Activity result indicating a return from the Google account selection intent.
   */
  private static final int ACTIVITY_RESULT_FROM_ACCOUNT_SELECTION = 2222;

  /**
   * Tracking variable for the active {@code AsyncTask} to prevent duplicates and allow for
   * cancellation.
   */
  private AuthorizationCheckTask mAuthTask = null;

  private View mLoginFormView;
  private View mLoginStatusView;
  private TextView mLoginStatusMessageView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_login);

    // Prevent the keyboard from being visible upon startup.
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

    mLoginFormView = findViewById(R.id.login_form);
    mLoginStatusView = findViewById(R.id.login_status);
    mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

    // Register the select_account_button to invoke an account chooser dialog.
    findViewById(R.id.select_account_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        // Check for Play Services availability.
        if (!AppUtils.checkGooglePlayServicesAvailability(LoginActivity.this)) {
          return;
        }

        TextView emailTextView = (TextView)LoginActivity.this.findViewById(R.id.email);

        // Check to see how many Google accounts are registered with the device.
        int googleAccounts = AppUtils.countGoogleAccounts(LoginActivity.this);
        if (googleAccounts == 0) {
          // No accounts present, nothing to do.
          Toast.makeText(LoginActivity.this, R.string.toast_no_google_accounts_registered,
              Toast.LENGTH_LONG).show();
        } else if (googleAccounts == 1) {
          // One account is present, select it.
          Toast.makeText(LoginActivity.this, R.string.toast_only_one_google_account_registered,
              Toast.LENGTH_LONG).show();
          AccountManager am = AccountManager.get(LoginActivity.this);
          Account[] accounts = am.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
          if (accounts != null && accounts.length > 0) {
            // Select account and perform authorization check.
            emailTextView.setText(accounts[0].name);
            performAuthCheck();
          }
        } else {
          // More than one Google Account is present, a chooser is necessary.

          // Reset selected account.
          emailTextView.setText("");

          // Disable the verify_authorization_button.
          Button listComputeButton = (Button) LoginActivity.this.findViewById(
              R.id.verify_authorization_button);
          listComputeButton.setEnabled(false);

          // Invoke an {@code Intent} to allow the user to select a Google account.
          Intent accountSelector = AppUtils.getAccountPickerIntent();
          LoginActivity.this.startActivityForResult(accountSelector,
              LoginActivity.ACTIVITY_RESULT_FROM_ACCOUNT_SELECTION);
        }
      }
    });

    // Register a way for the user to verify the application authorization. This will typically not
    // be needed by a user since performAuthCheck() is called at various times, but there are some
    // edge cases when it might be needed (internet connectivity issues).
    findViewById(R.id.verify_authorization_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        performAuthCheck();
      }
    });

    // Register the button that forwards to the Google Compute Engine resources list.
    findViewById(R.id.view_resources_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        invokeResourceListIntent();
      }
    });

    // Load email account used previously.
    String storedEmail = AppUtils.getStoredAccount(this);
    ((TextView) findViewById(R.id.email)).setText(storedEmail);

    // Load project ID used previously with this email account.
    String storedProjectId = AppUtils.getStoredProjectId(this, storedEmail);
    ((TextView) findViewById(R.id.project_id)).setText(storedProjectId);

    if (!Strings.isNullOrEmpty(storedEmail)) {
      // Authorization check for users who have previously selected an account.
      performAuthCheck();
    }

  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == ACTIVITY_RESULT_FROM_REQUEST_AUTH && resultCode == RESULT_OK) {
      // This path indicates that the user successfully authorized the application OAuth2
      // request on their phone. Now we need to kick off our authorization check again.
      performAuthCheck();
    } else if (requestCode == ACTIVITY_RESULT_FROM_ACCOUNT_SELECTION && resultCode == RESULT_OK) {
      // This path indicates the account selection activity resulted in the user selecting a
      // Google account and clicking OK.

      // Set the selected account.
      String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
      TextView emailAccountTextView = (TextView)this.findViewById(R.id.email);
      emailAccountTextView.setText(accountName);

      // Enable the verify_authorization_button
      Button verifyLoginButton = (Button) findViewById(R.id.verify_authorization_button);
      verifyLoginButton.setText(R.string.button_verify_authorization);
      verifyLoginButton.setEnabled(true);

      // Fire off the authorization check for this account and OAuth2 scopes. User might be prompted
      // if they haven't been prompted prior.
      performAuthCheck();
    }
  }

  private String getEmailAccount() {
    TextView emailTextView = (TextView) this.findViewById(R.id.email);
    String emailString = "";
    if (emailTextView.getText()!=null) {
      emailString = emailTextView.getText().toString();
    }
    return Strings.emptyToNull(emailString);
  }

  private String getProjectId() {
    TextView projectIdTextView = (TextView) this.findViewById(R.id.project_id);
    String projectIdString = "";
    if (projectIdTextView.getText()!=null) {
      projectIdString = projectIdTextView.getText().toString();
    }
    return Strings.emptyToNull(projectIdString);
  }

  /**
   * Invoke the activity that will display the Google Compute Engine project details.
   */
  private void invokeResourceListIntent() {
    // Store the email account and project id for the reset of the application and so that they can
    // be restored them to the UI later.
    String configuredEmail = getEmailAccount();
    AppUtils.setStoredAccount(this, configuredEmail);
    String configuredProjectId = getProjectId();
    AppUtils.setStoredProjectId(this, configuredProjectId, configuredEmail);

    // Open Google Compute Engine resource listing.
    Intent intent = new Intent(LoginActivity.this, ItemListActivity.class);
    startActivity(intent);

    if (DEBUG) {
      Log.i(LOG_TAG, "Forwarding to resource listing.");
    }
  }

  /**
   * Schedule the authorization check in an {@code AsyncTask}.
   */
  public void performAuthCheck() {
    if (Strings.isNullOrEmpty(getEmailAccount())) {
      // Email address hasn't been selected so we have nothing to check.
      return;
    }

    // Cancel previously running tasks.
    if (mAuthTask != null) {
      try {
        mAuthTask.cancel(true);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    // Show a progress spinner.
    mLoginStatusMessageView.setText(R.string.text_authenticating);
    showProgress(true);

    // Start task to check authorization.
    mAuthTask = new AuthorizationCheckTask();
    mAuthTask.execute();
  }

  /**
   * Shows the progress UI and hides the login form.
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
  private void showProgress(final boolean show) {
    // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
    // for very easy animations. If available, use these APIs to fade-in
    // the progress spinner.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
      int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

      mLoginStatusView.setVisibility(View.VISIBLE);
      mLoginStatusView.animate().setDuration(shortAnimTime)
              .alpha(show ? 1 : 0)
              .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                  mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
              });

      mLoginFormView.setVisibility(View.VISIBLE);
      mLoginFormView.animate().setDuration(shortAnimTime)
              .alpha(show ? 0 : 1)
              .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                  mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
              });
    } else {
      // The ViewPropertyAnimator APIs are not available, so simply show
      // and hide the relevant UI components.
      mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
      mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
  }

  /**
   * Verifies OAuth2 token access for the application and Google account combination with
   * the {@code AccountManager} and Play Services. If the appropriateOAuth2 access hasn't been
   * granted (to this application) then the task may fire an {@code Intent} to request that the user
   * approve such access. If the appropriate access does exist then the button that will let the
   * user proceed to the next activity is enabled.
   */
  class AuthorizationCheckTask extends AsyncTask<Object, Integer, Boolean> {
    private Intent userRecoverableIntent;
    @Override
    protected Boolean doInBackground(Object... unused) {
      Log.i(LOG_TAG, "Background task started.");

      // Ensure only one task is running at a time.
      mAuthTask = this;

      // Ensure an email was selected.
      String emailAccount = LoginActivity.this.getEmailAccount();
      if (Strings.isNullOrEmpty(emailAccount)) {
        publishProgress(R.string.toast_no_google_account_selected);
        // Failure.
        return false;
      }

      String oauth2 = AppUtils.getOAuth2ScopeString(new String[]{ComputeScopes.COMPUTE,
          ComputeScopes.DEVSTORAGE_READ_ONLY});

      if (DEBUG) {
        Log.d(LOG_TAG, "Attempting to get AuthToken for account: " + getEmailAccount());
      }

      try {
        // If the application has the appropriate access then a token will be retrieved, otherwise
        // an error will be thrown.
        GoogleAuthUtil.getToken(LoginActivity.this, getEmailAccount(), oauth2, null);

        if (DEBUG) {
          Log.d(LOG_TAG, "AuthToken retrieved");
        }

        // Success.
        return true;
      } catch (UserRecoverableAuthException userRecoverableException) {
        Log.w(LOG_TAG, "User recoverable auth exception; firing intent to resolve.",
            userRecoverableException);
        // Invoke the embedded intent to allow the user to resolve the issue causing the exception.
        // The intent is typically the acceptance of an OAuth2 authorization screen for the app.
        userRecoverableIntent =  userRecoverableException.getIntent();
        return false;
      } catch (GoogleAuthException authException) {
        Log.e(LOG_TAG, "Exception checking OAuth2 authentication.", authException);
        publishProgress(R.string.toast_exception_checking_authorization);
        return false;
      } catch (IOException ioException) {
        Log.e(LOG_TAG, "Exception checking OAuth2 authentication.", ioException);
        publishProgress(R.string.toast_exception_checking_authorization);
        return false;
      }
    }

    @Override
    protected void onPreExecute() {
      // Disable the button that can invoke this task.
      Button listComputeButton = (Button) LoginActivity.this.findViewById(
          R.id.view_resources_button);
      listComputeButton.setEnabled(false);
      mAuthTask = this;
    }

    @Override
    protected void onPostExecute(Boolean success) {
      if (success) {
        // Authorization check successful.

        // Enable button to move to the next activity to view teh resources list.
        Button listComputeButton = (Button) LoginActivity.this.findViewById(
            R.id.view_resources_button);
        listComputeButton.setEnabled(true);

        // Disable the verify credentials button since the auth task succeeded.
        Button verifyLogin = (Button) LoginActivity.this.findViewById(
            R.id.verify_authorization_button);
        verifyLogin.setEnabled(false);
        // Also change text on the button to indicate why it is disabled.
        verifyLogin.setText(R.string.button_authorization_verified);

      } else {
        // Authorization check not successful.
        if (userRecoverableIntent != null) {
          // If an intent exists fire it to resolve the issue. onActivityResult will be called once
          // the intent completes, and if the intent completed successfully we will start an
          // instance of this task again to check authorization.
          LoginActivity.this.startActivityForResult(userRecoverableIntent,
              ACTIVITY_RESULT_FROM_REQUEST_AUTH);
        }
      }

      // Stop Activity progress screen.
      showProgress(false);
      mAuthTask = null;
    }

    @Override
    protected void onCancelled() {
      // Stop Activity progress screen.
      showProgress(false);
      mAuthTask = null;
    }
  }
}
