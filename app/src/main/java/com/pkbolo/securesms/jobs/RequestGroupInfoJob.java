package com.pkbolo.securesms.jobs;

import androidx.annotation.NonNull;

import com.pkbolo.securesms.crypto.UnidentifiedAccessUtil;
import com.pkbolo.securesms.util.GroupUtil;
import com.pkbolo.securesms.dependencies.ApplicationDependencies;
import com.pkbolo.securesms.jobmanager.Data;
import com.pkbolo.securesms.jobmanager.Job;
import com.pkbolo.securesms.jobmanager.impl.NetworkConstraint;
import com.pkbolo.securesms.recipients.RecipientId;
import com.pkbolo.securesms.recipients.RecipientUtil;
import com.pkbolo.securesms.recipients.Recipient;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.SignalServiceGroup;
import org.whispersystems.signalservice.api.messages.SignalServiceGroup.Type;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class RequestGroupInfoJob extends BaseJob {

  public static final String KEY = "RequestGroupInfoJob";

  @SuppressWarnings("unused")
  private static final String TAG = RequestGroupInfoJob.class.getSimpleName();

  private static final String KEY_SOURCE   = "source";
  private static final String KEY_GROUP_ID = "group_id";

  private RecipientId source;
  private byte[]      groupId;

  public RequestGroupInfoJob(@NonNull RecipientId source, @NonNull byte[] groupId) {
    this(new Job.Parameters.Builder()
                           .addConstraint(NetworkConstraint.KEY)
                           .setLifespan(TimeUnit.DAYS.toMillis(1))
                           .setMaxAttempts(Parameters.UNLIMITED)
                           .build(),
         source,
         groupId);

  }

  private RequestGroupInfoJob(@NonNull Job.Parameters parameters, @NonNull RecipientId source, @NonNull byte[] groupId) {
    super(parameters);

    this.source  = source;
    this.groupId = groupId;
  }

  @Override
  public @NonNull Data serialize() {
    return new Data.Builder().putString(KEY_SOURCE, source.serialize())
                             .putString(KEY_GROUP_ID, GroupUtil.getEncodedId(groupId, false))
                             .build();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onRun() throws IOException, UntrustedIdentityException {
    SignalServiceGroup       group   = SignalServiceGroup.newBuilder(Type.REQUEST_INFO)
                                                         .withId(groupId)
                                                         .build();

    SignalServiceDataMessage message = SignalServiceDataMessage.newBuilder()
                                                               .asGroupMessage(group)
                                                               .withTimestamp(System.currentTimeMillis())
                                                               .build();

    SignalServiceMessageSender messageSender = ApplicationDependencies.getSignalServiceMessageSender();
    Recipient                  recipient     = Recipient.resolved(source);

    messageSender.sendMessage(RecipientUtil.toSignalServiceAddress(context, recipient),
                              UnidentifiedAccessUtil.getAccessFor(context, recipient),
                              message);
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception e) {
    return e instanceof PushNetworkException;
  }

  @Override
  public void onCanceled() {

  }

  public static final class Factory implements Job.Factory<RequestGroupInfoJob> {

    @Override
    public @NonNull RequestGroupInfoJob create(@NonNull Parameters parameters, @NonNull Data data) {
      try {
        return new RequestGroupInfoJob(parameters,
                                       RecipientId.from(data.getString(KEY_SOURCE)),
                                       GroupUtil.getDecodedId(data.getString(KEY_GROUP_ID)));
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    }
  }
}
