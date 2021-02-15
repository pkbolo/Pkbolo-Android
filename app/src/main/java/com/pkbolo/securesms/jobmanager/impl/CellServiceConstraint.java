package com.pkbolo.securesms.jobmanager.impl;

import android.app.Application;
import android.app.job.JobInfo;

import androidx.annotation.NonNull;

import com.pkbolo.securesms.jobmanager.Constraint;

public class CellServiceConstraint implements Constraint {

  public static final String KEY = "CellServiceConstraint";

  private final Application application;

  public CellServiceConstraint(@NonNull Application application) {
    this.application = application;
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public boolean isMet() {
    return CellServiceConstraintObserver.getInstance(application).hasService();
  }

  @Override
  public void applyToJobInfo(@NonNull JobInfo.Builder jobInfoBuilder) {
  }

  public static final class Factory implements Constraint.Factory<CellServiceConstraint> {

    private final Application application;

    public Factory(@NonNull Application application) {
      this.application = application;
    }

    @Override
    public CellServiceConstraint create() {
      return new CellServiceConstraint(application);
    }
  }
}
