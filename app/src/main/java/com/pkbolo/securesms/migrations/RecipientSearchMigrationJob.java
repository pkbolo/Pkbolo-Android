package com.pkbolo.securesms.migrations;

import androidx.annotation.NonNull;

import com.pkbolo.securesms.contacts.sync.DirectoryHelper;
import com.pkbolo.securesms.jobmanager.impl.NetworkConstraint;
import com.pkbolo.securesms.jobmanager.Data;
import com.pkbolo.securesms.jobmanager.Job;

import java.io.IOException;

/**
 * We added a column for keeping track of the phone number type ("mobile", "home", etc) to the
 * recipient database, and therefore we need to do a directory sync to fill in that column.
 */
public class RecipientSearchMigrationJob extends MigrationJob {

  public static final String KEY = "RecipientSearchMigrationJob";

  RecipientSearchMigrationJob() {
    this(new Job.Parameters.Builder().addConstraint(NetworkConstraint.KEY).build());
  }

  private RecipientSearchMigrationJob(@NonNull Job.Parameters parameters) {
    super(parameters);
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  boolean isUiBlocking() {
    return false;
  }

  @Override
  void performMigration() throws Exception {
    DirectoryHelper.refreshDirectory(context, false);
  }

  @Override
  boolean shouldRetry(@NonNull Exception e) {
    return e instanceof IOException;
  }

  public static class Factory implements Job.Factory<RecipientSearchMigrationJob> {
    @Override
    public @NonNull RecipientSearchMigrationJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new RecipientSearchMigrationJob(parameters);
    }
  }
}
