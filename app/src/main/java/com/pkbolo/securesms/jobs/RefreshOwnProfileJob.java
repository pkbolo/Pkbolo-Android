package com.pkbolo.securesms.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pkbolo.securesms.crypto.ProfileKeyUtil;
import com.pkbolo.securesms.database.DatabaseFactory;
import com.pkbolo.securesms.logging.Log;
import com.pkbolo.securesms.util.ProfileUtil;
import com.pkbolo.securesms.util.TextSecurePreferences;
import com.pkbolo.securesms.dependencies.ApplicationDependencies;
import com.pkbolo.securesms.jobmanager.Data;
import com.pkbolo.securesms.jobmanager.Job;
import com.pkbolo.securesms.jobmanager.impl.NetworkConstraint;
import com.pkbolo.securesms.recipients.Recipient;

import org.whispersystems.signalservice.api.crypto.InvalidCiphertextException;
import org.whispersystems.signalservice.api.profiles.SignalServiceProfile;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;

import java.io.IOException;


/**
 * Refreshes the profile of the local user. Different from {@link RetrieveProfileJob} in that we
 * have to sometimes look at/set different data stores, and we will *always* do the fetch regardless
 * of caching.
 */
public class RefreshOwnProfileJob extends BaseJob {

  public static final String KEY = "RefreshOwnProfileJob";

  private static final String TAG = Log.tag(RefreshOwnProfileJob.class);

  public RefreshOwnProfileJob() {
    this(new Parameters.Builder()
                       .addConstraint(NetworkConstraint.KEY)
                       .setQueue("RefreshOwnProfileJob")
                       .setMaxInstances(1)
                       .setMaxAttempts(10)
                       .build());
  }


  private RefreshOwnProfileJob(@NonNull Parameters parameters) {
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
  protected void onRun() throws Exception {
    SignalServiceProfile profile = ProfileUtil.retrieveProfile(context, Recipient.self());

    setProfileName(profile.getName());
    setProfileAvatar(profile.getAvatar());
    setProfileCapabilities(profile.getCapabilities());
  }

  @Override
  protected boolean onShouldRetry(@NonNull Exception e) {
    return e instanceof PushNetworkException;
  }

  @Override
  public void onCanceled() { }

  private void setProfileName(@Nullable String encryptedName) {
    try {
      byte[] profileKey    = ProfileKeyUtil.getProfileKey(context);
      String plaintextName = ProfileUtil.decryptName(profileKey, encryptedName);

      DatabaseFactory.getRecipientDatabase(context).setProfileName(Recipient.self().getId(), plaintextName);
      TextSecurePreferences.setProfileName(context, plaintextName);
    } catch (InvalidCiphertextException | IOException e) {
      Log.w(TAG, e);
    }
  }

  private void setProfileAvatar(@Nullable String avatar) {
    ApplicationDependencies.getJobManager().add(new RetrieveProfileAvatarJob(Recipient.self(), avatar));
  }

  private void setProfileCapabilities(@Nullable SignalServiceProfile.Capabilities capabilities) {
    if (capabilities == null) {
      return;
    }

    DatabaseFactory.getRecipientDatabase(context).setUuidSupported(Recipient.self().getId(), capabilities.isUuid());
  }

  public static final class Factory implements Job.Factory<RefreshOwnProfileJob> {

    @Override
    public @NonNull RefreshOwnProfileJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new RefreshOwnProfileJob(parameters);
    }
  }
}
