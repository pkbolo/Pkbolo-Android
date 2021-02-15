package com.pkbolo.securesms.dependencies;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.pkbolo.securesms.IncomingMessageProcessor;
import com.pkbolo.securesms.crypto.storage.SignalProtocolStoreImpl;
import com.pkbolo.securesms.database.DatabaseFactory;
import com.pkbolo.securesms.events.ReminderUpdateEvent;
import com.pkbolo.securesms.gcm.MessageRetriever;
import com.pkbolo.securesms.jobmanager.impl.JsonDataSerializer;
import com.pkbolo.securesms.jobs.FastJobStorage;
import com.pkbolo.securesms.jobs.JobManagerFactories;
import com.pkbolo.securesms.logging.Log;
import com.pkbolo.securesms.push.SecurityEventListener;
import com.pkbolo.securesms.push.SignalServiceNetworkAccess;
import com.pkbolo.securesms.service.IncomingMessageObserver;
import com.pkbolo.securesms.util.AlarmSleepTimer;
import com.pkbolo.securesms.util.FrameRateTracker;
import com.pkbolo.securesms.util.TextSecurePreferences;

import org.greenrobot.eventbus.EventBus;
import com.pkbolo.securesms.BuildConfig;

import com.pkbolo.securesms.jobmanager.JobManager;
import com.pkbolo.securesms.jobmanager.JobMigrator;
import com.pkbolo.securesms.recipients.LiveRecipientCache;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.util.CredentialsProvider;
import org.whispersystems.signalservice.api.util.SleepTimer;
import org.whispersystems.signalservice.api.util.UptimeSleepTimer;
import org.whispersystems.signalservice.api.websocket.ConnectivityListener;

import java.util.UUID;

/**
 * Implementation of {@link ApplicationDependencies.Provider} that provides real app dependencies.
 */
public class ApplicationDependencyProvider implements ApplicationDependencies.Provider {

  private static final String TAG = Log.tag(ApplicationDependencyProvider.class);

  private final Application                context;
  private final SignalServiceNetworkAccess networkAccess;

  public ApplicationDependencyProvider(@NonNull Application context, @NonNull SignalServiceNetworkAccess networkAccess) {
    this.context       = context;
    this.networkAccess = networkAccess;
  }

  @Override
  public @NonNull SignalServiceAccountManager provideSignalServiceAccountManager() {
    return new SignalServiceAccountManager(networkAccess.getConfiguration(context),
                                           new DynamicCredentialsProvider(context),
                                           BuildConfig.USER_AGENT);
  }

  @Override
  public @NonNull SignalServiceMessageSender provideSignalServiceMessageSender() {
      return new SignalServiceMessageSender(networkAccess.getConfiguration(context),
                                            new DynamicCredentialsProvider(context),
                                            new SignalProtocolStoreImpl(context),
                                            BuildConfig.USER_AGENT,
                                            TextSecurePreferences.isMultiDevice(context),
                                            Optional.fromNullable(IncomingMessageObserver.getPipe()),
                                            Optional.fromNullable(IncomingMessageObserver.getUnidentifiedPipe()),
                                            Optional.of(new SecurityEventListener(context)));
  }

  @Override
  public @NonNull SignalServiceMessageReceiver provideSignalServiceMessageReceiver() {
    SleepTimer sleepTimer = TextSecurePreferences.isFcmDisabled(context) ? new AlarmSleepTimer(context)
                                                                         : new UptimeSleepTimer();
    return new SignalServiceMessageReceiver(networkAccess.getConfiguration(context),
                                            new DynamicCredentialsProvider(context),
                                            BuildConfig.USER_AGENT,
                                            new PipeConnectivityListener(),
                                            sleepTimer);
  }

  @Override
  public @NonNull SignalServiceNetworkAccess provideSignalServiceNetworkAccess() {
    return networkAccess;
  }

  @Override
  public @NonNull
  IncomingMessageProcessor provideIncomingMessageProcessor() {
    return new IncomingMessageProcessor(context);
  }

  @Override
  public @NonNull
  MessageRetriever provideMessageRetriever() {
    return new MessageRetriever();
  }

  @Override
  public @NonNull LiveRecipientCache provideRecipientCache() {
    return new LiveRecipientCache(context);
  }

  @Override
  public @NonNull JobManager provideJobManager() {
    return new JobManager(context, new JobManager.Configuration.Builder()
                                                               .setDataSerializer(new JsonDataSerializer())
                                                               .setJobFactories(JobManagerFactories.getJobFactories(context))
                                                               .setConstraintFactories(JobManagerFactories.getConstraintFactories(context))
                                                               .setConstraintObservers(JobManagerFactories.getConstraintObservers(context))
                                                               .setJobStorage(new FastJobStorage(DatabaseFactory.getJobDatabase(context)))
                                                               .setJobMigrator(new JobMigrator(TextSecurePreferences.getJobManagerVersion(context), JobManager.CURRENT_VERSION, JobManagerFactories.getJobMigrations(context)))
                                                               .build());
  }

  @Override
  public @NonNull
  FrameRateTracker provideFrameRateTracker() {
    return new FrameRateTracker(context);
  }

  private static class DynamicCredentialsProvider implements CredentialsProvider {

    private final Context context;

    private DynamicCredentialsProvider(Context context) {
      this.context = context.getApplicationContext();
    }

    @Override
    public UUID getUuid() {
      return TextSecurePreferences.getLocalUuid(context);
    }

    @Override
    public String getE164() {
      return TextSecurePreferences.getLocalNumber(context);
    }

    @Override
    public String getPassword() {
      return TextSecurePreferences.getPushServerPassword(context);
    }

    @Override
    public String getSignalingKey() {
      return TextSecurePreferences.getSignalingKey(context);
    }
  }

  private class PipeConnectivityListener implements ConnectivityListener {

    @Override
    public void onConnected() {
      Log.i(TAG, "onConnected()");
      TextSecurePreferences.setUnauthorizedReceived(context, false);
    }

    @Override
    public void onConnecting() {
      Log.i(TAG, "onConnecting()");
    }

    @Override
    public void onDisconnected() {
      Log.w(TAG, "onDisconnected()");
    }

    @Override
    public void onAuthenticationFailure() {
      Log.w(TAG, "onAuthenticationFailure()");
      TextSecurePreferences.setUnauthorizedReceived(context, true);
      EventBus.getDefault().post(new ReminderUpdateEvent());
    }
  }
}
