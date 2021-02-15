package com.pkbolo.securesms.util;

import android.app.Activity;

import com.pkbolo.securesms.R;

public class DynamicDarkActionBarTheme extends DynamicTheme {
  @Override
  protected int getSelectedTheme(Activity activity) {
    String theme = TextSecurePreferences.getTheme(activity);

    if (theme.equals("dark")) {
      return R.style.TextSecure_DarkTheme_Conversation;
    }

    return R.style.TextSecure_LightTheme_Conversation;
  }
}
