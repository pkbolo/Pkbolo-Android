package com.pkbolo.securesms.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.telephony.SmsMessage;

import com.pkbolo.securesms.database.DatabaseFactory;
import com.pkbolo.securesms.database.MessagingDatabase;
import com.pkbolo.securesms.database.SmsDatabase;
import com.pkbolo.securesms.logging.Log;
import com.pkbolo.securesms.util.Base64;
import com.pkbolo.securesms.util.TextSecurePreferences;
import com.pkbolo.securesms.jobmanager.Data;
import com.pkbolo.securesms.jobmanager.Job;
import com.pkbolo.securesms.jobmanager.impl.SqlCipherMigrationConstraint;

import com.pkbolo.securesms.notifications.MessageNotifier;
import com.pkbolo.securesms.recipients.Recipient;
import com.pkbolo.securesms.sms.IncomingTextMessage;

import org.whispersystems.libsignal.util.guava.Optional;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class SmsReceiveJob extends BaseJob {

  public static final String KEY = "SmsReceiveJob";

  private static final String TAG = SmsReceiveJob.class.getSimpleName();

  private static final String KEY_PDUS            = "pdus";
  private static final String KEY_SUBSCRIPTION_ID = "subscription_id";

  private @Nullable Object[] pdus;

  private int subscriptionId;

  public SmsReceiveJob(@Nullable Object[] pdus, int subscriptionId) {
    this(new Job.Parameters.Builder()
                           .addConstraint(SqlCipherMigrationConstraint.KEY)
                           .setMaxAttempts(25)
                           .build(),
         pdus,
         subscriptionId);
  }

  private SmsReceiveJob(@NonNull Job.Parameters parameters, @Nullable Object[] pdus, int subscriptionId) {
    super(parameters);

    this.pdus           = pdus;
    this.subscriptionId = subscriptionId;
  }

  @Override
  public @NonNull Data serialize() {
    String[] encoded = new String[pdus.length];
    for (int i = 0; i < pdus.length; i++) {
      encoded[i] = Base64.encodeBytes((byte[]) pdus[i]);
    }

    return new Data.Builder().putStringArray(KEY_PDUS, encoded)
                             .putInt(KEY_SUBSCRIPTION_ID, subscriptionId)
                             .build();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onRun() throws MigrationPendingException {
    Log.i(TAG, "onRun()");
    
    Optional<IncomingTextMessage> message = assembleMessageFragments(pdus, subscriptionId);

    if (message.isPresent() && !isBlocked(message.get())) {
      Optional<MessagingDatabase.InsertResult> insertResult = storeMessage(message.get());

      if (insertResult.isPresent()) {
        MessageNotifier.updateNotification(context, insertResult.get().getThreadId());
      }
    } else if (message.isPresent()) {
      Log.w(TAG, "*** Received blocked SMS, ignoring...");
    } else {
      Log.w(TAG, "*** Failed to assemble message fragments!");
    }
  }

  @Override
  public void onCanceled() {

  }

  @Override
  public boolean onShouldRetry(@NonNull Exception exception) {
    return exception instanceof MigrationPendingException;
  }

  private boolean isBlocked(IncomingTextMessage message) {
    if (message.getSender() != null) {
      Recipient recipient = Recipient.resolved(message.getSender());
      return recipient.isBlocked();
    }

    return false;
  }

  private Optional<MessagingDatabase.InsertResult> storeMessage(IncomingTextMessage message) throws MigrationPendingException {
    SmsDatabase database = DatabaseFactory.getSmsDatabase(context);
    database.ensureMigration();

    if (TextSecurePreferences.getNeedsSqlCipherMigration(context)) {
      throw new MigrationPendingException();
    }

    if (message.isSecureMessage()) {
      IncomingTextMessage    placeholder  = new IncomingTextMessage(message, "");
      Optional<MessagingDatabase.InsertResult> insertResult = database.insertMessageInbox(placeholder);
      database.markAsLegacyVersion(insertResult.get().getMessageId());

      return insertResult;
    } else {
      return database.insertMessageInbox(message);
    }
  }

  private Optional<IncomingTextMessage> assembleMessageFragments(@Nullable Object[] pdus, int subscriptionId) {
    if (pdus == null) {
      return Optional.absent();
    }

    List<IncomingTextMessage> messages = new LinkedList<>();

    for (Object pdu : pdus) {
      SmsMessage message   = SmsMessage.createFromPdu((byte[])pdu);
      Recipient  recipient = Recipient.external(context, message.getDisplayOriginatingAddress());
      messages.add(new IncomingTextMessage(recipient.getId(), message, subscriptionId));
    }

    if (messages.isEmpty()) {
      return Optional.absent();
    }

    return Optional.of(new IncomingTextMessage(messages));
  }

  private class MigrationPendingException extends Exception {
  }

  public static final class Factory implements Job.Factory<SmsReceiveJob> {
    @Override
    public @NonNull SmsReceiveJob create(@NonNull Parameters parameters, @NonNull Data data) {
      try {
        int subscriptionId = data.getInt(KEY_SUBSCRIPTION_ID);
        String[] encoded   = data.getStringArray(KEY_PDUS);
        Object[] pdus      = new Object[encoded.length];

        for (int i = 0; i < encoded.length; i++) {
          pdus[i] = Base64.decode(encoded[i]);
        }

        return new SmsReceiveJob(parameters, pdus, subscriptionId);
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    }
  }
}
