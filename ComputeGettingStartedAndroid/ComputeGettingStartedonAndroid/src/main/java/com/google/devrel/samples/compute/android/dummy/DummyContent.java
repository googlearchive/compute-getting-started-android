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

package com.google.devrel.samples.compute.android.dummy;

import android.util.Log;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Zone;
import com.google.api.services.compute.model.Zone.MaintenanceWindows;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.devrel.samples.compute.android.AppUtils;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.google.devrel.samples.compute.android.BuildConfig.DEBUG;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p/>
 * TODO(developer): Replace all uses of this class before publishing your app.
 *
 * This Android sample code has been modified to allow storage of Google Compute Engine resource
 * data. Before publishing this App you MUST refactor you data storage technique;
 * {@code ContentProviders} are always a great choice.
 */
public class DummyContent {
  private static final String LOG_TAG = "DummyContent";

  /**
   * A list of sample (dummy) items.
   */
  public static List<DummyItem> ITEMS = Lists.newArrayList();

  /**
   * A map of sample (dummy) items, by ID.
   */
  public static Map<String, DummyItem> ITEM_MAP = Maps.newConcurrentMap();

  public static void clear() {
    ITEM_MAP.clear();
    ITEMS.clear();
  }

  public static void addContent(DummyItem dummyItem) {
    ITEMS.add(dummyItem);
    ITEM_MAP.put(dummyItem.id, dummyItem);
  }

  /**
   * A dummy item representing a piece of content.
   */
  public static class DummyItem {
    public String id;
    public String content;

    private DummyItem(String id, String content) {
      this.id = id;
      this.content = content;
    }

    @Override
    public String toString() {
      return content;
    }
  }

  /**
   * A dummy item representing a Google Compute Engine Instance.
   */
  public static class InstanceItem extends DummyItem {
    public Instance instance = null;
    public String userMessage = null;

    public InstanceItem(Instance instance, ZoneItem zoneItem) {
      super(instance.getSelfLink(), instance.getName());
      this.instance = instance;

      // Instances should display their Zone's user message.
      if (!Strings.isNullOrEmpty(zoneItem.userMessage)) {
        userMessage = "Zone activity: " + zoneItem.userMessage;
      }
    }
  }

  /**
   * A dummy item representing a Google Compute Engine Disk.
   */
  public static class DiskItem extends DummyItem {
    public Disk disk = null;
    public String userMessage = null;

    public DiskItem(Disk computeObject, ZoneItem zoneItem) {
      super(computeObject.getSelfLink(), computeObject.getName());
      disk = computeObject;

      // Disks should display their Zone's user message.
      if (!Strings.isNullOrEmpty(zoneItem.userMessage)) {
        userMessage = "Zone activity: " + zoneItem.userMessage;
      }
    }
  }

  /**
   * A dummy item representing a Google Compute Engine Zone.
   */
  public static class ZoneItem extends DummyItem {
    public Zone zone = null;
    public String userMessage = null;

    public ZoneItem(Zone computeObject) {
      super(computeObject.getSelfLink(), computeObject.getName());
      zone = computeObject;

      // Process the Zone and generate a user message if a maintenance window is near.
      List<MaintenanceWindows> maintenanceWindows = computeObject.getMaintenanceWindows();
      long millisUntilNextWindow = Long.MAX_VALUE;
      long nowInMillis = System.currentTimeMillis();

      // Iterate through each maintenance window to find the next one.
      for (MaintenanceWindows maintenanceWindow : maintenanceWindows) {
        long startTimeInMillis = AppUtils.convertDateTime(maintenanceWindow.getBeginTime()).getTime();
        Log.v(LOG_TAG, "StartTime:" + startTimeInMillis + " now " + nowInMillis);
        long millisUntilStartTime = startTimeInMillis - nowInMillis;

        if (millisUntilStartTime < millisUntilNextWindow) {
          millisUntilNextWindow = millisUntilStartTime;
        }
      }

      // Convert the next window millis time to days.
      long daysUntilNextWindow = (millisUntilNextWindow / (1000 * 60 * 60 * 24));

      if (DEBUG) {
        Log.v(LOG_TAG, "Zone " + zone.getName() + " with next maintenance window in " + daysUntilNextWindow + " day(s).");
      }

      if (daysUntilNextWindow < 28) {
        userMessage = daysUntilNextWindow + " day"
                + ((daysUntilNextWindow == 1) ? "" : "s")
                + " until scheduled outage window.";
      }
    }
  }

  /**
   * A dummy item representing a header for a section of content.
   */
  public static class DummyHeader extends DummyItem {
    public DummyHeader(String content) {
      super((new Random()).nextLong() + "", content);
    }
  }
}