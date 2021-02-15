package com.pkbolo.securesms.jobs;

import android.content.Context;
import androidx.annotation.NonNull;

import com.pkbolo.securesms.crypto.IdentityKeyUtil;
import com.pkbolo.securesms.crypto.PreKeyUtil;
import com.pkbolo.securesms.logging.Log;
import com.pkbolo.securesms.util.TextSecurePreferences;
import com.pkbolo.securesms.dependencies.ApplicationDependencies;
import com.pkbolo.securesms.jobmanager.Data;
import com.pkbolo.securesms.jobmanager.Job;
import com.pkbolo.securesms.jobmanager.impl.NetworkConstraint;

import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;

import java.io.IOException;

public class CreateSignedPreKeyJob extends BaseJob {

  public static final String KEY = "CreateSignedPreKeyJob";

  private static final String TAG = CreateSignedPreKeyJob.class.getSimpleName();

  public CreateSignedPreKeyJob(Context context) {
    this(new Job.Parameters.Builder()
                           .addConstraint(NetworkConstraint.KEY)
                           .setQueue("CreateSignedPreKeyJob")
                           .setMaxAttempts(25)
                           .build());
  }

  private CreateSignedPreKeyJob(@NonNull Job.Parameters parameters) {
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
  public void onRun() throws IOException {
    if (TextSecurePreferences.isSignedPreKeyRegistered(context)) {
      Log.w(TAG, "Signed prekey already registered...");
      return;
    }

    if (!TextSecurePreferences.isPushRegistered(context)) {
      Log.w(TAG, "Not yet registered...");
      return;
    }

    SignalServiceAccountManager accountManager     = ApplicationDependencies.getSignalServiceAccountManager();
    IdentityKeyPair             identityKeyPair    = IdentityKeyUtil.getIdentityKeyPair(context);
    SignedPreKeyRecord          signedPreKeyRecord = PreKeyUtil.generateSignedPreKey(context, identityKeyPair, true);

    accountManager.setSignedPreKey(signedPreKeyRecord);
    TextSecurePreferences.setSignedPreKeyRegistered(context, true);
  }

  @Override
  public void onCanceled() {}

  @Override
  public boolean onShouldRetry(@NonNull Exception exception) {
    if (exception instanceof PushNetworkException) return true;
    return false;
  }

  public static final class Factory implements Job.Factory<CreateSignedPreKeyJob> {
    @Override
    public @NonNull CreateSignedPreKeyJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new CreateSignedPreKeyJob(parameters);
    }
  }
}
