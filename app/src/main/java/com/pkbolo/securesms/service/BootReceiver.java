package com.pkbolo.securesms.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.pkbolo.securesms.dependencies.ApplicationDependencies;
import com.pkbolo.securesms.jobs.PushNotificationReceiveJob;

public class BootReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    ApplicationDependencies.getJobManager().add(new PushNotificationReceiveJob(context));
  }
}
