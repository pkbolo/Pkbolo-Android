/*
 * Copyright (C) 2015 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.pkbolo.securesms;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.pkbolo.securesms.contacts.ContactsCursorLoader;
import com.pkbolo.securesms.logging.Log;
import com.pkbolo.securesms.util.DynamicLanguage;
import com.pkbolo.securesms.util.DynamicNoActionBarTheme;
import com.pkbolo.securesms.util.DynamicTheme;
import com.pkbolo.securesms.util.TextSecurePreferences;
import com.pkbolo.securesms.util.ViewUtil;

import com.pkbolo.securesms.components.ContactFilterToolbar;
import com.pkbolo.securesms.contacts.sync.DirectoryHelper;
import com.pkbolo.securesms.recipients.RecipientId;

import org.whispersystems.libsignal.util.guava.Optional;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Base activity container for selecting a list of contacts.
 *
 * @author Moxie Marlinspike
 *
 */
public abstract class ContactSelectionActivity extends PassphraseRequiredActionBarActivity
                                               implements SwipeRefreshLayout.OnRefreshListener,
                                                          ContactSelectionListFragment.OnContactSelectedListener
{
  private static final String TAG = ContactSelectionActivity.class.getSimpleName();

  private final DynamicTheme dynamicTheme    = new DynamicNoActionBarTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  protected ContactSelectionListFragment contactsFragment;

  private ContactFilterToolbar toolbar;

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
  }

  @Override
  protected void onCreate(Bundle icicle, boolean ready) {
    if (!getIntent().hasExtra(ContactSelectionListFragment.DISPLAY_MODE)) {
      int displayMode = TextSecurePreferences.isSmsEnabled(this) ? ContactsCursorLoader.DisplayMode.FLAG_ALL
                                                                 : ContactsCursorLoader.DisplayMode.FLAG_PUSH | ContactsCursorLoader.DisplayMode.FLAG_ACTIVE_GROUPS | ContactsCursorLoader.DisplayMode.FLAG_INACTIVE_GROUPS;
      getIntent().putExtra(ContactSelectionListFragment.DISPLAY_MODE, displayMode);
    }

    setContentView(R.layout.contact_selection_activity);

    initializeToolbar();
    initializeResources();
    initializeSearch();
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
  }

  protected ContactFilterToolbar getToolbar() {
    return toolbar;
  }

  private void initializeToolbar() {
    this.toolbar = ViewUtil.findById(this, R.id.toolbar);
    setSupportActionBar(toolbar);

    assert  getSupportActionBar() != null;
    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    getSupportActionBar().setDisplayShowTitleEnabled(false);
    getSupportActionBar().setIcon(null);
    getSupportActionBar().setLogo(null);
  }

  private void initializeResources() {
    contactsFragment = (ContactSelectionListFragment) getSupportFragmentManager().findFragmentById(R.id.contact_selection_list_fragment);
    contactsFragment.setOnContactSelectedListener(this);
    contactsFragment.setOnRefreshListener(this);
  }

  private void initializeSearch() {
    toolbar.setOnFilterChangedListener(filter -> contactsFragment.setQueryFilter(filter));
  }

  @Override
  public void onRefresh() {
    new RefreshDirectoryTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getApplicationContext());
  }

  @Override
  public void onContactSelected(Optional<RecipientId> recipientId, String number) {}

  @Override
  public void onContactDeselected(Optional<RecipientId> recipientId, String number) {}

  private static class RefreshDirectoryTask extends AsyncTask<Context, Void, Void> {

    private final WeakReference<ContactSelectionActivity> activity;

    private RefreshDirectoryTask(ContactSelectionActivity activity) {
      this.activity = new WeakReference<>(activity);
    }

    @Override
    protected Void doInBackground(Context... params) {
      try {
        DirectoryHelper.refreshDirectory(params[0], true);
      } catch (IOException e) {
        Log.w(TAG, e);
      }

      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      ContactSelectionActivity activity = this.activity.get();

      if (activity != null && !activity.isFinishing()) {
        activity.toolbar.clear();
        activity.contactsFragment.resetQueryFilter();
      }
    }
  }
}
