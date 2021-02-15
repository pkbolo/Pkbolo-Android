package com.pkbolo.securesms.jobs;

import androidx.annotation.NonNull;

import com.annimon.stream.Stream;
import com.pkbolo.securesms.database.DatabaseFactory;
import com.pkbolo.securesms.database.RecipientDatabase;
import com.pkbolo.securesms.database.StorageKeyDatabase;
import com.pkbolo.securesms.logging.Log;
import com.pkbolo.securesms.transport.RetryLaterException;
import com.pkbolo.securesms.util.FeatureFlags;
import com.pkbolo.securesms.util.TextSecurePreferences;
import com.pkbolo.securesms.util.Util;

import com.pkbolo.securesms.contacts.sync.StorageSyncHelper;
import com.pkbolo.securesms.dependencies.ApplicationDependencies;
import com.pkbolo.securesms.jobmanager.Data;
import com.pkbolo.securesms.jobmanager.Job;
import com.pkbolo.securesms.jobmanager.impl.NetworkConstraint;
import com.pkbolo.securesms.recipients.RecipientId;

import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;
import org.whispersystems.signalservice.api.storage.SignalStorageManifest;
import org.whispersystems.signalservice.api.storage.SignalStorageRecord;
import org.whispersystems.signalservice.api.storage.SignalStorageUtil;
import org.whispersystems.signalservice.internal.storage.protos.StorageRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Forces remote storage to match our local state. This should only be done after a key change or
 * when we detect that the remote data is badly-encrypted.
 */
public class StorageForcePushJob extends BaseJob {

  public static final String KEY = "StorageForcePushJob";

  private static final String TAG = Log.tag(StorageForcePushJob.class);

  public StorageForcePushJob() {
    this(new Parameters.Builder().addConstraint(NetworkConstraint.KEY)
                                     .setQueue(StorageSyncJob.QUEUE_KEY)
                                     .setMaxInstances(1)
                                     .setLifespan(TimeUnit.DAYS.toMillis(1))
                                     .build());
  }

  private StorageForcePushJob(@NonNull Parameters parameters) {
    super(parameters);
  }

  @Override
  public @NonNull Data serialize() {
    return Data.EMPTY;
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  protected void onRun() throws IOException, RetryLaterException {
    if (!FeatureFlags.STORAGE_SERVICE) throw new AssertionError();

    byte[] kbsMasterKey = TextSecurePreferences.getMasterKey(context);

    if (kbsMasterKey == null) {
      Log.w(TAG, "No KBS master key is set! Must abort.");
      return;
    }

    byte[]                      storageServiceKey  = SignalStorageUtil.computeStorageServiceKey(kbsMasterKey);
    SignalServiceAccountManager accountManager     = ApplicationDependencies.getSignalServiceAccountManager();
    RecipientDatabase recipientDatabase  = DatabaseFactory.getRecipientDatabase(context);
    StorageKeyDatabase storageKeyDatabase = DatabaseFactory.getStorageKeyDatabase(context);

    long                     currentVersion = accountManager.getStorageManifestVersion();
    Map<RecipientId, byte[]> oldContactKeys = recipientDatabase.getAllStorageSyncKeysMap();
    List<byte[]>             oldUnknownKeys = storageKeyDatabase.getAllKeys();

    long                      newVersion     = currentVersion + 1;
    Map<RecipientId, byte[]>  newContactKeys = generateNewKeys(oldContactKeys);
    List<byte[]>              keysToDelete   = Util.concatenatedList(new ArrayList<>(oldContactKeys.values()), oldUnknownKeys);
    List<SignalStorageRecord> inserts        = Stream.of(oldContactKeys.keySet())
                                                     .map(recipientDatabase::getRecipientSettings)
                                                     .withoutNulls()
                                                     .map(StorageSyncHelper::localToRemoteContact)
                                                     .map(r -> SignalStorageRecord.forContact(r.getKey(), r))
                                                     .toList();

    SignalStorageManifest manifest = new SignalStorageManifest(newVersion, new ArrayList<>(newContactKeys.values()));

    try {
      accountManager.writeStorageRecords(storageServiceKey, manifest, inserts, keysToDelete);
    } catch (InvalidKeyException e) {
      Log.w(TAG, "Hit an invalid key exception, which likely indicates a conflict.");
      throw new RetryLaterException();
    }

    TextSecurePreferences.setStorageManifestVersion(context, newVersion);
    recipientDatabase.applyStorageSyncKeyUpdates(newContactKeys);
    storageKeyDatabase.deleteAll();
  }

  @Override
  protected boolean onShouldRetry(@NonNull Exception e) {
    return e instanceof PushNetworkException || e instanceof RetryLaterException;
  }

  @Override
  public void onCanceled() {
  }

  private static @NonNull Map<RecipientId, byte[]> generateNewKeys(@NonNull Map<RecipientId, byte[]> oldKeys) {
    Map<RecipientId, byte[]> out = new HashMap<>();

    for (Map.Entry<RecipientId, byte[]> entry : oldKeys.entrySet()) {
      out.put(entry.getKey(), StorageSyncHelper.generateKey());
    }

    return out;
  }

  public static final class Factory implements Job.Factory<StorageForcePushJob> {

    @Override
    public @NonNull
    StorageForcePushJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new StorageForcePushJob(parameters);
    }
  }
}
