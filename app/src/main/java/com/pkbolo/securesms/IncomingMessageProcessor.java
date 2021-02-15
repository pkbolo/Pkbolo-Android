package com.pkbolo.securesms;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pkbolo.securesms.database.DatabaseFactory;
import com.pkbolo.securesms.database.MessagingDatabase;
import com.pkbolo.securesms.database.MmsSmsDatabase;
import com.pkbolo.securesms.database.PushDatabase;
import com.pkbolo.securesms.jobs.PushDecryptMessageJob;
import com.pkbolo.securesms.logging.Log;
import com.pkbolo.securesms.dependencies.ApplicationDependencies;
import com.pkbolo.securesms.jobmanager.JobManager;
import com.pkbolo.securesms.recipients.Recipient;
import org.whispersystems.signalservice.api.messages.SignalServiceEnvelope;

import java.io.Closeable;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The central entry point for all envelopes that have been retrieved. Envelopes must be processed
 * here to guarantee proper ordering.
 */
public class IncomingMessageProcessor {

  private static final String TAG = Log.tag(IncomingMessageProcessor.class);

  private final Context       context;
  private final ReentrantLock lock;

  public IncomingMessageProcessor(@NonNull Context context) {
    this.context = context;
    this.lock    = new ReentrantLock();
  }

  /**
   * @return An instance of a Processor that will allow you to process messages in a thread safe
   * way. Must be closed.
   */
  public Processor acquire() {
    lock.lock();

    Thread current = Thread.currentThread();
    Log.d(TAG, "Lock acquired by thread " + current.getId() + " (" + current.getName() + ")");

    return new Processor(context);
  }

  private void release() {
    Thread current = Thread.currentThread();
    Log.d(TAG, "Lock about to be released by thread " + current.getId() + " (" + current.getName() + ")");

    lock.unlock();
  }

  public class Processor implements Closeable {

    private final Context           context;
    private final PushDatabase pushDatabase;
    private final MmsSmsDatabase mmsSmsDatabase;
    private final JobManager        jobManager;

    private Processor(@NonNull Context context) {
      this.context           = context;
      this.pushDatabase      = DatabaseFactory.getPushDatabase(context);
      this.mmsSmsDatabase    = DatabaseFactory.getMmsSmsDatabase(context);
      this.jobManager        = ApplicationDependencies.getJobManager();
    }

    /**
     * @return The id of the {@link PushDecryptMessageJob} that was scheduled to process the message, if
     *         one was created. Otherwise null.
     */
    public @Nullable String processEnvelope(@NonNull SignalServiceEnvelope envelope) {
      if (envelope.hasSource()) {
        Recipient.externalPush(context, envelope.getSourceAddress());
      }

      if (envelope.isReceipt()) {
        processReceipt(envelope);
        return null;
      } else if (envelope.isPreKeySignalMessage() || envelope.isSignalMessage() || envelope.isUnidentifiedSender()) {
        return processMessage(envelope);
      } else {
        Log.w(TAG, "Received envelope of unknown type: " + envelope.getType());
        return null;
      }
    }

    private @NonNull String processMessage(@NonNull SignalServiceEnvelope envelope) {
      Log.i(TAG, "Received message. Inserting in PushDatabase.");

      long                  id  = pushDatabase.insert(envelope);
      PushDecryptMessageJob job = new PushDecryptMessageJob(context, id);

      jobManager.add(job);

      return job.getId();
    }

    private void processReceipt(@NonNull SignalServiceEnvelope envelope) {
      Log.i(TAG, String.format(Locale.ENGLISH, "Received receipt: (XXXXX, %d)", envelope.getTimestamp()));
      mmsSmsDatabase.incrementDeliveryReceiptCount(new MessagingDatabase.SyncMessageId(Recipient.externalPush(context, envelope.getSourceAddress()).getId(), envelope.getTimestamp()),
                                                   System.currentTimeMillis());
    }

    @Override
    public void close() {
      release();
    }
  }
}
