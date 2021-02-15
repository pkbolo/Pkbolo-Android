package com.pkbolo.securesms.jobs;


import androidx.annotation.NonNull;

import com.pkbolo.securesms.crypto.UnidentifiedAccessUtil;
import com.pkbolo.securesms.logging.Log;
import com.pkbolo.securesms.util.TextSecurePreferences;
import com.pkbolo.securesms.dependencies.ApplicationDependencies;
import com.pkbolo.securesms.jobmanager.Data;
import com.pkbolo.securesms.jobmanager.Job;
import com.pkbolo.securesms.jobmanager.impl.NetworkConstraint;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.messages.multidevice.KeysMessage;
import org.whispersystems.signalservice.api.messages.multidevice.SignalServiceSyncMessage;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;
import org.whispersystems.signalservice.api.storage.SignalStorageUtil;

import java.io.IOException;

public class MultiDeviceKeysUpdateJob extends BaseJob {

  public static final String KEY = "MultiDeviceKeysUpdateJob";

  private static final String TAG = MultiDeviceKeysUpdateJob.class.getSimpleName();

  public MultiDeviceKeysUpdateJob() {
    this(new Parameters.Builder()
                           .setQueue("MultiDeviceKeysUpdateJob")
                           .setMaxInstances(2)
                           .addConstraint(NetworkConstraint.KEY)
                           .setMaxAttempts(10)
                           .build());

  }

  private MultiDeviceKeysUpdateJob(@NonNull Parameters parameters) {
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
  public void onRun() throws IOException, UntrustedIdentityException {
    if (!TextSecurePreferences.isMultiDevice(context)) {
      Log.i(TAG, "Not multi device, aborting...");
      return;
    }

    SignalServiceMessageSender messageSender = ApplicationDependencies.getSignalServiceMessageSender();

    byte[] masterKey         = TextSecurePreferences.getMasterKey(context);
    byte[] storageServiceKey = masterKey != null ? SignalStorageUtil.computeStorageServiceKey(masterKey)
                                                 : null;

    if (storageServiceKey == null) {
      Log.w(TAG, "Syncing a null storage service key.");
    }

    messageSender.sendMessage(SignalServiceSyncMessage.forKeys(new KeysMessage(Optional.fromNullable(storageServiceKey))),
                              UnidentifiedAccessUtil.getAccessForSync(context));
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception e) {
    return e instanceof PushNetworkException;
  }

  @Override
  public void onCanceled() {
  }

  public static final class Factory implements Job.Factory<MultiDeviceKeysUpdateJob> {
    @Override
    public @NonNull MultiDeviceKeysUpdateJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new MultiDeviceKeysUpdateJob(parameters);
    }
  }
}
