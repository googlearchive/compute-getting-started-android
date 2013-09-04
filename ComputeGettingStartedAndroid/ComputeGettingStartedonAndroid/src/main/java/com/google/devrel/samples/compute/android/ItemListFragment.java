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

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Zone;
import com.google.devrel.samples.compute.android.dummy.DummyContent;
import com.google.devrel.samples.compute.android.dummy.DummyContent.DiskItem;
import com.google.devrel.samples.compute.android.dummy.DummyContent.DummyItem;
import com.google.devrel.samples.compute.android.dummy.DummyContent.InstanceItem;
import com.google.devrel.samples.compute.android.dummy.DummyContent.ZoneItem;
import com.google.devrel.samples.compute.android.tasks.DownloadProjectInformationTask;

/**
 * A list fragment representing a list of Items. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link ItemDetailFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 *
 * This Android sample code has been modified to display Google Compute Engine resource data
 * that is stored in extended {@code DummyContent.DummyItem} objects in a consolidated
 * {@code ListView}.
 *
 * @author paul.rashidi@google.com (Paul Rashidi)
 */
public class ItemListFragment extends ListFragment {
  /**
   * The serialization (saved instance state) Bundle key representing the
   * activated item position. Only used on tablets.
   */
  private static final String STATE_ACTIVATED_POSITION = "activated_position";

  private static final String LOG_TAG = "ItemListFragment";

  private String mEmailAccount;
  private String mProjectId;

  /**
   * The fragment's current callback object, which is notified of list item
   * clicks.
   */
  private Callbacks mCallbacks = sDummyCallbacks;

  /**
   * The current activated item position. Only used on tablets.
   */
  private int mActivatedPosition = ListView.INVALID_POSITION;

  /**
   * A callback interface that all activities containing this fragment must
   * implement. This mechanism allows activities to be notified of item
   * selections.
   */
  public interface Callbacks {
    /**
     * Callback for when an item has been selected.
     */
    public void onItemSelected(String id);
  }

  /**
   * A dummy implementation of the {@link Callbacks} interface that does
   * nothing. Used only when this fragment is not attached to an activity.
   */
  private static Callbacks sDummyCallbacks = new Callbacks() {
    @Override
    public void onItemSelected(String id) {
    }
  };

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public ItemListFragment() {
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Retrieve from application preferences.
    mEmailAccount = AppUtils.getStoredAccount(getActivity());
    mProjectId = AppUtils.getStoredProjectId(getActivity(), mEmailAccount);

    Log.d(LOG_TAG, "Listing resources for:" + mEmailAccount + " and " + mProjectId);

    // TODO(developer): replace with a real list adapter that is backed by real data storage.
    ArrayAdapter<DummyItem> arrayAdapter = new ComputeResourceListAdapter(this.getActivity());
    setListAdapter(arrayAdapter);

    // Kick off a task to refresh the local data.
    DownloadProjectInformationTask downloadProjectInfoTask =
        new DownloadProjectInformationTask(getActivity(), mEmailAccount, mProjectId, arrayAdapter);
    downloadProjectInfoTask.execute((Object)null);

  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    // Restore the previously serialized activated item position.
    if (savedInstanceState != null && savedInstanceState.containsKey(
        STATE_ACTIVATED_POSITION)) {
      setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
    }
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    // Activities containing this fragment must implement its callbacks.
    if (!(activity instanceof Callbacks)) {
      throw new IllegalStateException("Activity must implement fragment's callbacks.");
    }

    mCallbacks = (Callbacks) activity;
  }

  @Override
  public void onDetach() {
    super.onDetach();

    // Reset the active callbacks interface to the dummy implementation.
    mCallbacks = sDummyCallbacks;
  }

  @Override
  public void onListItemClick(ListView listView, View view, int position, long id) {
    super.onListItemClick(listView, view, position, id);

    // Notify the active callbacks interface (the activity, if the
    // fragment is attached to one) that an item has been selected.
    DummyContent.DummyItem item = DummyContent.ITEMS.get(position);
    if (!(item instanceof DummyContent.DummyHeader)) {
      // Ignore clicks on header items.
      mCallbacks.onItemSelected(DummyContent.ITEMS.get(position).id);
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (mActivatedPosition != ListView.INVALID_POSITION) {
      // Serialize and persist the activated item position.
      outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
    }
  }

  /**
   * Turns on activate-on-click mode. When this mode is on, list items will be
   * given the 'activated' state when touched.
   */
  public void setActivateOnItemClick(boolean activateOnItemClick) {
    // When setting CHOICE_MODE_SINGLE, ListView will automatically
    // give items the 'activated' state when touched.
    getListView().setChoiceMode(activateOnItemClick ? ListView.CHOICE_MODE_SINGLE :
        ListView.CHOICE_MODE_NONE);
  }

  private void setActivatedPosition(int position) {
    if (position == ListView.INVALID_POSITION) {
      getListView().setItemChecked(mActivatedPosition, false);
    } else {
      getListView().setItemChecked(position, true);
    }

    mActivatedPosition = position;
  }

  /**
   * Reuse the basic code of an {@code ArrayAdapter} to display Google Compute Engine resources in a
   * ListView.
   */
  public static class ComputeResourceListAdapter extends ArrayAdapter<DummyItem> {
    private static final int COLOR_GREEN = Color.parseColor("#0FB721");
    private static final int COLOR_RED = Color.parseColor("#CA1229");
    /**
     * Reusing an Android supplied simple list layout.
     */
    private static final int ITEM_LAYOUT = android.R.layout.simple_list_item_2;
    /**
     * Referencing the ID of the small TextView in {@code ITEM_LAYOUT}.
     */
    private static final int LARGE_TEXT_VIEW_ID_IN_LAYOUT = android.R.id.text1;
    /**
     * Referencing the ID of the small TextView in {@code ITEM_LAYOUT}.
     */
    private static final int SMALL_TEXT_VIEW_ID_IN_LAYOUT = android.R.id.text2;

    public ComputeResourceListAdapter(Activity activity) {
      super(activity, ITEM_LAYOUT, LARGE_TEXT_VIEW_ID_IN_LAYOUT, DummyContent.ITEMS);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      // ArrayAdapter takes care of populating the view with text according to the .toString() of
      // the objects within the array that it is mapping to the ListView widget.
      View view = super.getView(position, convertView, parent);

      // Find the item associated with this row.
      DummyContent.DummyItem item = getItem(position);

      // Override the text being displayed by ArrayAdapter by default.
      if (item instanceof InstanceItem) {
        displayInstanceDataInView(view, (InstanceItem) item);
      } else if (item instanceof DiskItem) {
        displayDiskDataInView(view, (DiskItem) item);
      } else if (item instanceof ZoneItem) {
        displayZoneDataInView(view, (ZoneItem) item);
      } else {
        // Clear out the small text view.
        TextView smallTextTextView = (TextView) view.findViewById(SMALL_TEXT_VIEW_ID_IN_LAYOUT);
        smallTextTextView.setText("");
        smallTextTextView.setVisibility(View.GONE);
      }

      if (item instanceof DummyContent.DummyHeader) {
        TextView textView = (TextView) view.findViewById(LARGE_TEXT_VIEW_ID_IN_LAYOUT);
        textView.setGravity(View.TEXT_ALIGNMENT_GRAVITY);
      } else {
        TextView textView = (TextView) view.findViewById(LARGE_TEXT_VIEW_ID_IN_LAYOUT);
        textView.setGravity(View.TEXT_ALIGNMENT_CENTER);
      }

      return view;
    }

    /**
     * Override the {@code ArrayAdapter} code to display more than just .toString() if the item is
     * a Zone.
     */
    private void displayZoneDataInView(View view, ZoneItem zoneItem) {
      Zone zone = zoneItem.zone;

      // Construct a colored status string.
      SpannableString statusText = new SpannableString(zone.getStatus());
      if ("UP".equalsIgnoreCase(zone.getStatus())) {
        statusText.setSpan(new ForegroundColorSpan(COLOR_GREEN), 0, zone.getStatus().length(), 0);
      } else {
        statusText.setSpan(new ForegroundColorSpan(COLOR_RED), 0, zone.getStatus().length(), 0);
      }

      // Put the status string in the small TextView of the view.
      TextView smallTextTextView = (TextView)view.findViewById(SMALL_TEXT_VIEW_ID_IN_LAYOUT);
      smallTextTextView.setText(statusText);
      smallTextTextView.setVisibility(View.VISIBLE);

      // Rebuild the large TextView string if the zoneItem has a userMessage.
      if (!Strings.isNullOrEmpty(zoneItem.userMessage)) {
        TextView largeTextTextView = (TextView) view.findViewById(LARGE_TEXT_VIEW_ID_IN_LAYOUT);
        SpannableString text2 = new SpannableString(zone.getName() + "\n" + zoneItem.userMessage);
        text2.setSpan(new ForegroundColorSpan(COLOR_RED), zone.getName().length() + 1,
            text2.length(), 0);
        text2.setSpan(new RelativeSizeSpan(.75F), zone.getName().length() + 1, text2.length(), 0);
        largeTextTextView.setText(text2);
      }
    }

    /**
     * Override the {@code ArrayAdapter} code to display more than just .toString() if the item is
     * a Disk.
     */
    private void displayDiskDataInView(View view, DiskItem diskItem) {
      Disk disk = diskItem.disk;

      // For a DiskItem populate the small text view with the user message and status.
      SpannableString text;
      if (!Strings.isNullOrEmpty(diskItem.userMessage)) {
        // User message was present.
        String statusText = disk.getStatus() + "\n"
            + ((diskItem.userMessage != null) ? diskItem.userMessage : "");
        text = new SpannableString(statusText);
        text.setSpan(new ForegroundColorSpan(COLOR_RED), 0, text.length(), 0);
      } else if ("READY".equalsIgnoreCase(disk.getStatus())) {
        // No user message and disk was in a good state, therefore, display status.
        text = new SpannableString(disk.getStatus());
        text.setSpan(new ForegroundColorSpan(COLOR_GREEN), 0, disk.getStatus().length(), 0);
      } else {
        // No user message and disk was not in a good state; display status in red.
        text = new SpannableString(disk.getStatus());
        text.setSpan(new ForegroundColorSpan(COLOR_RED), 0, disk.getStatus().length(), 0);
      }

      TextView smallTextTextView = (TextView)view.findViewById(SMALL_TEXT_VIEW_ID_IN_LAYOUT);
      smallTextTextView.setText(text);
      smallTextTextView.setVisibility(View.VISIBLE);
    }

    /**
     * Override the {@code ArrayAdapter} code to display more than just .toString() if the item is
     * a Instance.
     */
    private void displayInstanceDataInView(View view, InstanceItem instanceItem) {
      Instance instance = instanceItem.instance;

      // Rebuild the large text TextView if the Instance has a description.
      if (!Strings.isNullOrEmpty(instance.getDescription())) {
        String descriptionText = AppUtils.trimString(instance.getDescription(), 40, null, "..");
        TextView largeTextTextView = (TextView) view.findViewById(LARGE_TEXT_VIEW_ID_IN_LAYOUT);
        SpannableString text2 = new SpannableString(instance.getName() + "\n" + descriptionText);
        text2.setSpan(new ForegroundColorSpan(Color.GRAY), instance.getName().length() + 1,
            text2.length(), 0);
        text2.setSpan(new RelativeSizeSpan(.75F), instance.getName().length() + 1, text2.length(),
            0);
        largeTextTextView.setText(text2);
      }

      // For an InstanceItem populate the small text view with the user message and status.
      SpannableString text;
      if (!Strings.isNullOrEmpty(instanceItem.userMessage)) {
        // User message was present.
        String statusText = instance.getStatus() + "\n"
            + ((instanceItem.userMessage != null) ? instanceItem.userMessage : "");
        text = new SpannableString(statusText);
        text.setSpan(new ForegroundColorSpan(COLOR_RED), 0, text.length(), 0);
      } else if ("RUNNING".equalsIgnoreCase(instance.getStatus())) {
        // No user message and instance was in a good state, therefore, display status.
        text = new SpannableString(instance.getStatus());
        text.setSpan(new ForegroundColorSpan(COLOR_GREEN), 0, instance.getStatus().length(), 0);
      } else {
        // No user message and instance was not in a good state; display status in red.
        text = new SpannableString(instance.getStatus());
        text.setSpan(new ForegroundColorSpan(COLOR_RED), 0, instance.getStatus().length(), 0);
      }

      TextView smallTextTextView = (TextView)view.findViewById(SMALL_TEXT_VIEW_ID_IN_LAYOUT);
      smallTextTextView.setText(text);
      smallTextTextView.setVisibility(View.VISIBLE);
    }
  }
}
