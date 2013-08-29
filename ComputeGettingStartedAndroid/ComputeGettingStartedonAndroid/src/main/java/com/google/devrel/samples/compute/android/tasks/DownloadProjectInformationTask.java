package com.google.devrel.samples.compute.android.tasks;

import android.app.Activity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Disks;
import com.google.api.services.compute.Compute.Instances.AggregatedList;
import com.google.api.services.compute.Compute.Zones;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.DiskAggregatedList;
import com.google.api.services.compute.model.DisksScopedList;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceAggregatedList;
import com.google.api.services.compute.model.InstancesScopedList;
import com.google.api.services.compute.model.Zone;
import com.google.api.services.compute.model.ZoneList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.devrel.samples.compute.android.AppUtils;
import com.google.devrel.samples.compute.android.BuildConfig;
import com.google.devrel.samples.compute.android.dummy.DummyContent;
import com.google.devrel.samples.compute.android.dummy.DummyContent.DiskItem;
import com.google.devrel.samples.compute.android.dummy.DummyContent.DummyHeader;
import com.google.devrel.samples.compute.android.dummy.DummyContent.DummyItem;
import com.google.devrel.samples.compute.android.dummy.DummyContent.InstanceItem;
import com.google.devrel.samples.compute.android.dummy.DummyContent.ZoneItem;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Download Zone and Instance lists for a Google Compute Engine project id in a background task.
 * Post data to the static {@code DummyContent} data store once downloaded.
 *
 * TODO(developer): Replace {@code DummyContent} with a {@code ContentProvider} implementation.
 *
 * @author paulrashidi@google.com (Paul Rashidi)
 */
public class DownloadProjectInformationTask extends ComputeTask<Object, Integer, List<DummyItem>> {
  private static String LOG_TAG = "ProjectInfoTask";

  private final Activity mParentActivity;
  private final String mEmailAccount;
  private final String mProjectId;
  private final ArrayAdapter<DummyItem> mResourcesAdapter;

  public DownloadProjectInformationTask(Activity parentActivity, String emailAccount,
      String projectId, ArrayAdapter<DummyItem> resourcesAdapter) {
    mParentActivity = parentActivity;
    mEmailAccount = emailAccount;
    mProjectId = projectId;
    mResourcesAdapter = resourcesAdapter;
  }

  protected List<DummyItem> doInBackground(Object... unused) {
    Log.i(LOG_TAG, "Background task started.");

    // Maintain a hard cap on the number of resources we will report on.
    long resourceMaxLimit = AppUtils.RESOURCE_LISTING_TOTAL_LIMIT;
    // Maintain a limit on the number of resources returned per API call.
    long perPageResourceLimit = AppUtils.RESOURCE_LISTING_ITERATION_LIMIT;
    // Maintain a list of resources currently in memory.
    long resourceCount = 0L;

    // Local storage of information we are downloading.
    List<DummyItem> downloadedData = Lists.newArrayList();
    Compute compute = AppUtils.getComputeServiceObject(mParentActivity, mEmailAccount);

    // Collect a list of Zones available for this Project.
    HashMap<String, ZoneItem> zoneMap = Maps.newHashMap();
    try {
      // Create Zone List Operation.
      Zones.List zoneListCommand = compute.zones().list(mProjectId);

      // Limit response to only the fields needed to save bandwidth (http://goo.gl/SEOu5).
      zoneListCommand.setFields("items(name,id,maintenanceWindows,selfLink,status)");

      // Execute the operation.
      ZoneList zoneList = zoneListCommand.execute();

      // Load Zones into the map.
      for (Zone zone : zoneList.getItems()) {
        zoneMap.put(zone.getName(), new ZoneItem(zone));
        if (BuildConfig.DEBUG) {
          Log.v(LOG_TAG, zone.toPrettyString());
        }
      }

      Log.i(LOG_TAG, "Background task completed collecting Zone information.");

      InstanceAggregatedList aggInstanceList;

      // Create aggregated instance list operation.
      AggregatedList aggInstanceListOperation = compute.instances().aggregatedList(mProjectId);
      aggInstanceListOperation.setProject(mProjectId);

      // Limit the response to a reasonable number of instances.
      aggInstanceListOperation.setMaxResults(perPageResourceLimit);

      // Add section header for this content.
      downloadedData.add(new DummyHeader("Instances"));

      do {
        // Execute list operation.
        aggInstanceList = aggInstanceListOperation.execute();
        for (InstancesScopedList zonedScopedInstanceList : aggInstanceList.getItems().values()) {
          if (zonedScopedInstanceList.getInstances() != null) {
            for (Instance instance : zonedScopedInstanceList.getInstances()) {
              if (BuildConfig.DEBUG) {
                Log.v(LOG_TAG, instance.toPrettyString());
              }
              String zoneName = AppUtils.getNameFromSelfLink(instance.getZone());
              ZoneItem zoneItem = zoneMap.get(zoneName);
              downloadedData.add(new InstanceItem(instance, zoneItem));
              resourceCount++;
            }
          }
        }

        // Get ready to process a second page of results if they exist.
        if (aggInstanceList.getNextPageToken() != null) {
          aggInstanceListOperation.setPageToken(aggInstanceList.getNextPageToken());
        }
      } while (aggInstanceList.getNextPageToken() != null && resourceCount < resourceMaxLimit);

      Log.i(LOG_TAG, "Background task completed loading instance information");

      // Create aggregated instance list operation.
      DiskAggregatedList aggDiskList;
      Disks.AggregatedList aggDiskListOperation = compute.disks().aggregatedList(mProjectId);
      // Limit the response to a reasonable number of instances.
      aggDiskListOperation.setMaxResults(perPageResourceLimit);
      aggDiskListOperation.setProject(mProjectId);

      downloadedData.add(new DummyHeader("Disks"));

      do {

        // Execute list operation.
        aggDiskList = aggDiskListOperation.execute();

        if (BuildConfig.DEBUG) {
          // Iterate through lists printing out information on a per Zone basis.
          String scopeId = aggDiskList.getId();
          String scopeKind = aggDiskList.getKind();
          if (BuildConfig.DEBUG) {
            Log.v(LOG_TAG, "Scope id: " + scopeId + " and kind: " + scopeKind);
          }
          for (DisksScopedList instanceScopedDiskList :
                  aggDiskList.getItems().values()) {
            if (instanceScopedDiskList.getDisks() != null) {
              for (Disk disk : instanceScopedDiskList.getDisks()) {
                if (BuildConfig.DEBUG) {
                  Log.v(LOG_TAG, disk.toPrettyString());
                }
                ZoneItem zoneItem = zoneMap.get(AppUtils.getNameFromSelfLink(
                    disk.getZone()));
                downloadedData.add(new DiskItem(disk, zoneItem));
                resourceCount++;
              }
            }
          }
        }

        // Get ready to process a second page of results if they exist.
        if (aggDiskList.getNextPageToken() != null) {
          aggDiskListOperation.setPageToken(aggDiskList.getNextPageToken());
        }
      } while (aggDiskList.getNextPageToken() != null && resourceCount < resourceMaxLimit);
    } catch (IOException e) {
      Log.e(LOG_TAG, "Exception downloading project information", e);
      return null;
    }

    Log.i(LOG_TAG, "Background task completed loading instance information");

    // Add Zones to the dummy data list.
    // Placeholder for a section header.
    downloadedData.add(new DummyHeader("Zones"));
    downloadedData.addAll(zoneMap.values());

    return downloadedData;
  }

  @Override
  protected void onPreExecute() {
    // Clear data that will be loaded by this task.
    DummyContent.clear();
  }

  @Override
  protected void onProgressUpdate(Integer... userMessagesResourceIds) {
    super.onProgressUpdate(userMessagesResourceIds);

    // Alternatively, we could have loaded DummyContent here instead of in onPostExecute() for a
    // more responsive UI.

    if (userMessagesResourceIds==null || userMessagesResourceIds.length<1) {
      return;
    }

    // Select the latest message.
    Integer userMessage = userMessagesResourceIds[userMessagesResourceIds.length-1];

    // Toast.
    Toast.makeText(mParentActivity, userMessage, Toast.LENGTH_SHORT).show();
  }

  @Override
  protected void onPostExecute(List<DummyItem> result) {
    // Load data downloaded from this task.
    if (result==null || result.size() < 1) {
      Log.d(LOG_TAG, "Downloaded Data result was empty");
      return;
    }

    Log.d(LOG_TAG, "Downloaded Data result size: " + result.size());

    for (DummyItem item : result) {
      DummyContent.addContent(item);
    }
    // Notify adapter of change so that {@code ListView} will update itself.
    mResourcesAdapter.notifyDataSetChanged();
  }
}