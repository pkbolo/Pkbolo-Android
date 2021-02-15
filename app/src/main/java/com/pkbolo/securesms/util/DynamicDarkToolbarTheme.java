package com.pkbolo.securesms.util;

import android.app.Activity;

import com.pkbolo.securesms.R;

public class DynamicDarkToolbarTheme extends DynamicTheme {
  @Override
  protected int getSelectedTheme(Activity activity) {
    String theme = TextSecurePreferences.getTheme(activity);

    if (theme.equals("dark")) {
      return R.style.TextSecure_DarkNoActionBar_DarkToolbar;
    }

    return R.style.TextSecure_LightNoActionBar_DarkToolbar;
  }
}
