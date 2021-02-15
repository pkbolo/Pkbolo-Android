package com.pkbolo.securesms.insights;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import com.pkbolo.securesms.color.MaterialColor;
import com.pkbolo.securesms.contacts.avatars.FallbackContactPhoto;
import com.pkbolo.securesms.contacts.avatars.ProfileContactPhoto;
import com.pkbolo.securesms.mms.GlideApp;
import com.pkbolo.securesms.mms.GlideRequests;

class InsightsUserAvatar {
  private final ProfileContactPhoto  profileContactPhoto;
  private final MaterialColor        fallbackColor;
  private final FallbackContactPhoto fallbackContactPhoto;

  InsightsUserAvatar(@NonNull ProfileContactPhoto profileContactPhoto, @NonNull MaterialColor fallbackColor, @NonNull FallbackContactPhoto fallbackContactPhoto) {
    this.profileContactPhoto  = profileContactPhoto;
    this.fallbackColor        = fallbackColor;
    this.fallbackContactPhoto = fallbackContactPhoto;
  }

  private Drawable fallbackDrawable(@NonNull Context context) {
    return fallbackContactPhoto.asDrawable(context, fallbackColor.toAvatarColor(context));
  }

  void load(ImageView into) {
    GlideApp.with(into)
            .load(profileContactPhoto)
            .error(fallbackDrawable(into.getContext()))
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(into);
  }
}
