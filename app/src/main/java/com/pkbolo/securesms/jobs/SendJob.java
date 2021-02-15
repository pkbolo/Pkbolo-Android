package com.pkbolo.securesms.jobs;

import androidx.annotation.NonNull;

import com.pkbolo.securesms.database.AttachmentDatabase;
import com.pkbolo.securesms.database.DatabaseFactory;
import com.pkbolo.securesms.logging.Log;
import com.pkbolo.securesms.util.Util;

import com.pkbolo.securesms.BuildConfig;
import com.pkbolo.securesms.TextSecureExpiredException;
import com.pkbolo.securesms.attachments.Attachment;
import com.pkbolo.securesms.jobmanager.Job;

import java.util.List;

public abstract class SendJob extends BaseJob {

  @SuppressWarnings("unused")
  private final static String TAG = SendJob.class.getSimpleName();

  public SendJob(Job.Parameters parameters) {
    super(parameters);
  }

  @Override
  public final void onRun() throws Exception {
    if (Util.getDaysTillBuildExpiry() <= 0) {
      throw new TextSecureExpiredException(String.format("TextSecure expired (build %d, now %d)",
                                                         BuildConfig.BUILD_TIMESTAMP,
                                                         System.currentTimeMillis()));
    }

    Log.i(TAG, "Starting message send attempt");
    onSend();
    Log.i(TAG, "Message send completed");
  }

  protected abstract void onSend() throws Exception;

  protected void markAttachmentsUploaded(long messageId, @NonNull List<Attachment> attachments) {
    AttachmentDatabase database = DatabaseFactory.getAttachmentDatabase(context);

    for (Attachment attachment : attachments) {
      database.markAttachmentUploaded(messageId, attachment);
    }
  }
}
