
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

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeRequest;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Zone;
import com.google.devrel.samples.compute.android.dummy.DummyContent;
import com.google.devrel.samples.compute.android.dummy.DummyContent.DiskItem;
import com.google.devrel.samples.compute.android.dummy.DummyContent.InstanceItem;
import com.google.devrel.samples.compute.android.dummy.DummyContent.ZoneItem;
import com.google.devrel.samples.compute.android.tasks.ZoneOperationParameters;
import com.google.devrel.samples.compute.android.tasks.ZoneOperationTask;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.google.devrel.samples.compute.android.BuildConfig.DEBUG;

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a {@link ItemListActivity}
 * in two-pane mode (on tablets) or a {@link ItemDetailActivity}
 * on handsets.
 * <p>
 * This Android sample code has been modified to display Google Compute Engine resource data
 * that is stored in extended {@code DummyContent} objects. When displaying some types of resources
 * a button has also been added that invokes a {@code ZoneOperationTask} to delete the resources in
 * a background {@code AsyncTask}.
 *
 * @author paul.rashidi@google.com (Paul Rashidi)
 */
public class ItemDetailFragment extends Fragment {
  private static String LOG_TAG = "ItemDetailFragment";

  /**
   * The fragment argument representing the item ID that this fragment represents.
   */
  public static final String ARG_ITEM_ID = "item_id";

  /**
   * The dummy content this fragment is presenting.
   */
  private DummyContent.DummyItem mItem;

  private String mGoogleAccount;
  private String mProjectId;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public ItemDetailFragment() {
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.w(LOG_TAG, "onCreate called");
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    Log.w(LOG_TAG, "onCreateView called");

    View rootView = null;

    // Retrieve account and project ID from application preferences.
    Context context = this.getActivity();
    mGoogleAccount = AppUtils.getStoredAccount(context);
    mProjectId = AppUtils.getStoredProjectId(context, mGoogleAccount);

    if (getArguments().containsKey(ARG_ITEM_ID)) {
      // Load the dummy content specified by the fragment
      // arguments. In a real-world scenario, use a Loader
      // to load content from a content provider.

      String argumentId = getArguments().getString(ARG_ITEM_ID);

      Log.w(LOG_TAG, "ID being displayed: " + argumentId);

      // Pull DummyItem from storage.
      DummyContent.DummyItem dummyItem = DummyContent.ITEM_MAP.get(argumentId);

      // Display data if DummyItem was found.
      if (dummyItem != null) {
        mItem = dummyItem;
        if (DEBUG) {
          Log.v(LOG_TAG, "Creating view for " + dummyItem.toString());
        }
        if (mItem instanceof DummyContent.ZoneItem) {
          rootView = inflater.inflate(R.layout.fragment_item_detail_zone, container, false);
          displayZoneInformation(rootView, (ZoneItem) mItem);
        } else if (mItem instanceof InstanceItem) {
          rootView = inflater.inflate(R.layout.fragment_item_detail_instance, container, false);
          displayInstanceInformation(rootView, (InstanceItem) mItem);
        } else if (mItem instanceof DiskItem) {
          rootView = inflater.inflate(R.layout.fragment_item_detail_disk, container, false);
          displayDiskInformation(rootView, (DiskItem) mItem);
        } else {
          rootView = inflater.inflate(R.layout.fragment_item_detail, container, false);
          // Unknown type, nothing to display.
        }
      }
    }

    return rootView;
  }

  private void displayZoneInformation(View rootView, ZoneItem zoneItem) {
    Zone zone = zoneItem.zone;

    // Create next maintenance window text.
    StringBuilder maintWindowTextString = new StringBuilder();
    if (zoneItem.zone.getMaintenanceWindows() != null
        && zoneItem.zone.getMaintenanceWindows().size() > 0) {
      for (Zone.MaintenanceWindows nextMaintWindow : zoneItem.zone.getMaintenanceWindows()) {
        java.text.DateFormat dateFormatter = new SimpleDateFormat(
            AppConstants.DATE_TIME_FORMAT_STRING);

        String beginString = dateFormatter.format(
            AppUtils.convertDateTime(nextMaintWindow.getBeginTime()));
        String endString = dateFormatter.format(
            AppUtils.convertDateTime(nextMaintWindow.getEndTime()));
        if (maintWindowTextString.length()!=0) {
          maintWindowTextString.append("\n");
        }
        maintWindowTextString.append(beginString);
        maintWindowTextString.append("\nto\n");
        maintWindowTextString.append(endString);
      }
    } else {
      maintWindowTextString.append("N/A");
    }

    // Assign data to the appropriate TextViews.
    setTextViewText(rootView, R.id.item_detail_name_tv, zone.getName());
    setTextViewText(rootView, R.id.item_detail_status_tv, zone.getStatus());
    setTextViewText(rootView, R.id.item_detail_outage_tv, maintWindowTextString.toString());
  }

  private void displayInstanceInformation(View rootView, InstanceItem instanceItem) {
    Instance instance = instanceItem.instance;

    java.text.DateFormat dateFormatter = new SimpleDateFormat(
        AppConstants.DATE_TIME_FORMAT_STRING);

    // Assign instance data to the appropriate TextViews.
    setTextViewText(rootView, R.id.item_detail_name_tv, instance.getName());
    setTextViewText(rootView, R.id.item_detail_description_tv, instance.getDescription());
    setTextViewText(rootView, R.id.item_detail_status_tv, instance.getStatus());
    setTextViewText(rootView, R.id.item_detail_machine_type_tv,
        AppUtils.getNameFromSelfLink(instance.getMachineType()));
    setTextViewText(rootView, R.id.item_detail_zone_tv,
        AppUtils.getNameFromSelfLink(instance.getZone()));

    // Compose the creation time display string and populate it into the view.
    Date creationDateTime = AppUtils.convertDateTime(instance.getCreationTimestamp());
    String createTimeString = dateFormatter.format(creationDateTime);

    // Append a simple up-time human readable string to the create time.
    createTimeString = createTimeString + "\n" + generateDaysAgoString(creationDateTime.getTime());
    setTextViewText(rootView, R.id.item_detail_creation_time_tv, createTimeString);

    // Build a string to display the associated tags for the Instance then populate into view.
    StringBuilder tagsString = new StringBuilder();
    if (instance.getTags() != null && instance.getTags().getItems() != null) {
      for (String tag : instance.getTags().getItems()) {
        if (tagsString.length() > 0) {
          // Process iterations 2-N.
          tagsString.append("\n");
          tagsString.append(tag);
        } else {
          // Process iteration 1.
          tagsString.append(tag);
        }
      }
    }
    setTextViewText(rootView, R.id.item_detail_tags_tv, tagsString.toString());

    // Enable the delete button.
    Button deleteButton = ((Button) rootView.findViewById(R.id.item_detail_delete_button));
    deleteButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        view.setEnabled(false);
        Toast.makeText(view.getContext(), R.string.toast_deleting_instance_dot_dot,
            Toast.LENGTH_LONG).show();
        Compute compute = AppUtils.getComputeServiceObject(getActivity(), mGoogleAccount);
        Instance instance = ((InstanceItem) ItemDetailFragment.this.mItem).instance;

        // Create and execute the delete operation.
        Compute.Instances.Delete deleteCommand;
        try {
          String zoneName = AppUtils.getNameFromSelfLink(instance.getZone());
          deleteCommand = compute.instances().delete(mProjectId, zoneName, instance.getName());
          executeZoneOperation(zoneName, deleteCommand);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
  }

  private void displayDiskInformation(View rootView, DiskItem diskItem) {
    Disk disk = diskItem.disk;
    java.text.DateFormat dateFormatter = new SimpleDateFormat(AppConstants.DATE_TIME_FORMAT_STRING);

    // Assign instance data to the appropriate TextViews.
    setTextViewText(rootView, R.id.item_detail_name_tv, disk.getName());
    setTextViewText(rootView, R.id.item_detail_description_tv, disk.getDescription());
    setTextViewText(rootView, R.id.item_detail_status_tv, disk.getStatus());
    setTextViewText(rootView, R.id.item_detail_size_tv, disk.getSizeGb() + "");
    setTextViewText(rootView, R.id.item_detail_zone_tv,
        AppUtils.getNameFromSelfLink(disk.getZone()));

    // Compose the creation time display string and populate it into the view.
    Date creationDateTime = AppUtils.convertDateTime(disk.getCreationTimestamp());
    String createTimeString = dateFormatter.format(creationDateTime);

    // Append a simple up-time human readable string to the create time.
    createTimeString = createTimeString + "\n" + generateDaysAgoString(creationDateTime.getTime());
    setTextViewText(rootView, R.id.item_detail_creation_time_tv, createTimeString);

    // Enable the delete button.
    Button deleteButton = ((Button) rootView.findViewById(R.id.item_detail_delete_button));
    deleteButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        view.setEnabled(false);
        Toast.makeText(view.getContext(), R.string.toast_deleting_instance_dot_dot,
            Toast.LENGTH_LONG).show();
        Compute compute = AppUtils.getComputeServiceObject(getActivity(), mGoogleAccount);
        Disk disk = ((DiskItem) ItemDetailFragment.this.mItem).disk;

        // Create and execute the delete operation.
        Compute.Instances.Delete deleteCommand;
        try {
          String zoneName = AppUtils.getNameFromSelfLink(disk.getZone());
          deleteCommand = compute.instances().delete(mProjectId, zoneName, disk.getName());
          executeZoneOperation(zoneName, deleteCommand);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
  }

  private void executeZoneOperation(String zoneName, ComputeRequest command) {
    ZoneOperationTask task = new ZoneOperationTask(getActivity(), mGoogleAccount, mProjectId);
    ZoneOperationParameters deleteResourceOperation = new ZoneOperationParameters();
    deleteResourceOperation.zone = zoneName;
    deleteResourceOperation.computeOperation = command;
    task.execute(deleteResourceOperation);
  }

  private static void setTextViewText(View rootView, int viewId, String text) {
    TextView textView = (TextView)rootView.findViewById(viewId);
    textView.setText(text);
  }

  private static String generateDaysAgoString(long timeInMillis) {
    long durationMillis = System.currentTimeMillis() - timeInMillis;
    long durationDays = durationMillis / 1000 / 60 / 60 /24;
    if (durationDays == 1) {
      return "1 day ago";
    } else {
      return durationDays + " days ago";
    }
  }
}
