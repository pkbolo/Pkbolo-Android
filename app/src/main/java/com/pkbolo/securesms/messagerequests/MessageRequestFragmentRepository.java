package com.pkbolo.securesms.messagerequests;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import com.pkbolo.securesms.database.DatabaseFactory;
import com.pkbolo.securesms.database.GroupDatabase;
import com.pkbolo.securesms.database.MessagingDatabase;
import com.pkbolo.securesms.database.MmsSmsDatabase;
import com.pkbolo.securesms.database.RecipientDatabase;
import com.pkbolo.securesms.database.ThreadDatabase;
import com.pkbolo.securesms.database.model.MessageRecord;
import com.pkbolo.securesms.util.concurrent.SignalExecutors;
import com.pkbolo.securesms.util.concurrent.SimpleTask;
import com.pkbolo.securesms.notifications.MarkReadReceiver;
import com.pkbolo.securesms.notifications.MessageNotifier;
import com.pkbolo.securesms.recipients.LiveRecipient;
import com.pkbolo.securesms.recipients.Recipient;
import com.pkbolo.securesms.recipients.RecipientId;
import com.pkbolo.securesms.recipients.RecipientUtil;

import org.whispersystems.libsignal.util.guava.Optional;

import java.util.List;

public class MessageRequestFragmentRepository {

  private final Context       context;
  private final RecipientId   recipientId;
  private final long          threadId;
  private final LiveRecipient liveRecipient;

  public MessageRequestFragmentRepository(@NonNull Context context, @NonNull RecipientId recipientId, long threadId) {
    this.context       = context.getApplicationContext();
    this.recipientId   = recipientId;
    this.threadId      = threadId;
    this.liveRecipient = Recipient.live(recipientId);
  }

  public LiveRecipient getLiveRecipient() {
    return liveRecipient;
  }

  public void refreshRecipient() {
    SignalExecutors.BOUNDED.execute(liveRecipient::refresh);
  }

  public void getMessageRecord(@NonNull Consumer<MessageRecord> onMessageRecordLoaded) {
    SimpleTask.run(() -> {
      MmsSmsDatabase mmsSmsDatabase = DatabaseFactory.getMmsSmsDatabase(context);
      try (Cursor cursor = mmsSmsDatabase.getConversation(threadId, 0, 1)) {
        if (!cursor.moveToFirst()) return null;
        return mmsSmsDatabase.readerFor(cursor).getCurrent();
      }
    }, onMessageRecordLoaded::accept);
  }

  public void getGroups(@NonNull Consumer<List<String>> onGroupsLoaded) {
    SimpleTask.run(() -> {
      GroupDatabase groupDatabase = DatabaseFactory.getGroupDatabase(context);
      return groupDatabase.getGroupNamesContainingMember(recipientId);
    }, onGroupsLoaded::accept);
  }

  public void getMemberCount(@NonNull Consumer<Integer> onMemberCountLoaded) {
    SimpleTask.run(() -> {
      GroupDatabase                       groupDatabase = DatabaseFactory.getGroupDatabase(context);
      Optional<GroupDatabase.GroupRecord> groupRecord   = groupDatabase.getGroup(recipientId);
      return groupRecord.transform(record -> record.getMembers().size()).or(0);
    }, onMemberCountLoaded::accept);
  }

  public void acceptMessageRequest(@NonNull Runnable onMessageRequestAccepted) {
    SimpleTask.run(() -> {
      RecipientDatabase recipientDatabase = DatabaseFactory.getRecipientDatabase(context);
      recipientDatabase.setProfileSharing(recipientId, true);
      liveRecipient.refresh();

      List<MessagingDatabase.MarkedMessageInfo> messageIds = DatabaseFactory.getThreadDatabase(context)
                                                                            .setEntireThreadRead(threadId);
      MessageNotifier.updateNotification(context);
      MarkReadReceiver.process(context, messageIds);

      return null;
    }, v -> onMessageRequestAccepted.run());
  }

  public void deleteMessageRequest(@NonNull Runnable onMessageRequestDeleted) {
    SimpleTask.run(() -> {
      ThreadDatabase threadDatabase = DatabaseFactory.getThreadDatabase(context);
      threadDatabase.deleteConversation(threadId);
      return null;
    }, v -> onMessageRequestDeleted.run());
  }

  public void blockMessageRequest(@NonNull Runnable onMessageRequestBlocked) {
    SimpleTask.run(() -> {
      Recipient recipient = liveRecipient.resolve();
      RecipientUtil.block(context, recipient);
      liveRecipient.refresh();
      return null;
    }, v -> onMessageRequestBlocked.run());
  }
}
