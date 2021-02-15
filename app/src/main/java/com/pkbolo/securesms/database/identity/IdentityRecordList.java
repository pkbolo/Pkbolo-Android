package com.pkbolo.securesms.database.identity;


import android.content.Context;

import com.pkbolo.securesms.database.IdentityDatabase;
import com.pkbolo.securesms.recipients.Recipient;
import org.whispersystems.libsignal.util.guava.Optional;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class IdentityRecordList {

  private static final String TAG = IdentityRecordList.class.getSimpleName();

  private final List<IdentityDatabase.IdentityRecord> identityRecords = new LinkedList<>();

  public void add(Optional<IdentityDatabase.IdentityRecord> identityRecord) {
    if (identityRecord.isPresent()) {
      identityRecords.add(identityRecord.get());
    }
  }

  public void replaceWith(IdentityRecordList identityRecordList) {
    identityRecords.clear();
    identityRecords.addAll(identityRecordList.identityRecords);
  }

  public boolean isVerified() {
    for (IdentityDatabase.IdentityRecord identityRecord : identityRecords) {
      if (identityRecord.getVerifiedStatus() != IdentityDatabase.VerifiedStatus.VERIFIED) {
        return false;
      }
    }

    return identityRecords.size() > 0;
  }

  public boolean isUnverified() {
    for (IdentityDatabase.IdentityRecord identityRecord : identityRecords) {
      if (identityRecord.getVerifiedStatus() == IdentityDatabase.VerifiedStatus.UNVERIFIED) {
        return true;
      }
    }

    return false;
  }

  public boolean isUntrusted() {
    for (IdentityDatabase.IdentityRecord identityRecord : identityRecords) {
      if (isUntrusted(identityRecord)) {
        return true;
      }
    }

    return false;
  }

  public List<IdentityDatabase.IdentityRecord> getUntrustedRecords() {
    List<IdentityDatabase.IdentityRecord> results = new LinkedList<>();

    for (IdentityDatabase.IdentityRecord identityRecord : identityRecords) {
      if (isUntrusted(identityRecord)) {
        results.add(identityRecord);
      }
    }

    return results;
  }

  public List<Recipient> getUntrustedRecipients(Context context) {
    List<Recipient> untrusted = new LinkedList<>();

    for (IdentityDatabase.IdentityRecord identityRecord : identityRecords) {
      if (isUntrusted(identityRecord)) {
        untrusted.add(Recipient.resolved(identityRecord.getRecipientId()));
      }
    }

    return untrusted;
  }

  public List<IdentityDatabase.IdentityRecord> getUnverifiedRecords() {
    List<IdentityDatabase.IdentityRecord> results = new LinkedList<>();

    for (IdentityDatabase.IdentityRecord identityRecord : identityRecords) {
      if (identityRecord.getVerifiedStatus() == IdentityDatabase.VerifiedStatus.UNVERIFIED) {
        results.add(identityRecord);
      }
    }

    return results;
  }

  public List<Recipient> getUnverifiedRecipients(Context context) {
    List<Recipient> unverified = new LinkedList<>();

    for (IdentityDatabase.IdentityRecord identityRecord : identityRecords) {
      if (identityRecord.getVerifiedStatus() == IdentityDatabase.VerifiedStatus.UNVERIFIED) {
        unverified.add(Recipient.resolved(identityRecord.getRecipientId()));
      }
    }

    return unverified;
  }

  private boolean isUntrusted(IdentityDatabase.IdentityRecord identityRecord) {
    return !identityRecord.isApprovedNonBlocking() &&
           System.currentTimeMillis() - identityRecord.getTimestamp() < TimeUnit.SECONDS.toMillis(5);
  }

}
