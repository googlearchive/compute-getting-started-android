package com.google.devrel.samples.compute.android.tasks;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.ZoneOperations.Get;
import com.google.api.services.compute.model.Operation;
import com.google.common.collect.Sets;
import com.google.devrel.samples.compute.android.AppUtils;
import com.google.devrel.samples.compute.android.R;

import java.io.IOException;
import java.util.Set;

/**
 * Run a Google Compute Engine Zone operation and monitor the status of the operation. Toast
 * messages periodically popup regarding the operation status as well as upon completion.
 *
 * @author paul.rashidi@google.com (Paul Rashidi)
 */
public class ZoneOperationTask extends ComputeTask<ZoneOperationParameters, Integer, Boolean> {
  private static String LOG_TAG = "ZoneOperationTask";

  private final static Set<String> OPERATION_STATUS_SUCCESS_STRINGS =
      Sets.newHashSet("DONE");
  private final static Set<String> OPERATION_STATUS_IN_PROGRESS_STRINGS =
      Sets.newHashSet("RUNNING", "PENDING");
  private final static Set<String> OPERATION_STATUS_FAILURE_STRINGS =
      Sets.newHashSet("ERROR");

  private final Activity mParentActivity;
  private final String mEmailAccount;
  private final String mProjectId;

  public ZoneOperationTask(Activity parentActivity, String emailAccount, String projectId) {
    mParentActivity = parentActivity;
    mEmailAccount = emailAccount;
    mProjectId = projectId;
  }

  protected Boolean doInBackground(ZoneOperationParameters... params) {
    Log.i(LOG_TAG, "Background task started.");
    boolean operationSucceeded = false;
    Integer userMessage;

    if (params==null || params.length<1) {
      Log.e(LOG_TAG, "No parameters passed in.");
      return false;
    }

    ZoneOperationParameters operationParam = params[0];

    try {
      // Retrieve reference to the Compute Engine API.
      Compute compute = AppUtils.getComputeServiceObject(mParentActivity, mEmailAccount);

      // Send the operation on the server.
      Operation deleteInstanceOperation = (Operation) operationParam.computeOperation.execute();

      // Extract the Zone name from the fully qualified Zone self link.
      String zoneName = AppUtils.getNameFromSelfLink(operationParam.zone);

      boolean continueCheckingStatus;
      // Fail safe counter used to ensure the do-while loop will not continue forever on an
      // unexpected condition.
      int loopCount = 0;

      // Polling loop to server until the operation completes.
      do {
        loopCount++;

        if (loopCount>1) {
          // Exponential back-off for subsequent runs.
          Thread.sleep(2000L ^ loopCount);
        }

        Log.v(LOG_TAG, "Querying on operation named: " + deleteInstanceOperation.getName());

        // Create the command to re-retrieve the operation that sent to the server.
        Get listDeleteOperation = compute.zoneOperations().get(mProjectId,
            zoneName, deleteInstanceOperation.getName());

        // Execute the command.
        Operation deleteOperationStatus = listDeleteOperation.execute();

        // Extract the Zone operation status.
        String operationStatus = deleteOperationStatus.getStatus();

        Log.v(LOG_TAG, "Delete operation status:" + deleteOperationStatus.getStatus());

        if (OPERATION_STATUS_IN_PROGRESS_STRINGS.contains(operationStatus)) {
          userMessage = R.string.toast_operation_still_running;
          continueCheckingStatus = true;
        } else {
          // Operation doesn't appear to be running any longer.
          continueCheckingStatus = false;
          if (OPERATION_STATUS_FAILURE_STRINGS.contains(operationStatus)) {
            userMessage = R.string.toast_operation_failed;
          } else if (OPERATION_STATUS_SUCCESS_STRINGS.contains(operationStatus)) {
            userMessage = R.string.toast_operation_succeeded;
            operationSucceeded = true;
          } else {
            userMessage = R.string.toast_operation_returned_unknown_status;
            Log.e(LOG_TAG, "Unknown status was returned: " + operationStatus);
          }
        }
        publishProgress(userMessage);
      } while (loopCount < 8 && continueCheckingStatus);

      Log.i(LOG_TAG, "Background task completed loading instance information");
    } catch (IOException e) {
      operationSucceeded = false;
      Log.e(LOG_TAG, e.getMessage(), e);
    } catch (InterruptedException e) {
      operationSucceeded = false;
      Log.e(LOG_TAG, e.getMessage(), e);
    }

    return operationSucceeded;
  }

  @Override
  protected void onProgressUpdate(Integer... userMessages) {
    super.onProgressUpdate(userMessages);

    if (userMessages==null || userMessages.length<1) {
      return;
    }

    // Toast the latest message.
    Integer userMessage = userMessages[userMessages.length-1];
    if (userMessage != null) {
      Toast message = Toast.makeText(mParentActivity, userMessage, Toast.LENGTH_SHORT);
      message.show();
    }
  }

  @Override
  protected void onPostExecute(Boolean result) {
    Log.i(LOG_TAG, "Operation result: " + result);

    mParentActivity.finish();
  }
}