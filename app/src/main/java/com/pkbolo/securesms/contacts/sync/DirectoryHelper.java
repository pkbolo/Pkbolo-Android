package com.pkbolo.securesms.contacts.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.pkbolo.securesms.database.RecipientDatabase;
import com.pkbolo.securesms.jobs.StorageSyncJob;
import com.pkbolo.securesms.util.FeatureFlags;
import com.pkbolo.securesms.dependencies.ApplicationDependencies;
import com.pkbolo.securesms.recipients.Recipient;

import java.io.IOException;

public class DirectoryHelper {

  @WorkerThread
  public static void refreshDirectory(@NonNull Context context, boolean notifyOfNewUsers) throws IOException {
    if (FeatureFlags.UUIDS) {
      // TODO [greyson] Create a DirectoryHelperV2 when appropriate.
      DirectoryHelperV1.refreshDirectory(context, notifyOfNewUsers);
    } else {
      DirectoryHelperV1.refreshDirectory(context, notifyOfNewUsers);
    }

    if (FeatureFlags.STORAGE_SERVICE) {
      ApplicationDependencies.getJobManager().add(new StorageSyncJob());
    }
  }

  @WorkerThread
  public static RecipientDatabase.RegisteredState refreshDirectoryFor(@NonNull Context context, @NonNull Recipient recipient, boolean notifyOfNewUsers) throws IOException {
    RecipientDatabase.RegisteredState originalRegisteredState = recipient.resolve().getRegistered();
    RecipientDatabase.RegisteredState newRegisteredState      = null;

    if (FeatureFlags.UUIDS) {
      // TODO [greyson] Create a DirectoryHelperV2 when appropriate.
      newRegisteredState = DirectoryHelperV1.refreshDirectoryFor(context, recipient, notifyOfNewUsers);
    } else {
      newRegisteredState = DirectoryHelperV1.refreshDirectoryFor(context, recipient, notifyOfNewUsers);
    }

    if (FeatureFlags.STORAGE_SERVICE && newRegisteredState != originalRegisteredState) {
      ApplicationDependencies.getJobManager().add(new StorageSyncJob());
    }

    return newRegisteredState;
  }
}
