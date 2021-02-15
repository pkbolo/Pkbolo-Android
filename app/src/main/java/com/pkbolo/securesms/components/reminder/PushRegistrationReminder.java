package com.pkbolo.securesms.components.reminder;

import android.content.Context;

import com.pkbolo.securesms.util.TextSecurePreferences;

import com.pkbolo.securesms.R;
import com.pkbolo.securesms.registration.RegistrationNavigationActivity;

public class PushRegistrationReminder extends Reminder {

  public PushRegistrationReminder(final Context context) {
    super(context.getString(R.string.reminder_header_push_title),
          context.getString(R.string.reminder_header_push_text));

    setOkListener(v -> context.startActivity(RegistrationNavigationActivity.newIntentForReRegistration(context)));
  }

  @Override
  public boolean isDismissable() {
    return false;
  }

  public static boolean isEligible(Context context) {
    return !TextSecurePreferences.isPushRegistered(context);
  }
}
