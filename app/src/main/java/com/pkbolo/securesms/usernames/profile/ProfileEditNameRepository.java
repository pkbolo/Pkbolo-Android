package com.pkbolo.securesms.usernames.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.pkbolo.securesms.crypto.ProfileKeyUtil;
import com.pkbolo.securesms.database.DatabaseFactory;
import com.pkbolo.securesms.util.TextSecurePreferences;
import com.pkbolo.securesms.util.Util;
import com.pkbolo.securesms.dependencies.ApplicationDependencies;
import com.pkbolo.securesms.recipients.Recipient;
import com.pkbolo.securesms.util.concurrent.SignalExecutors;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;

import java.io.IOException;
import java.util.concurrent.Executor;

class ProfileEditNameRepository {

  private final Application                 application;
  private final SignalServiceAccountManager accountManager;
  private final Executor                    executor;

  ProfileEditNameRepository() {
    this.application    = ApplicationDependencies.getApplication();
    this.accountManager = ApplicationDependencies.getSignalServiceAccountManager();
    this.executor       = SignalExecutors.UNBOUNDED;
  }

  void setProfileName(@NonNull String profileName, @NonNull Callback<ProfileNameResult> callback) {
    executor.execute(() -> callback.onResult(setProfileNameInternal(profileName)));
  }

  @WorkerThread
  private @NonNull ProfileNameResult setProfileNameInternal(@NonNull String profileName) {
    Util.sleep(1000);
    try {
      accountManager.setProfileName(ProfileKeyUtil.getProfileKey(application), profileName);
      TextSecurePreferences.setProfileName(application, profileName);
      DatabaseFactory.getRecipientDatabase(application).setProfileName(Recipient.self().getId(), profileName);
      return ProfileNameResult.SUCCESS;
    } catch (IOException e) {
      return ProfileNameResult.NETWORK_FAILURE;
    }
  }

  enum ProfileNameResult {
    SUCCESS, NETWORK_FAILURE
  }

  interface Callback<E> {
    void onResult(@NonNull E result);
  }
}
